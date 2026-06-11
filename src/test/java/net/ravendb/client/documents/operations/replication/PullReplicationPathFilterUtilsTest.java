package net.ravendb.client.documents.operations.replication;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PullReplicationPathFilterUtilsTest {

    @Test
    public void normalizeTrimsAndDropsEmptyEntries() {
        String[] result = PullReplicationPathFilterUtils.normalize(new String[]{" users/* ", "", "  ", null, "orders/"});

        assertThat(result)
                .containsExactly("users/*", "orders/");
    }

    @Test
    public void normalizeReturnsNullForNullInput() {
        assertThat(PullReplicationPathFilterUtils.normalize(null)).isNull();
    }

    @Test
    public void normalizeReturnsEmptyArrayWhenAllEntriesAreBlank() {
        assertThat(PullReplicationPathFilterUtils.normalize(new String[]{" ", "", null})).isEmpty();
    }

    @Test
    public void normalizeAndValidateAcceptsWildcardPrecededBySlashOrDash() {
        assertThat(PullReplicationPathFilterUtils.normalizeAndValidate(new String[]{"users/*", "orders-*"}, "filter"))
                .containsExactly("users/*", "orders-*");
    }

    @Test
    public void normalizeAndValidateRejectsWildcardWithInvalidPrecedingCharacter() {
        assertThatThrownBy(() -> PullReplicationPathFilterUtils.normalizeAndValidate(new String[]{"users*"}, "filter"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("the previous character must be '/' or '-'");
    }

    @Test
    public void normalizeAndValidateAcceptsSingleWildcard() {
        assertThat(PullReplicationPathFilterUtils.normalizeAndValidate(new String[]{"*"}, "filter"))
                .containsExactly("*");
    }
}
