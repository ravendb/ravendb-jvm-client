package net.ravendb.client.extensions;

import net.ravendb.client.util.TimeUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonExtensionsTest {

    @Test
    public void canSerializeDuration() {
        assertThat(serialize(Duration.ZERO))
                .isEqualTo("00:00:00");

        assertThat(serialize(Duration.ofSeconds(2)))
                .isEqualTo("00:00:02");

        assertThat(serialize(Duration.ofMinutes(3)))
                .isEqualTo("00:03:00");

        assertThat(serialize(Duration.ofHours(4)))
                .isEqualTo("04:00:00");

        assertThat(serialize(Duration.ofDays(1)))
                .isEqualTo("1.00:00:00");

        assertThat(serialize(Duration.ofDays(2).plusHours(5).plusMinutes(3).plusSeconds(7)))
                .isEqualTo("2.05:03:07");

        assertThat(serialize(Duration.ofMillis(2)))
                .isEqualTo("00:00:00.0020000");
    }

    @Test
    public void canDeserializeFromTimeSpan() {
        assertThat(deserialize("00:00:01").toString())
                .isEqualTo("PT1S");

        assertThat(deserialize("00:00:00").toString())
                .isEqualTo("PT0S");

        assertThat(deserialize("2.00:00:01").toString())
                .isEqualTo("PT48H1S");

        assertThat(deserialize("00:00:00.1234").toString())
                .isEqualTo("PT0.1234S");
    }


    private static String serialize(Duration duration) {
        return TimeUtils.durationToTimeSpan(duration);
    }

    private static Duration deserialize(String text) {
        return TimeUtils.timeSpanToDuration(text);
    }
}
