package net.ravendb.client.infrastructure;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class EnableOnServerConditionTest {

    @ParameterizedTest
    @CsvSource({
            "7.2, 7.2.7, true",    // CI passes only the release line, which runs its latest nightly
            "7.2, 7.2, true",
            "7.2.8, 7.2.7, true",
            "7.2.7, 7.2.7, true",
            "7.2.10, 7.2.2, true", // numeric, not lexicographic
            "7.2.1, 7.2.2, false",
            "7.1, 7.2, false",
            "7.1, 7.2.2, false",
            "6.2, 7.2, false",
            "8.0, 7.2.7, true"
    })
    public void comparesVersionsNumerically(String serverVersion, String threshold, boolean expected) {
        assertThat(EnableOnServerCondition.isAtLeast(serverVersion, threshold)).isEqualTo(expected);
    }
}
