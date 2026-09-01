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
 * Domain resources -- meeting rooms, cars, laptops -- provisioned through the webadmin API.
 *
 * <p>Like team calendars they are an administration object the product does not create, so the
 * suite plays the administrator and then looks at the result through the interface.
 */
public class ResourceProbe {
    private static final Pattern ID_IN_LOCATION = Pattern.compile("([^/]+)$");

    private final HttpClient httpClient;
    private final String baseUrl;

    public ResourceProbe(TwakeCalendarStack stack) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.baseUrl = stack.webAdminUri() + "/domains/open-paas.org/resources";
    }

    /**
     * Creates a resource administered by somebody.
     *
     * <p>As for team calendars, the administrator has to have signed in once: accounts are
     * provisioned lazily and the API does not know them before that.
     */
    public String create(String name, String description, E2EUser administrator) {
        HttpResponse<String> response = send("POST", baseUrl, "{"
            + "\"name\":\"" + name + "\","
            + "\"description\":\"" + description + "\","
            + "\"creator\":\"" + administrator.email() + "\","
            + "\"icon\":\"laptop\","
            + "\"administrators\":[{\"email\":\"" + administrator.email()
            + "\",\"davRight\":\"dav:administration\"}]}");
        if (response.statusCode() != 201) {
            throw new IllegalStateException("Could not create the resource " + name + ": "
                + response.statusCode() + " " + response.body());
        }
        String location = response.headers().firstValue("Location").orElse("");
        Matcher matcher = ID_IN_LOCATION.matcher(location);
        if (!matcher.find()) {
            throw new IllegalStateException("No resource identifier in the Location " + location);
        }
        return matcher.group(1);
    }

    public String get(String resourceId) {
        return send("GET", baseUrl + "/" + resourceId, null).body();
    }

    public void delete(String resourceId) {
        HttpResponse<String> response = send("DELETE", baseUrl + "/" + resourceId, null);
        if (response.statusCode() != 204) {
            throw new IllegalStateException("Could not delete the resource " + resourceId + ": "
                + response.statusCode() + " " + response.body());
        }
    }

    public List<String> names() {
        List<String> names = new java.util.ArrayList<>();
        Matcher matcher = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"")
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
