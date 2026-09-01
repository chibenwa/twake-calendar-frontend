package com.linagora.calendar.e2e.backend;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linagora.calendar.e2e.docker.TwakeCalendarStack;

/**
 * What the backend actually put in the post.
 *
 * <p>The stack sends its mail to a mock SMTP server which keeps everything it receives and hands
 * it back over HTTP. That is the only way to reach an invitation link the way a real recipient
 * would: the public pages are behind a token that exists nowhere else.
 */
public class MailProbe {
    private static final ObjectMapper JSON = new ObjectMapper();
    /** Links are wrapped and quoted-printable encoded in the body, hence the tolerant pattern. */
    private static final Pattern LINK = Pattern.compile("https?://[^\\s\"'<>\\\\]+");

    private final HttpClient httpClient;
    private final String baseUrl;

    public MailProbe(TwakeCalendarStack stack) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.baseUrl = stack.mockSmtpUri();
    }

    /** One mail as the server received it. */
    public record Mail(String from, List<String> recipients, String body) {
        public boolean wasSentTo(String address) {
            return recipients.stream().anyMatch(recipient -> recipient.contains(address));
        }
    }

    public List<Mail> mails() {
        HttpResponse<String> response = send("GET", "/smtpMails");
        try {
            JsonNode array = JSON.readTree(response.body());
            return java.util.stream.StreamSupport.stream(array.spliterator(), false)
                .map(node -> new Mail(
                    node.path("from").asText(""),
                    java.util.stream.StreamSupport
                        .stream(node.path("recipients").spliterator(), false)
                        .map(recipient -> recipient.path("address").asText(recipient.asText("")))
                        .toList(),
                    decode(node.path("message").asText(""))))
                .toList();
        } catch (Exception e) {
            throw new RuntimeException("Could not read the mail from " + baseUrl
                + ", which answered " + response.body(), e);
        }
    }

    /** Every mail sent to somebody, most recent last. */
    public List<Mail> mailsTo(String address) {
        return mails().stream().filter(mail -> mail.wasSentTo(address)).toList();
    }

    /** Forgets everything received so far, so a test only sees what it caused itself. */
    public void clear() {
        send("DELETE", "/smtpMails");
    }

    /** The first link of a mail whose address carries the given path, e.g. {@code /excal}. */
    public Optional<String> linkTo(String address, String path) {
        return mailsTo(address).stream()
            .flatMap(mail -> links(mail.body()).stream())
            .filter(link -> link.contains(path))
            .findFirst();
    }

    public List<String> links(String body) {
        List<String> found = new java.util.ArrayList<>();
        Matcher matcher = LINK.matcher(body);
        while (matcher.find()) {
            found.add(matcher.group());
        }
        return found;
    }

    /**
     * Undoes the quoted printable encoding of a mail body, which otherwise breaks every link in
     * it with soft line breaks and {@code =3D} for each equals sign -- and a token is mostly
     * equals signs.
     */
    private static String decode(String body) {
        String unwrapped = body.replace("=\r\n", "").replace("=\n", "");
        StringBuilder out = new StringBuilder(unwrapped.length());
        for (int index = 0; index < unwrapped.length(); index++) {
            char current = unwrapped.charAt(index);
            if (current == '=' && index + 2 < unwrapped.length()) {
                String hex = unwrapped.substring(index + 1, index + 3);
                try {
                    out.append((char) Integer.parseInt(hex, 16));
                    index += 2;
                    continue;
                } catch (NumberFormatException notEncoded) {
                    // a plain equals sign: keep it
                }
            }
            out.append(current);
        }
        return out.toString();
    }

    private HttpResponse<String> send(String method, String path) {
        try {
            return httpClient.send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(method + " " + path + " failed", e);
        }
    }
}
