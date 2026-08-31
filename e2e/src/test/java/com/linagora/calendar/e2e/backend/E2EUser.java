package com.linagora.calendar.e2e.backend;

/**
 * An account of the e2e stack.
 *
 * <p>Never build one by hand: an account only exists once it has been written to the
 * directory Dex authenticates against. Ask {@link E2EUserFactory} for one — or simply declare
 * an {@code E2EUser} parameter on the test method and get a brand new one.
 */
public record E2EUser(String uid, String email, String password, String displayName) {
}
