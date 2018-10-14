import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.FileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.nio.file.WatchEvent;

public class FileSyncProcessor {
    public static interface UserInterface {
        void writeInfo(String s);

        void writeInfoTicker();

        void endInfoTicker();

        void writeDebug(String s);

        void listItem(String relativePath, DiffStatus diffStatus);
    }


    private static final String keyPathSeparator = "\u0000"; // path separator used for the internal key string
    private static final long oneHourInMillis = 3600000;
    private static final long mirrorTickerInterval = 1000;

    private Options options;
    private UserInterface ui;
    private Path sourcePathAbs;
    private Path targetPathAbs;
    private FileSystem sourceFileSystem;
    private FileSystem targetFileSystem;
    public static Path sourceBaseDir;
    public static Path targetBaseDir;
    private String sourcePathSeparator;
    private String targetPathSeparator;
    private boolean sourceFileSystemIsCaseSensitive;
    private boolean targetFileSystemIsCaseSensitive;
    private boolean caseSensitive;
    private HashMap < String, SyncItem > itemMap; // maps key strings to SyncItems
    private ArrayList < SyncItem > itemList; // items sorted by key
    private int itemListPos; // current position in itemList
    private long mirrorTickerCounter;
    private Statistics statistics;
    
    private long lastSync = Long.MAX_VALUE; 

    public FileSyncProcessor(Options options, UserInterface ui){
        this.options = options;
        this.ui = ui;
    }
    
    public void setSyncPath(Path sourcePath, Path targetPath){
        this.sourcePathAbs = sourcePath.toAbsolutePath().normalize();
        this.targetPathAbs = targetPath.toAbsolutePath().normalize();
    }
    
    //--- Main ---------------------------------------------------------------------
    public void executeFullSync(Statistics statistics, boolean twoWaySync, long lastSync) throws Exception {  
        this.statistics = statistics;
        this.lastSync = lastSync;
        init();
        readDirectoryTrees();
        
        //System.out.println("Last Sync: " + lastSync);
        
        // Comparison of the Files
        if (!compareFiles(twoWaySync)) {
            return;
        }
        
        if (options.listOnly) {
            listFiles();
        } else {
            if(twoWaySync){
                System.out.println("Balancing Files");
                mirrorFiles();
            } 
            else{
                System.out.println("Mirroring Files");
                mirrorFiles();
            }
        }
    }
    
    public void syncOnlyOneFile(Statistics statistics, boolean twoWaySync, long lastSync, Path file, WatchEvent.Kind kind) throws Exception {
        this.statistics = statistics;
        this.lastSync = lastSync;
        init();
        readDirectoryTrees();
        
        //System.out.println("Last Sync: " + lastSync);
        
        // Comparison of the Files
        if (!compareFilesWithDetection(twoWaySync, file, kind)) {
            return;
        }
        
        
        if (options.listOnly) {
            listFiles();
        } else {
            if(twoWaySync){
                System.out.println("Balancing Files");
                mirrorFiles();
            } 
            else{
                System.out.println("Mirroring Files");
                mirrorFiles();
            }
            
        }
    }
    
    // --- Init ----------------------
    private void init() throws Exception {
        if (!Files.exists(sourcePathAbs)) throw new Exception("The source path does not exist.");
        if (sourcePathAbs.equals(targetPathAbs)) throw new Exception("Source and target paths are equal.");
        BasicFileAttributes sourcePathAttrs = Files.readAttributes(sourcePathAbs, BasicFileAttributes.class);
        if (sourcePathAttrs.isDirectory()) {
            sourceBaseDir = sourcePathAbs;
        } else { // source path is file
            sourceBaseDir = sourcePathAbs.getParent();
        }
        BasicFileAttributes targetPathAttrs = !Files.exists(targetPathAbs) ? null : Files.readAttributes(targetPathAbs, BasicFileAttributes.class);
        if (targetPathAttrs != null && !targetPathAttrs.isDirectory()) {
            throw new Exception("Target path exists and is not a directory.");
        }
        targetBaseDir = targetPathAbs;
        sourceFileSystem = sourcePathAbs.getFileSystem();
        targetFileSystem = targetPathAbs.getFileSystem();
        sourceFileSystemIsCaseSensitive = isFileSystemCaseSensitive(sourceFileSystem);
        targetFileSystemIsCaseSensitive = isFileSystemCaseSensitive(targetFileSystem);
        caseSensitive = !options.ignoreCase && sourceFileSystemIsCaseSensitive && targetFileSystemIsCaseSensitive;
        // Unless boths file systems are case-sensitive, we use case-insensitive comparisons for file names.
        sourcePathSeparator = sourceFileSystem.getSeparator();
        targetPathSeparator = targetFileSystem.getSeparator();
    }

    private static boolean isFileSystemCaseSensitive(FileSystem fileSystem) {
        return !fileSystem.getPath("a").equals(fileSystem.getPath("A"));
    }

    //--- Read directory trees -----------------------------------------------------
    private void readDirectoryTrees() throws Exception {
        //All items in the Sync Process (Source like Destination)
        itemMap = new HashMap < String, SyncItem > (4096);
        
        readSourceDirectoryTree();
        readTargetDirectoryTree();
        
        itemList = new ArrayList < SyncItem > (itemMap.values());
        Collections.sort(itemList);
        itemMap = null; // Free space
    }

    private void readSourceDirectoryTree() throws Exception {
        if (options.verbosityLevel >= 1) ui.writeInfo("Reading source directory.");
        FileSyncFileVisitor fileVisitor = new FileSyncFileVisitor();
        fileVisitor.isSource = true;
        Files.walkFileTree(sourcePathAbs, fileVisitor);
        if (options.verbosityLevel >= 2) ui.endInfoTicker();
    }

    private void readTargetDirectoryTree() throws Exception {
        if (options.verbosityLevel >= 1) ui.writeInfo("Reading target directory.");
        if (!Files.exists(targetPathAbs)) return;
        FileSyncFileVisitor fileVisitor = new FileSyncFileVisitor();
        fileVisitor.isSource = false;
        Files.walkFileTree(targetPathAbs, fileVisitor);
        if (options.verbosityLevel >= 2) ui.endInfoTicker();
    }

    private class FileSyncFileVisitor extends SimpleFileVisitor < Path > {
        private static final long tickerInterval = 10000;
        boolean isSource; // true=processing source tree, false=processing target tree
        long tickerCounter;
        @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (options.debugLevel >= 9) ui.writeDebug("preVisitDirectory: " + dir);
            ticker();
            if (!isPathIncluded(dir, isSource)) {
                if (options.debugLevel >= 6) System.out.println("Directory excluded: \"" + dir + "\".");
                return FileVisitResult.SKIP_SUBTREE;
            }
            registerSyncItem(dir, attrs, true, isSource);
            return FileVisitResult.CONTINUE;
        }

        @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            ticker();
            if (options.debugLevel >= 9) ui.writeDebug("visitFile: " + file);
            if (!isPathIncluded(file, isSource)) {
                if (options.debugLevel >= 7) ui.writeDebug("File excluded: \"" + file + "\".");
                return FileVisitResult.CONTINUE;
            }
            registerSyncItem(file, attrs, false, isSource);
            return FileVisitResult.CONTINUE;
        }

        @Override public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
            ticker();
            if (options.debugLevel >= 9) ui.writeDebug("VisitFileFailed for \"" + file + "\", exception=" + e);
            // Problem: Under Windows, an AccessDeniedException occurs for "\System Volume Information" before
            // preVisitDirectory() is called. We solve this by ignoring visitFileFailed for files/directories that
            // are not included.
            if (!isPathIncluded(file, isSource)) {
                return FileVisitResult.CONTINUE;
            }
            throw e;
        }

        private final void ticker() {
            if (options.verbosityLevel >= 2) {
                if (++tickerCounter % tickerInterval == 0) {
                    ui.writeInfoTicker();
                }
            }
        }
    }

    private void registerSyncItem(Path path, BasicFileAttributes attrs, boolean isDirectory, boolean isSource) throws IOException {
       
        
        String relativePath = genRelativePath(path, isSource);
        if (relativePath == null) return; // ignore base directories
        String key = genSyncItemKey(relativePath, isSource);
        
        SyncItem item = itemMap.get(key); 
        if (item == null) { //If ANY item isnt in list
            //System.out.println("  Registering of: " + path.toString());
            
            item = new SyncItem();
            item.key = key;
            itemMap.put(key, item);
        }
        
        //Modifiying the item in the Map
        if (isSource) { //Its the Source
            if (item.sourceExists) { //Error
                throw new IOException("File name collision in source directory tree, " +
                    "path1=\"" + item.sourceRelativePath + "\", path2=\"" + relativePath + "\".");
            }
            item.sourceExists = true;
            item.sourceRelativePath = relativePath;
            item.sourceName = path.getFileName().toString();
            item.sourceIsDirectory = isDirectory;
            if (!isDirectory) {
                item.sourceFileSize = attrs.size();
                item.sourceLastModifiedTime = attrs.lastModifiedTime().toMillis();
                item.sourceCreatedTime = attrs.creationTime().toMillis();
            }
        } else { //Its the Destination
            if (item.targetExists) { //Error
                throw new IOException("File name collision in target directory tree, " +
                    "path1=\"" + item.targetRelativePath + "\", path2=\"" + relativePath + "\".");
            }
            item.targetExists = true;
            item.targetRelativePath = relativePath;
            item.targetName = path.getFileName().toString();
            item.targetIsDirectory = isDirectory;
            if (!isDirectory) {
                item.targetFileSize = attrs.size();
                item.targetLastModifiedTime = attrs.lastModifiedTime().toMillis();
                item.targetCreatedTime = attrs.creationTime().toMillis();
            }
        }
    }

    private String genRelativePath(Path path, boolean isSource) {
        Path relPath;
        if (isSource) {
            relPath = sourceBaseDir.relativize(path);
        } else {
            relPath = targetBaseDir.relativize(path);
        }
        if (relPath == null || relPath.toString().length() == 0) return null;
        return relPath.toString();
    }

    private String genSyncItemKey(String relativePath, boolean isSource) {
        String s = relativePath;
        if (!caseSensitive) {
            s = s.toUpperCase();
        }
        if (isSource) {
            s = fastReplace(s, sourcePathSeparator, keyPathSeparator);
        } else {
            s = fastReplace(s, targetPathSeparator, keyPathSeparator);
        }
        return s;
    }

    private boolean isPathIncluded(Path path, boolean isSource) throws IOException {
        // Under Windows, the root directory of a drive (e.g. "c:\") has the system and hidden attributes set.
        // We solve this problem by always including the root source and target directories.
        if (isSource) {
            if (path.equals(sourcePathAbs)) return true;
        } else {
            if (path.equals(targetPathAbs)) return true;
        }
        
        if (options.ignoreSystemHiddenFiles) {
            DosFileAttributes dosAttrs = getDosFileAttributes(path);
            if (dosAttrs != null && dosAttrs.isSystem() && dosAttrs.isHidden()) {
                return false;
            }
        }
        return true;
    }

    private DosFileAttributes getDosFileAttributes(Path path) throws IOException {
        try {
            return Files.readAttributes(path, DosFileAttributes.class);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    //--- Compare files ------------------------------------------------------------

    private boolean compareFiles(boolean compareTwoWay) {
        if (options.debugLevel >= 2) ui.writeDebug("Comparing directory entries.");
        boolean differencesFound = false;
        
        // for each item
        for (SyncItem item: itemList) {
            if(compareTwoWay){
                item.diffStatus = compareItemTwoWay(item);
            }
            else
            {
                item.diffStatus = compareItem(item);
            }
            
            if (item.diffStatus != null) {
                differencesFound = true;
                System.out.println("  -> " + item.key + ": " + item.diffStatus);
            }
        }
        
        return differencesFound;
    }
    
    private boolean compareFilesWithDetection(boolean compareTwoWay, Path file, WatchEvent.Kind kind){
        if (options.debugLevel >= 2) ui.writeDebug("Comparing directory entries with detected Change.");
        boolean differencesFound = false;
  
        // for each item
        for (SyncItem item: itemList) {
            //System.out.println("File: \n --> Old: " + item.getOldTargetPath() + "\n --> New: "+ item.getNewTargetPath() + "\n --> Source: "+ item.getSourcePath());
            // TODO
            // Changes only can be in target
            if(compareTwoWay){
                if(item.isTheSame(file)){
                    System.out.println("File found!: " + file);
                    switch(kind.name()){
                        case "ENTRY_CREATE": 
                            item.diffStatus = DiffStatus.addSource;
                            break;
                        case "ENTRY_DELETE": 
                            item.diffStatus = DiffStatus.deleteSource;
                            break;
                        case "ENTRY_MODIFY": 
                            item.diffStatus = DiffStatus.modifySource;
                            break;
                        default: item.diffStatus = null;
                    }
                    
                    if(!item.sourceExists){
                        //Darf nur bei Create auftreten!!!
                        System.out.println("Source file doesnt exist!!!");
                    }
                } else{
                    item.diffStatus = null;
                }
            }
            else
            {
                // No Operation for source to do
                item.diffStatus = null;
            }
            
            if (item.diffStatus != null) {
                differencesFound = true;
                System.out.println("  -> " + item.key + ": " + item.diffStatus);
            }
        }
        
        if (differencesFound == false) {
                System.out.println("File couldtn found wether in Source or Target!!");
        }
            
        return differencesFound;
    }
    
    // Returns null if source and target are equal.
    private DiffStatus compareItem(SyncItem item) {
        if (!item.targetExists) return DiffStatus.add;
        if (!item.sourceExists) return DiffStatus.delete;
        if (item.sourceIsDirectory != item.targetIsDirectory) return DiffStatus.modify;
        boolean isDirectory = item.sourceIsDirectory;
        if (!isDirectory) {
            if (item.sourceFileSize != item.targetFileSize) return DiffStatus.modify;
            long timeDiff = Math.abs(item.sourceLastModifiedTime - item.targetLastModifiedTime);
            if (options.summerTimeTolerance && timeDiff >= oneHourInMillis) {
                timeDiff = timeDiff - oneHourInMillis;
            }
            if (timeDiff > options.timeTolerance) return DiffStatus.modify;
        }
        if (options.renameCase && !item.sourceName.equals(item.targetName)) return DiffStatus.rename;
        return null;
    }
    
    private DiffStatus compareItemTwoWay(SyncItem item) {
        //System.out.println("Comparison of item for TwoWay Sync: " + item.getRelativePath());
       
        // Deleted
        // Datei in Quelle nicht vorhanden, aber ziel vor letzten Sync erstellt -> lösche in Ziel
        if(!item.sourceExists && item.targetExists && item.targetCreatedTime < lastSync) return DiffStatus.delete;
        
        // Datei im Ziel nicht vorhanden, aber Quelle vor letzten Sync erstellt -> lösche in Quelle
        if(item.sourceExists && !item.targetExists && item.sourceCreatedTime < lastSync) return DiffStatus.deleteSource;
        
        // Datei nur in Quelle vorhanden
        // -> Zum Ziel hinzufügen
        if (item.sourceExists && !item.targetExists) return DiffStatus.add;
        
        // Datei nur im Ziel vorhanden
        // -> Zur Quelle hinzufügen
        if (item.targetExists && !item.sourceExists) return DiffStatus.addSource;
        
        // ???
        if (item.sourceIsDirectory != item.targetIsDirectory) return DiffStatus.modify;
        

        boolean isDirectory = item.sourceIsDirectory;
        if (!isDirectory) {
            if (item.sourceFileSize != item.targetFileSize) {
                //Welches wurde zuletzt geändert???
                // Quelle ist neuer
                if(item.sourceLastModifiedTime > item.targetLastModifiedTime){
                    return DiffStatus.modify;
                }
                else{ // Ziel ist neuer
                    return DiffStatus.modifySource;
                }
            }
            
            long timeDiff = Math.abs(item.sourceLastModifiedTime - item.targetLastModifiedTime);
            
            if (options.summerTimeTolerance && timeDiff >= oneHourInMillis) {
                timeDiff = timeDiff - oneHourInMillis;
            }
            if (timeDiff > options.timeTolerance) return DiffStatus.modify;
        }
        if (options.renameCase && !item.sourceName.equals(item.targetName)) return DiffStatus.rename;
        return null;
    }

    //--- List files ---------------------------------------------------------------

    private void listFiles() {
        for (SyncItem item: itemList) {
            if (item.diffStatus != null && options.verbosityLevel >= 5) {
                listItem(item);
            }
            updateStatistics(item);
        }
    }

    private void listItem(SyncItem item) {
        ui.listItem(item.getRelativePath(), item.diffStatus);
    }

    //--- Mirror copy files --------------------------------------------------------

    private void mirrorFiles() throws Exception {
        mirrorTickerCounter = 0;
        if (options.verbosityLevel >= 1) ui.writeInfo("Transferring files.");
        createTargetBaseDirectory();
        itemListPos = 0;
        while (itemListPos < itemList.size()) {
            SyncItem item = itemList.get(itemListPos++);
            mirrorItem(item);
        }
    }

    private void mirrorItem(SyncItem item) throws Exception {
        if (item.diffStatus == null) return;
        if (options.verbosityLevel >= 5) {
            listItem(item);
        } else if (options.verbosityLevel >= 2) {
            if (++mirrorTickerCounter % mirrorTickerInterval == 0) ui.writeInfoTicker();
        }
        switch (item.diffStatus) {
            case add:
                addItem(item);
                break;
            case addSource:
                addItem(item, true);
                break;
            case modify:
                modifyItem(item);
                break;
            case modifySource:
                modifyItem(item, true);
                break;
            case rename:
                renameItem(item);
                break;
            case delete:
                deleteItem(item);
                break; 
            case deleteSource:
                deleteItem(item, true);
                break;
            default:
            throw new AssertionError();
        }
        updateStatistics(item);
    }
    
    private void addItem(SyncItem item) throws Exception {
        addItem(item, false);
    }

    private void addItem(SyncItem item, boolean toSource) throws Exception {
        if(toSource){
            Path sourcePath = item.getOldTargetPath(); //ok
            Path targetPath = FileSyncProcessor.sourceBaseDir.resolve(FileSyncProcessor.sourceBaseDir + "/" + item.getRelativePath()); //TODO!!!
            
            if (item.targetIsDirectory) {
                if (options.debugLevel >= 3) ui.writeDebug("Creating directory \"" + targetPath + "\".");
                copyEmptyDirectory(sourcePath, targetPath);
            } else {
                if (options.debugLevel >= 3) ui.writeDebug("Copying file \"" + sourcePath + "\" to \"" + targetPath + "\".");
                copyFile(sourcePath, targetPath);
            }
        }
        else{
            Path sourcePath = item.getSourcePath();
            Path targetPath = item.getNewTargetPath();
            if (item.sourceIsDirectory) {
                if (options.debugLevel >= 3) ui.writeDebug("Creating directory \"" + targetPath + "\".");
                copyEmptyDirectory(sourcePath, targetPath);
            } else {
                if (options.debugLevel >= 3) ui.writeDebug("Copying file \"" + sourcePath + "\" to \"" + targetPath + "\".");
                copyFile(sourcePath, targetPath);
            }
        }
        
    }

    private void copyEmptyDirectory(Path sourcePath, Path targetPath) throws Exception {
        Files.copy(sourcePath, targetPath, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private void copyFile(Path sourcePath, Path targetPath) throws Exception {
        // To prevent a partially copied file when the program is interrupted,
        // we first copy to a temporary file and then rename the copied file.
        Path tempPath = targetPath.getParent().resolve("$$tempFileMirrorSyncOutputFile$$.tmp");
        Files.copy(sourcePath, tempPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private void deleteItem(SyncItem item) throws Exception {
        deleteItem(item, false);
    }
    
    private void deleteItem(SyncItem item, boolean toSource) throws Exception {
        Path path;
        
        if(toSource){
            if (item.sourceIsDirectory) {
                //System.out.println("Delete Directory Key: " + sourceBaseDir + "/" + item.key);
                deleteDirectoryContents(item.key);
            } // (recoursive)
            path = item.getSourcePath();
        }
        else{
            if (item.targetIsDirectory) {
                deleteDirectoryContents(item.key);
            } // (recoursive)
            path = item.getOldTargetPath();
        }
        
        if (options.debugLevel >= 3) ui.writeDebug("Deleting \"" + path + "\".");
        deletePath(path);
    }

    private void deletePath(Path path) throws Exception {
        try {
            Files.delete(path);
        } catch (AccessDeniedException e) {
            if (!tryToResetReadOnlyAttribute(path)) throw e;
            Files.delete(path);
        }
    }

    private boolean tryToResetReadOnlyAttribute(Path path) {
        try {
            DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
            if (view == null) return false;
            if (!view.readAttributes().isReadOnly()) return false;
            view.setReadOnly(false);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    // Deletes the contents of a directory.
    private void deleteDirectoryContents(String key) throws Exception {
        String keyStart = key + keyPathSeparator;
        System.out.println("deleteDirectoryContents" + keyStart);
        while (itemListPos < itemList.size()) {
            SyncItem item = itemList.get(itemListPos);
            if (!item.key.startsWith(keyStart)) break; // end of directory reached
            itemListPos++;
            if (item.diffStatus != DiffStatus.delete) {
                throw new Exception("Unexpected file/directory found in directory that should be deleted: \"" + item.getRelativePath() + "\".");
            }
            mirrorItem(item);
        }
    }

    private void modifyItem(SyncItem item) throws Exception {
        modifyItem(item, false);
    }
    
    private void modifyItem(SyncItem item, boolean toSource) throws Exception {
        deleteItem(item, toSource);
        addItem(item, toSource);
    }

    private void renameItem(SyncItem item) throws Exception {
        Path oldPath = item.getOldTargetPath();
        Path newPath = item.getNewTargetPath();
        if (options.debugLevel >= 3) ui.writeDebug("Renaming \"" + oldPath + "\" to \"" + newPath + "\".");
        Files.move(oldPath, newPath, StandardCopyOption.ATOMIC_MOVE);
        // Without the ATOMIC_MOVE option, the file is not renamed in Windows, because the old and the new
        // file name differ only in upper/lower case.
        if (item.targetIsDirectory) {
            fixupTargetPaths(item.key, item.targetRelativePath, item.sourceRelativePath);
        }
    }

    // When a target directory is renamed, we fix up the relative target path strings of all files
    // and subdirectories contained in that directory.
    private void fixupTargetPaths(String key, String oldPath, String newPath) {
        String keyStart = key + keyPathSeparator;
        String oldPathStart = oldPath + targetPathSeparator;
        String newPathStart = fastReplace(newPath, sourcePathSeparator, targetPathSeparator) + targetPathSeparator;
        int i = itemListPos;
        while (i < itemList.size()) {
            SyncItem item = itemList.get(i++);
            if (!item.key.startsWith(keyStart)) break; // end of directory reached
            if (!item.targetExists) continue;
            if (item.targetRelativePath.startsWith(oldPathStart)) {
                String s = newPathStart + item.targetRelativePath.substring(oldPathStart.length());
                if (options.debugLevel >= 9) ui.writeDebug("Fixup target path: from \"" + item.targetRelativePath + "\" to \"" + s + "\".");
                item.targetRelativePath = s;
            }
        }
    }

    private void createTargetBaseDirectory() throws Exception {
        if (Files.exists(targetBaseDir)) return;
        if (options.debugLevel >= 3) ui.writeDebug("Creating target base directory \"" + targetBaseDir + "\".");
        Files.createDirectories(targetBaseDir);
    }

    //--- Statistics ---------------------------------------------------------------

    private void updateStatistics(SyncItem item) {
        if (item.diffStatus == null) {
            return;
        }
        boolean isDirectory = item.sourceExists ? item.sourceIsDirectory : item.targetIsDirectory;
        if (isDirectory) {
            statistics.directoryDiffs[item.diffStatus.ordinal()]++;
            statistics.totalDirectoryDiffs++;
        } else {
            statistics.fileDiffs[item.diffStatus.ordinal()]++;
            statistics.totalFileDiffs++;
            if (item.diffStatus == DiffStatus.add || item.diffStatus == DiffStatus.modify || item.diffStatus == DiffStatus.addSource || item.diffStatus == DiffStatus.modifySource) {
                statistics.bytesCopied += item.sourceFileSize;
            }
        }
        statistics.totalDiffs++;
    }

    //--- General tools ------------------------------------------------------------

    // String.replace(CharSequence, CharSequence) is slow in JDK7 beta.
    private static String fastReplace(String s, String s1, String s2) {
        if (s1.length() != 1 || s2.length() != 1) {
            if (s1.equals(s2)) return s;
            return s.replace(s1, s2);
        }
        char c1 = s1.charAt(0);
        char c2 = s2.charAt(0);
        if (c1 == c2) return s;
        return s.replace(c1, c2);
    }

} // end class FileSyncProcessor