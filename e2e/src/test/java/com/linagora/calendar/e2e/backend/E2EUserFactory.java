package com.linagora.calendar.e2e.backend;

import java.util.Hashtable;
import java.util.UUID;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.linagora.calendar.e2e.docker.TwakeCalendarStack;

/**
 * Creates accounts on demand, one per test.
 *
 * <p>Dex authenticates against the OpenLDAP of the stack rather than against a list written in
 * its configuration file, so an account is just an LDAP entry: it can be created a millisecond
 * before the test logs in, and nothing caps how many a run may consume. Everything downstream
 * follows on its own — the side service provisions the OpenPaaS user on the first
 * authenticated call, which in turn provisions the default calendar.
 *
 * <p>Accounts are never reused, which is the whole point: the docker stack is shared by the
 * run because booting it costs minutes, so the user is the unit of isolation. A test that owns
 * its user cannot inherit the events, calendars or settings of another, and none has to clean
 * up after itself.
 */
public class E2EUserFactory {
    public static final String DOMAIN = "open-paas.org";
    public static final String PASSWORD = "secret";

    private static final String USERS_BASE_DN = "ou=users,dc=open-paas.org,dc=lng";
    private static final String ADMIN_DN = "cn=admin,dc=open-paas.org,dc=lng";
    private static final String ADMIN_PASSWORD = "admin";

    private final DirContext directory;

    public E2EUserFactory(TwakeCalendarStack stack) {
        Hashtable<String, Object> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.PROVIDER_URL, stack.ldapUrl());
        environment.put(Context.SECURITY_AUTHENTICATION, "simple");
        environment.put(Context.SECURITY_PRINCIPAL, ADMIN_DN);
        environment.put(Context.SECURITY_CREDENTIALS, ADMIN_PASSWORD);
        try {
            this.directory = new InitialDirContext(environment);
        } catch (NamingException e) {
            throw new RuntimeException("Cannot reach the e2e directory at " + stack.ldapUrl(), e);
        }
    }

    /** A brand new account, ready to log in. */
    public E2EUser newUser() {
        return newUser("e2e-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    }

    /**
     * A brand new account whose local part starts with the given prefix, for the rare test
     * that asserts on what it displays. The prefix is suffixed to keep it unique.
     */
    public E2EUser newUser(String prefix) {
        String uid = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        String email = uid + "@" + DOMAIN;
        create(uid, email);
        return new E2EUser(uid, email, PASSWORD, uid);
    }

    private synchronized void create(String uid, String email) {
        Attribute objectClass = new BasicAttribute("objectClass");
        objectClass.add("inetOrgPerson");

        Attributes attributes = new BasicAttributes(true);
        attributes.put(objectClass);
        attributes.put("uid", uid);
        attributes.put("cn", uid);
        attributes.put("sn", uid);
        attributes.put("givenName", uid);
        attributes.put("mail", email);
        attributes.put("userPassword", PASSWORD);

        try {
            directory.createSubcontext("uid=" + uid + "," + USERS_BASE_DN, attributes);
        } catch (NamingException e) {
            throw new RuntimeException("Failed to create the e2e account " + email, e);
        }
    }
}
