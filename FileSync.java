import java.nio.file.Path;
import java.nio.file.Paths;
import Orig.CommandLineParser;
import java.nio.file.WatchEvent;


// FileMirrorSync - A file mirror synchronization tool (one-way file sync, incremental file copy).
// Driver for the command line interface.
public class FileSync {
    private static Path sourcePath;
    private static Path targetPath;
    private static Options options;
    
    private long lastSync = Long.MAX_VALUE;
    
    private boolean isSyncing = false;
    
    
    FileSyncProcessor fsp;
    UserInterface ui;
        
    // Constructor of Class FileSync
    public FileSync(){
        options = new Options();
        //options.debugLevel = 9;
        
        ui = new UserInterface();
        fsp = new FileSyncProcessor(options, ui);
    }
    
    public void setSyncPath(String source, String destination){
        sourcePath = Paths.get(source);
        targetPath = Paths.get(destination);
        
        fsp.setSyncPath(sourcePath, targetPath);
    }
    
    private void checkSyncPath(){
        if (sourcePath == null) throw new CommandLineParser.CommandLineException("Missing source path parameter.");
        if (targetPath == null) throw new CommandLineParser.CommandLineException("Missing target path parameter.");
    }
    
    public boolean isSyncing(){
        return isSyncing;
    }
    
    public int makeManualSync(boolean syncTwoWay){
        isSyncing = true;
        checkSyncPath();
   
        Statistics statistics = new Statistics();
        
        try{
            fsp.executeFullSync(statistics, syncTwoWay, lastSync);
        } 
        catch (Exception e){
            isSyncing = false;
            return 0;
        }
        displayStatistics(statistics);
        
        
        lastSync = System.currentTimeMillis();
        isSyncing = false;
        return 1;
    }
    
    public int makeSyncAfterModify(boolean syncTwoWay, Path file, WatchEvent.Kind kind) {
        //System.out.println("Quelle: " + sourcePath + " -> Ziel: " + targetPath + ", File: " + file);
        isSyncing = true;
        checkSyncPath();
 
        Statistics statistics = new Statistics();
        
        try{
            fsp.syncOnlyOneFile(statistics, syncTwoWay, lastSync, file, kind);
        } 
        catch (Exception e){
            isSyncing = false;
            return 0;
        }
        displayStatistics(statistics);
        
        
        lastSync = System.currentTimeMillis();
        isSyncing = false;
        return 1;
    }

    
    //--- User interface ------------------------------------------------------------------------------
    private static void displayStatistics(Statistics statistics) {
        if (options.verbosityLevel <= 0) return;
        if (statistics.totalDiffs == 0) {
            System.out.println("No differences found.");
            return;
        }
        String would = options.listOnly ? "that would be " : "";
        if (options.verbosityLevel <= 1) {
            System.out.println("Total differences: " + statistics.totalDiffs);
        } else {
            System.out.println("Files " + would + formatDiffs(statistics.fileDiffs));
            System.out.println("Total file differences: " + statistics.totalFileDiffs);
            System.out.println("Directories " + would + formatDiffs(statistics.directoryDiffs));
            System.out.println("Total directory differences: " + statistics.totalDirectoryDiffs);
        }
        System.out.println("Number of bytes " + would + "copied: " + statistics.bytesCopied);
    }

    private static String formatDiffs(long[] diffs) {
        return "added: " + diffs[DiffStatus.add.ordinal()] + ", " +
        "modified: " + diffs[DiffStatus.modify.ordinal()] + ", " +
        "renamed: " + diffs[DiffStatus.rename.ordinal()] + ", " +
        "deleted: " + diffs[DiffStatus.delete.ordinal()];
    }
}