public class Statistics {
        public long[] fileDiffs = new long[DiffStatus.values().length];
        public long totalFileDiffs;
        public long[] directoryDiffs = new long[DiffStatus.values().length];
        public long totalDirectoryDiffs;
        public long totalDiffs;
        public long bytesCopied;
    }