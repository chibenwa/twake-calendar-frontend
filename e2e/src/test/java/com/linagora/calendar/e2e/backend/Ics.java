package com.linagora.calendar.e2e.backend;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Just enough iCalendar parsing to assert on what the SPA wrote.
 *
 * <p>A recurring event lives in a single calendar object: one master VEVENT carrying the
 * `RRULE`, plus one overriding VEVENT per modified occurrence, each carrying a
 * `RECURRENCE-ID`. Most recurrence regressions are visible right here.
 */
public class Ics {

    /** Unfolds the RFC 5545 line folding, so a property can be matched in one piece. */
    public static String unfold(String icalendar) {
        return icalendar.replaceAll("\r?\n[ \t]", "");
    }

    /** Every VEVENT of the calendar object, master first. */
    public static List<String> vevents(String icalendar) {
        List<String> events = new ArrayList<>();
        Matcher matcher = Pattern.compile("BEGIN:VEVENT(.*?)END:VEVENT", Pattern.DOTALL)
            .matcher(unfold(icalendar));
        while (matcher.find()) {
            events.add(matcher.group(1));
        }
        return events;
    }

    /**
     * The main VEVENT of a calendar object: the master of a series, or the only event.
     * Always go through it rather than reading a property off the whole object, which also
     * holds a VTIMEZONE carrying its own DTSTART and RRULE.
     */
    public static String event(String icalendar) {
        List<String> events = vevents(icalendar);
        return events.stream()
            .filter(vevent -> property(vevent, "RECURRENCE-ID").isEmpty())
            .findFirst()
            .orElseGet(events::getFirst);
    }

    /** The VEVENT holding the `RRULE`, that is the one the occurrences are generated from. */
    public static String master(String icalendar) {
        return vevents(icalendar).stream()
            .filter(vevent -> property(vevent, "RRULE").isPresent())
            .findFirst()
            .orElseThrow(() -> new AssertionError("No recurring VEVENT in:\n" + icalendar));
    }

    /** The VEVENTs overriding one occurrence, that is the exceptions of the series. */
    public static List<String> overrides(String icalendar) {
        return vevents(icalendar).stream()
            .filter(vevent -> property(vevent, "RECURRENCE-ID").isPresent())
            .toList();
    }

    /**
     * The raw value of a property, parameters excluded. `RRULE` of a master, `SUMMARY`,
     * `SEQUENCE`... Empty when the property is absent.
     */
    public static Optional<String> property(String vevent, String name) {
        Matcher matcher = Pattern.compile("^" + Pattern.quote(name) + "(;[^:\r\n]*)?:(.*)$",
            Pattern.MULTILINE).matcher(vevent);
        return matcher.find() ? Optional.of(matcher.group(2).trim()) : Optional.empty();
    }

    /** The parameters of a property, `TZID=Europe/Paris` and friends. Empty string when none. */
    public static String parameters(String vevent, String name) {
        Matcher matcher = Pattern.compile("^" + Pattern.quote(name) + "(;[^:\r\n]*)?:", Pattern.MULTILINE)
            .matcher(vevent);
        return matcher.find() ? Optional.ofNullable(matcher.group(1)).orElse("") : "";
    }

    /** Every value of a property that may appear several times, `EXDATE` typically. */
    public static List<String> properties(String vevent, String name) {
        List<String> values = new ArrayList<>();
        Matcher matcher = Pattern.compile("^" + Pattern.quote(name) + "(;[^:\r\n]*)?:(.*)$",
            Pattern.MULTILINE).matcher(vevent);
        while (matcher.find()) {
            values.add(matcher.group(2).trim());
        }
        return values;
    }

    /** One part of an `RRULE`, `FREQ`, `INTERVAL`, `COUNT`, `UNTIL`, `BYDAY`... */
    public static Optional<String> rulePart(String rrule, String part) {
        Matcher matcher = Pattern.compile("(?:^|;)" + Pattern.quote(part) + "=([^;]*)").matcher(rrule);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
