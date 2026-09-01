package com.linagora.calendar.e2e.backend;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;

import com.linagora.calendar.e2e.docker.TwakeCalendarStack;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * Backend side view of a user's calendar, for tests that need to seed a fixture or to assert
 * that what the UI showed really made it to the server.
 *
 * <p>It talks CalDAV to Sabre with the admin impersonation credentials, which sidesteps OIDC
 * entirely: a probe call never disturbs the browser session under test.
 */
public class CalendarProbe {
    private static final String SABRE_ADMIN_PASSWORD = "secret123";
    private static final Pattern SUMMARY = Pattern.compile("^SUMMARY:(.*)$", Pattern.MULTILINE);
    private static final Pattern HREF = Pattern.compile("<[^>]*href>([^<]*\\.ics)</[^>]*href>");

    private final HttpClient httpClient;
    private final String davBaseUrl;
    private final MongoDatabase esnDatabase;

    public CalendarProbe(TwakeCalendarStack stack) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        this.davBaseUrl = stack.davUri();
        MongoClient mongoClient = MongoClients.create(stack.mongoUri());
        this.esnDatabase = mongoClient.getDatabase("esn_docker");
    }

    /**
     * OpenPaaS identifier of a user, which is also the name of its DAV principal.
     * Empty until the user logged in once: the side service provisions accounts lazily,
     * on the first authenticated call.
     */
    public Optional<String> openPaasId(E2EUser user) {
        return Optional.ofNullable(esnDatabase.getCollection("users")
                .find(new Document("accounts.emails", user.email())).first())
            .map(document -> document.getObjectId("_id").toString());
    }

    public String requireOpenPaasId(E2EUser user) {
        return openPaasId(user).orElseThrow(() -> new IllegalStateException(
            user.email() + " is not provisioned yet. It gets created upon its first login."));
    }

    /** The display names of every calendar collection the user owns. */
    public List<String> calendarNames(E2EUser user) {
        HttpResponse<String> response = execute(user, "PROPFIND",
            "/calendars/" + requireOpenPaasId(user) + "/", null, null);
        List<String> names = new ArrayList<>();
        Matcher matcher = Pattern.compile("<[^>]*displayname[^>]*>([^<]+)<").matcher(response.body());
        while (matcher.find()) {
            names.add(matcher.group(1).trim());
        }
        return names;
    }

    /** Summaries of every event of the user's default calendar. */
    public List<String> eventSummaries(E2EUser user) {
        return eventHrefs(user).stream()
            .map(href -> get(user, href))
            .map(this::summaryOf)
            .flatMap(Optional::stream)
            .toList();
    }

    /** Raw iCalendar payload of every event of the user's default calendar. */
    public List<String> rawEvents(E2EUser user) {
        return eventHrefs(user).stream()
            .map(href -> get(user, href))
            .toList();
    }

    /**
     * The one and only calendar object of the user, raw. A recurring event is a single object,
     * holding its master VEVENT and one VEVENT per overridden occurrence.
     */
    public String singleEvent(E2EUser user) {
        List<String> events = rawEvents(user);
        if (events.size() != 1) {
            throw new AssertionError("Expected exactly one calendar object, got " + events.size());
        }
        return events.getFirst();
    }

    /** Removes every event of the user's default calendar, to give a test a clean slate. */
    public void clearCalendar(E2EUser user) {
        eventHrefs(user).forEach(href -> execute(user, "DELETE", href, null, null));
    }

    public void deleteEvent(E2EUser user, String eventUid) {
        execute(user, "DELETE", defaultCalendarPath(user) + eventUid + ".ics", null, null);
    }

    /** Writes an event straight into the user's default calendar, bypassing the UI. */
    public void putEvent(E2EUser user, String eventUid, String icalendar) {
        String href = defaultCalendarPath(user) + eventUid + ".ics";
        HttpResponse<String> response = execute(user, "PUT", href, icalendar, "text/calendar");
        if (response.statusCode() != 201 && response.statusCode() != 204) {
            throw new IllegalStateException("Failed to create event " + eventUid + ": "
                + response.statusCode() + " " + response.body());
        }
    }

    /** Forces the provisioning of the user's default calendar. */
    public void provisionDefaultCalendar(E2EUser user) {
        String id = requireOpenPaasId(user);
        execute(user, "PROPFIND", "/calendars/" + id, null, null);
    }

    private List<String> eventHrefs(E2EUser user) {
        HttpResponse<String> response = execute(user, "PROPFIND", defaultCalendarPath(user), null, null);
        if (response.statusCode() != 207) {
            throw new IllegalStateException("Failed to list the calendar of " + user.email() + ": "
                + response.statusCode() + " " + response.body());
        }
        List<String> hrefs = new ArrayList<>();
        Matcher matcher = HREF.matcher(response.body());
        while (matcher.find()) {
            hrefs.add(matcher.group(1));
        }
        return hrefs;
    }

    private String defaultCalendarPath(E2EUser user) {
        String id = requireOpenPaasId(user);
        return "/calendars/" + id + "/" + id + "/";
    }

    private String get(E2EUser user, String href) {
        return execute(user, "GET", href, null, null).body();
    }

    private Optional<String> summaryOf(String icalendar) {
        // RFC 5545 folds long lines: unfold first, or a long summary comes back truncated
        Matcher matcher = SUMMARY.matcher(Ics.unfold(icalendar));
        return matcher.find() ? Optional.of(matcher.group(1).trim()) : Optional.empty();
    }

    private HttpResponse<String> execute(E2EUser user, String method, String path, String body, String contentType) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(davBaseUrl + path))
            .header("Authorization", impersonationHeader(user))
            .header("Accept", "application/xml, text/calendar, */*")
            .timeout(Duration.ofSeconds(30));
        if ("PROPFIND".equals(method)) {
            builder.header("Depth", "1");
        }
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        builder.method(method, body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        try {
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(method + " " + path + " failed", e);
        }
    }

    private String impersonationHeader(E2EUser user) {
        String credentials = "admin&" + user.email() + ":" + SABRE_ADMIN_PASSWORD;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
