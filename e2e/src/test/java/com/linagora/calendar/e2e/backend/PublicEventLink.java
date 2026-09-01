package com.linagora.calendar.e2e.backend;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

/**
 * The links the backend puts in an invitation mail, minted by the suite instead.
 *
 * <p>The public event preview is reached with a token in the query string, which the public
 * application forwards to the side service. That token is normally handed to an attendee by
 * mail; this stack sends none, so the suite signs its own with the very key the side service
 * verifies against -- the one already sitting in its configuration.
 *
 * <p>What this covers is the public page and the endpoint behind it. It says nothing about the
 * mail that would carry the link in production, which stays outside the reach of the suite.
 */
public class PublicEventLink {
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    /** The answer an attendee gives, as the token spells it. */
    public enum Action {
        ACCEPTED, DECLINED, TENTATIVE
    }

    private final PrivateKey signingKey;

    public PublicEventLink() {
        this.signingKey = readSigningKey();
    }

    /** A token for an attendee of an event, valid from now. */
    public String tokenFor(String calendarUri, String eventUid, String organizerEmail,
                           String attendeeEmail, Action action) {
        return token(calendarUri, eventUid, organizerEmail, attendeeEmail, action,
            Instant.now().plusSeconds(3600));
    }

    /** The same token, already past its expiry, for the scenarios about stale links. */
    public String expiredTokenFor(String calendarUri, String eventUid, String organizerEmail,
                                  String attendeeEmail, Action action) {
        return token(calendarUri, eventUid, organizerEmail, attendeeEmail, action,
            Instant.now().minusSeconds(3600));
    }

    private String token(String calendarUri, String eventUid, String organizerEmail,
                         String attendeeEmail, Action action, Instant expiry) {
        String header = encode("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String payload = encode("{"
            + "\"calendarURI\":\"" + calendarUri + "\","
            + "\"uid\":\"" + eventUid + "\","
            + "\"action\":\"" + action.name() + "\","
            + "\"organizerEmail\":\"" + organizerEmail + "\","
            + "\"attendeeEmail\":\"" + attendeeEmail + "\","
            + "\"iat\":" + Instant.now().getEpochSecond() + ","
            + "\"exp\":" + expiry.getEpochSecond()
            + "}");
        return header + "." + payload + "." + sign(header + "." + payload);
    }

    /** The address of the public preview page carrying that token. */
    public String previewUrl(String token) {
        return "http://public/excal?jwt=" + java.net.URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private String sign(String signingInput) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(signingKey);
            signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
            return BASE64_URL.encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign the token", e);
        }
    }

    private static String encode(String json) {
        return BASE64_URL.encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static PrivateKey readSigningKey() {
        try (var stream = PublicEventLink.class.getResourceAsStream(
                "/twake-calendar-side-service-conf/jwt_privatekey.pkcs8")) {
            String pem = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
            return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not read the side service signing key", e);
        }
    }
}
