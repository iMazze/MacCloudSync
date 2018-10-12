public class Options {
        public boolean listOnly;
        // List only, without changing anything in the target directory tree.
        public boolean ignoreCase;
        // Ignore case when associating source file/directory names with target file/directory names.
        // This is automatically done when one or both of the file systems are case-insensitive.
        public boolean renameCase = true;
        // True to rename target files/directories when the names of source and target only differ in case.
        public int timeTolerance = 1999; // in milliseconds
        // Tolerance in milliseconds for comparing the last modified time of files.
        // We assume 1.999 seconds, because the FAT file system only has 2 seconds resolution.
        public boolean summerTimeTolerance = true;
        // True to ignore time offsets of +/- 1 hour.
        // Offsets of +/- 1 hour may occur when one copy of the files is stored on a file system
        // that uses UTC time (e.g. NTFS), and the other version on a file system that uses local
        // time (e.g. FAT).
        public boolean ignoreSystemHiddenFiles = true;
        // True to ignore files and directories that have both the system and the hidden
        // attributes set. When enabled, files and directories with these attributes are ignored in the
        // source and in the target directory trees.
        public int verbosityLevel = 5; // 0..9
        public int debugLevel;
    } // 0..9