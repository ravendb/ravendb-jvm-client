package net.ravendb.client.test.issues;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.documents.operations.backups.PeriodicBackupStatus;
import net.ravendb.client.extensions.JsonExtensions;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_22849Test {

    @Test
    public void periodicBackupStatusIsDeserializedLikeCSharp() throws Exception {
        ObjectMapper mapper = JsonExtensions.getDefaultMapper();

        String json = "{" +
                "\"TaskId\": 1," +
                "\"IsFull\": true," +
                "\"LastFullBackupInternal\": \"2024-01-01T10:00:00.0000000Z\"," +
                "\"DurationInMs\": 1500," +
                "\"LastRaftIndex\": { \"LastEtag\": 10, \"LastFullBackupEtag\": 7 }," +
                "\"LocalBackup\": { \"FullBackupDurationInMs\": 100, \"IncrementalBackupDurationInMs\": 42 }," +
                "\"UploadToGoogleCloud\": { \"Skipped\": true }" +
                "}";

        PeriodicBackupStatus status = mapper.readValue(json, PeriodicBackupStatus.class);

        assertThat(status.getLastRaftIndex().getLastEtag())
                .isEqualTo(10L);
        assertThat(status.getLastRaftIndex().getLastFullBackupEtag())
                .isEqualTo(7L);

        Date endTime = status.getEndTime();
        assertThat(endTime)
                .isNotNull();
        assertThat(endTime.getTime() - status.getLastFullBackupInternal().getTime())
                .isEqualTo(1500L);

        // pre-existing Java bugs: these properties did not match the server names, so they were never filled
        assertThat(status.getLocalBackup().getIncrementalBackupDurationInMs())
                .isEqualTo(42L);
        assertThat(status.getUploadToGoogleCloud())
                .isNotNull();
        assertThat(status.getUploadToGoogleCloud().isSkipped())
                .isTrue();
    }

    @Test
    public void endTimeIsNullWithoutBackup() {
        PeriodicBackupStatus status = new PeriodicBackupStatus();
        assertThat(status.getEndTime())
                .isNull();
    }
}
