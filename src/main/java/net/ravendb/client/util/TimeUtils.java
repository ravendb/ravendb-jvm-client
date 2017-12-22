package net.ravendb.client.util;

import java.time.Duration;

public class TimeUtils {

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
