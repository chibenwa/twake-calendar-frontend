package com.linagora.calendar.e2e.backend;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.linagora.calendar.e2e.docker.TwakeCalendarStack;

/**
 * Team calendars, provisioned through the webadmin API of the side service.
 *
 * <p>Nothing in the product creates one: they are an administration object, so the suite plays
 * the administrator here and then looks at the result through the interface, which is where the
 * regressions actually happen.
 *
 * <p>See the webadmin documentation of twake-calendar-side-service for the routes used below.
 */
public class TeamCalendarProbe {
    /** The rights a member can hold, in the vocabulary the API speaks. */
    public enum Right {
        READ("dav:read"),
        READ_WRITE("dav:read-write"),
        ADMINISTRATION("dav:administration");

        private final String property;

        Right(String property) {
            this.property = property;
        }

        String property() {
            return property;
        }
    }

    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient;
    private final String baseUrl;

    public TeamCalendarProbe(TwakeCalendarStack stack) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.baseUrl = stack.webAdminUri() + "/domains/open-paas.org/team-calendars";
    }

    /** Creates a team calendar and returns its identifier. */
    public String create(String name, String displayName) {
        HttpResponse<String> response = send("POST", baseUrl,
            "{\"name\":\"" + name + "\",\"displayName\":\"" + displayName + "\"}");
        if (response.statusCode() != 201) {
            throw new IllegalStateException("Could not create the team calendar " + name + ": "
                + response.statusCode() + " " + response.body());
        }
        Matcher matcher = ID.matcher(response.body());
        if (!matcher.find()) {
            throw new IllegalStateException("No identifier in " + response.body());
        }
        return matcher.group(1);
    }

    /**
     * Grants a right to somebody, or changes the one they hold.
     *
     * <p>A partial update: the members left out keep what they had.
     */
    public void grant(String teamCalendarId, E2EUser member, Right right) {
        share(teamCalendarId, "\"set\":[{\"dav:href\":\"mailto:" + member.email() + "\",\""
            + right.property() + "\":true}]");
    }

    /** Takes back the right somebody holds on a team calendar. */
    public void revoke(String teamCalendarId, E2EUser member) {
        share(teamCalendarId, "\"remove\":[{\"dav:href\":\"mailto:" + member.email() + "\"}]");
    }

    private void share(String teamCalendarId, String body) {
        HttpResponse<String> response = send("POST",
            baseUrl + "/" + teamCalendarId + "/members/invitee",
            "{\"share\":{" + body + "}}");
        if (response.statusCode() != 204) {
            throw new IllegalStateException("The team calendar " + teamCalendarId
                + " refused the membership change: " + response.statusCode() + " " + response.body());
        }
    }

    /** The members of a team calendar, raw, for the assertions on provisioning itself. */
    public String members(String teamCalendarId) {
        return send("GET", baseUrl + "/" + teamCalendarId + "/members", null).body();
    }

    public List<String> names() {
        List<String> names = new java.util.ArrayList<>();
        Matcher matcher = Pattern.compile("\"displayName\"\\s*:\\s*\"([^\"]+)\"")
            .matcher(send("GET", baseUrl, null).body());
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private HttpResponse<String> send(String method, String url, String body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30));
            if (body != null) {
                builder.header("Content-Type", "application/json");
            }
            builder.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(method + " " + url + " failed", e);
        }
    }
}
