package com.linagora.calendar.e2e.pages;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.awaitility.Awaitility;

import com.linagora.calendar.e2e.backend.E2EUser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * A calendar somebody lent, seen from the session of the person they lent it to.
 *
 * <p>It lands in a "Shared calendars" section of the sidebar, switched off, and named after its
 * owner: the owner is what identifies it.
 */
public class SharedCalendar {
    /** How long a share, or a change to it, may take to reach the other session. */
    public static final long PROPAGATION_MS = 60_000;

    private final Page grantee;
    private final E2EUser owner;
    private final List<String> requestsToOwnerNode = new CopyOnWriteArrayList<>();

    public SharedCalendar(Page grantee, E2EUser owner) {
        this.grantee = grantee;
        this.owner = owner;
    }

    /**
     * Starts recording every request this session sends to the calendar home of the owner.
     *
     * <p>A share gives no right on the owner's own node: Sabre creates an instance of the
     * calendar in the grantee's home, and only that instance carries the grantee's rights. So
     * whatever the grantee does with the calendar has to go through that instance, and a request
     * to {@code /calendars/<ownerId>/} is a request bound to be refused -- or, on a server
     * lenient enough to answer it, one that only works by accident.
     */
    public SharedCalendar watchRequestsToOwnerNode(String ownerOpenPaasId) {
        String ownerNode = "/calendars/" + ownerOpenPaasId + "/";
        grantee.onRequest(request -> {
            if (request.url().contains(ownerNode)) {
                requestsToOwnerNode.add(request.method() + " " + request.url());
            }
        });
        return this;
    }

    /** What {@link #watchRequestsToOwnerNode} recorded so far. */
    public List<String> requestsToOwnerNode() {
        return List.copyOf(requestsToOwnerNode);
    }

    /**
     * The sidebar row. Matched on the local part of the owner's address rather than on the
     * whole: the row is named after the owner's display name, and that is the address in some
     * builds and only its local part in others. The local part is in both.
     */
    public Locator row() {
        return grantee.locator("li").filter(new Locator.FilterOptions().setHasText(owner.uid()));
    }

    /**
     * Reloads the grantee's page until the calendar shows up in their sidebar.
     *
     * <p>Polls slowly on purpose: each attempt reloads the application, and the sidebar needs a
     * moment after that to fetch the calendars. Hammering reload every few milliseconds keeps it
     * permanently at the beginning of that fetch, and the calendar would never appear.
     */
    public SharedCalendar awaitInSidebar() {
        Awaitility.await().atMost(Duration.ofMillis(PROPAGATION_MS))
            .pollInterval(Duration.ofSeconds(2))
            // a locator giving up on one attempt is no reason to give up on the next one
            .ignoreExceptions()
            .untilAsserted(() -> {
                grantee.reload();
                new CalendarPage(grantee).waitUntilLoaded();
                row().first().waitFor(new Locator.WaitForOptions().setTimeout(8_000));
            });
        return this;
    }

    /**
     * Ticks the calendar. It arrives switched off, so its events are not drawn until the grantee
     * asks for them -- a test looking straight at the grid would conclude the share failed.
     */
    public SharedCalendar show() {
        Locator checkbox = row().first().locator("input[type=checkbox]").first();
        if (!checkbox.isChecked()) {
            checkbox.check();
        }
        return this;
    }

    /** Waits, reloading slowly, for an event of the calendar to reach the grantee's grid. */
    public SharedCalendar awaitEvent(String title) {
        awaitInSidebar();
        Awaitility.await().atMost(Duration.ofMillis(PROPAGATION_MS))
            .pollInterval(Duration.ofSeconds(2))
            // a locator giving up on one attempt is no reason to give up on the next one
            .ignoreExceptions()
            .untilAsserted(() -> {
                grantee.reload();
                CalendarPage calendar = new CalendarPage(grantee).waitUntilLoaded();
                show();
                calendar.eventCard(title).first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(8_000));
            });
        return this;
    }

    /** What the overflow menu of the row offers. */
    public List<String> menu() {
        Locator row = row().first();
        row.hover();
        row.locator("button").last().click();
        grantee.locator("[role=menuitem]").first().waitFor();
        List<String> entries = grantee.locator("[role=menuitem]").allInnerTexts();
        grantee.keyboard().press("Escape");
        return entries;
    }
}
