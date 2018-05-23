package net.ravendb.client.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.extensions.JsonExtensions;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class DurationSerializationTest {

    @Test
    public void canSerializeDuration() throws JsonProcessingException {
        ObjectMapper mapper = JsonExtensions.getDefaultMapper();

        String json = mapper.writeValueAsString(Duration.ofDays(5).plusHours(2));

        assertThat(json)
                .isEqualTo("\"5.02:00:00\"");

        json = mapper.writeValueAsString(Duration.ofMillis(5));
        assertThat(json)
                .isEqualTo("\"00:00:00.0050000\"");
    }

    @Test
    public void canDeserializeDuration() throws Exception {
        ObjectMapper mapper = JsonExtensions.getDefaultMapper();

        Duration duration = mapper.readValue("null", Duration.class);
        assertThat(duration)
                .isNull();

        duration = mapper.readValue("\"5.02:00:00\"", Duration.class);
        assertThat(duration.toMillis())
                .isEqualTo(Duration.ofDays(5).plusHours(2).toMillis());

        duration = mapper.readValue("\"00:00:00.0050000\"", Duration.class);
        assertThat(duration.toMillis())
                .isEqualTo(5);

        duration = mapper.readValue("\"00:00:00.005000\"", Duration.class);
        assertThat(duration.toMillis())
                .isEqualTo(5);

        duration = mapper.readValue("\"00:00:00.0050\"", Duration.class);
        assertThat(duration.toMillis())
                .isEqualTo(5);

        duration = mapper.readValue("\"00:00:00.1\"", Duration.class);
        assertThat(duration.toMillis())
                .isEqualTo(100);


    }
}
