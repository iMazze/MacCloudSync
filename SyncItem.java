import java.io.IOException;

import java.nio.file.Path;

import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;

public class SyncItem implements Comparable <SyncItem> {
        DiffStatus diffStatus; // null=equal
        String key;
        boolean sourceExists;
        boolean targetExists;
        String sourceRelativePath;
        String targetRelativePath;
        String sourceName;
        String targetName;
        boolean sourceIsDirectory;
        boolean targetIsDirectory;
        long sourceFileSize;
        long targetFileSize;
        long sourceCreatedTime;      // in milliseconds
        long targetCreatedTime;      // in milliseconds
        long sourceLastModifiedTime; // in milliseconds
        long targetLastModifiedTime; // in milliseconds
        
        @Override public int compareTo(SyncItem o) {
            //System.out.println("-> Compare Key " + key + ", with " + o.key);
            return key.compareTo(o.key);
        }

        public String getRelativePath() {
            return (sourceRelativePath != null) ? sourceRelativePath : targetRelativePath;
        }

        public Path getSourcePath() {
            return FileSyncProcessor.sourceBaseDir.resolve(sourceRelativePath);
        }

        public Path getOldTargetPath() {
            return FileSyncProcessor.targetBaseDir.resolve(targetRelativePath);
        }

        public Path getNewTargetPath() {
            return FileSyncProcessor.targetBaseDir.resolve(sourceRelativePath);
        }
    }