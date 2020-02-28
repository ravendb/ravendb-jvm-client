package net.ravendb.client.util;

import com.fasterxml.jackson.core.JsonParseException;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;

public class TimeUtils {

    private static Duration parseMiddlePart(String input) {
        String[] tokens = input.split(":");
        int hours = Integer.parseInt(tokens[0]);
        int minutes = Integer.parseInt(tokens[1]);
        int seconds = Integer.parseInt(tokens[2]);

        if (tokens.length != 3) {
            throw new IllegalArgumentException("Unexpected Duration format: "+ input);
        }

        return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
    }

    public static Duration timeSpanToDuration(String text) {
        boolean hasDays = text.matches("^\\d+\\..*");
        boolean hasMillis = text.matches(".*\\.\\d+");

        if (hasDays && hasMillis) {
            String[] tokens = text.split("\\.");

            int days = Integer.parseInt(tokens[0]);
            int millis = Integer.parseInt(tokens[2]);
            return parseMiddlePart(tokens[1]).plusDays(days).plusMillis(millis);
        } else if (hasDays) {
            String[] tokens = text.split("\\.");
            int days = Integer.parseInt(tokens[0]);
            return parseMiddlePart(tokens[1]).plusDays(days);
        } else if (hasMillis) {
            String[] tokens = text.split("\\.");
            String fractionString = tokens[1];
            fractionString = StringUtils.rightPad(fractionString, 7, '0');
            long value = Long.parseLong(fractionString);

            value *= 100;

            return parseMiddlePart(tokens[0]).plusNanos(value);
        } else {
            return parseMiddlePart(text);
        }

    }

    public static String durationToTimeSpan(Duration duration) {
        long time = duration.toMillis();
        long millis = time % 1000;
        time = time / 1000; // seconds
        long seconds = time % 60;
        time = time / 60; // in minutes
        long minutes = time % 60;
        time = time / 60; // in hours
        long hours = time % 24;
        time = time / 24; // in days
        long days = time;

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append(".");
        }
        sb.append(String.format("%02d", hours)).append(":");
        sb.append(String.format("%02d", minutes)).append(":");
        sb.append(String.format("%02d", seconds));
        if (millis > 0) {
            sb.append(".");
            sb.append(String.format("%03d", millis)).append("0000");
        }

        return sb.toString();
    }

}
