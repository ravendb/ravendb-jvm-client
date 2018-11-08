package net.ravendb.client.documents.smuggler;

import net.ravendb.client.Constants;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Comparator;

public class PeriodicBackupFileExtensionComparator implements Comparator<File> {

    public static final PeriodicBackupFileExtensionComparator INSTANCE = new PeriodicBackupFileExtensionComparator();

    @Override
    public int compare(File o1, File o2) {
        if (o1.getAbsolutePath().equals(o2.getAbsolutePath())) {
            return 0;
        }

        if (FilenameUtils.getExtension(o1.getName()).equalsIgnoreCase(Constants.Documents.PeriodicBackup.SNAPSHOT_EXTENSION)) {
            return -1;
        }

        if (FilenameUtils.getExtension(o1.getName()).equalsIgnoreCase(Constants.Documents.PeriodicBackup.FULL_BACKUP_EXTENSION)) {
            return -1;
        }

        return 1;
    }
}
