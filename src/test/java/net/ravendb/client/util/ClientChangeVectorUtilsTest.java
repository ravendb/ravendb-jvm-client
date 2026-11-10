package net.ravendb.client.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ClientChangeVectorUtilsTest {

    @Test
    public void getEtagById_ReturnsEtagOfMatchingDatabaseId() {
        String cv = "A:12-dbIdA, B:7-dbIdB, RAFT:42-clusterTxId";

        assertThat(ClientChangeVectorUtils.getEtagById(cv, "clusterTxId"))
                .isEqualTo(42);
        assertThat(ClientChangeVectorUtils.getEtagById(cv, "dbIdA"))
                .isEqualTo(12);
        assertThat(ClientChangeVectorUtils.getEtagById(cv, "dbIdB"))
                .isEqualTo(7);
    }

    @Test
    public void getEtagById_ReturnsZeroWhenMissing() {
        assertThat(ClientChangeVectorUtils.getEtagById(null, "id"))
                .isZero();
        assertThat(ClientChangeVectorUtils.getEtagById("A:12-dbIdA", "other"))
                .isZero();
    }

    @Test
    public void getEtagById_ThrowsOnNullId() {
        assertThatThrownBy(() -> ClientChangeVectorUtils.getEtagById("A:12-dbIdA", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void getEtagById_ThrowsWhenChangeVectorContainsSeparator() {
        assertThatThrownBy(() -> ClientChangeVectorUtils.getEtagById("A:12-dbIdA|RAFT:3-tx", "tx"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'|'");
    }
}
