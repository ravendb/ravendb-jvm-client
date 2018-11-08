package net.ravendb.client.documents.smuggler;

import net.ravendb.client.Constants;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Comparator;

public final class BackupUtils {
    private BackupUtils() {
    }

    private static final String LEGACY_INCREMENTAL_BACKUP_EXTENSION = ".ravendb-incremental-dump";
    private static final String LEGACY_FULL_BACKUP_EXTENSION = ".ravendb-full-dump";

    public final static String[] BACKUP_FILE_SUFFIXES = new String[] {
            LEGACY_INCREMENTAL_BACKUP_EXTENSION, LEGACY_FULL_BACKUP_EXTENSION,
            Constants.Documents.PeriodicBackup.INCREMENTAL_BACKUP_EXTENSION,
            Constants.Documents.PeriodicBackup.FULL_BACKUP_EXTENSION
    };

    static boolean isIncrementalBackupFile(String extension) {
        return Constants.Documents.PeriodicBackup.INCREMENTAL_BACKUP_EXTENSION.equalsIgnoreCase(extension) ||
                LEGACY_INCREMENTAL_BACKUP_EXTENSION.equalsIgnoreCase(extension);
    }

    public static final Comparator<File> COMPARATOR = (o1, o2) -> {
        String baseName1 = FilenameUtils.getBaseName(o1.getName());
        String baseName2 = FilenameUtils.getBaseName(o2.getName());

        if (!baseName1.equals(baseName2)) {
            return baseName1.compareTo(baseName2);
        }

        String extension1 = FilenameUtils.getExtension(o1.getName());
        String extension2 = FilenameUtils.getExtension(o2.getName());

        if (!extension1.equals(extension2)) {
            return PeriodicBackupFileExtensionComparator.INSTANCE.compare(o1, o2);
        }

        long lastModified1 = o1.lastModified();
        long lastModified2 = o2.lastModified();

        return Long.valueOf(lastModified1).compareTo(lastModified2);
    };
}
