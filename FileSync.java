// Copyright 2010 Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland
// www.source-code.biz, www.inventec.ch/chdh
//
// This module is multi-licensed and may be used under the terms of any of the following licenses:
//
//  LGPL, GNU Lesser General Public License, V2.1 or later, http://www.gnu.org/licenses/lgpl.html
//  EPL, Eclipse Public License, V1.0 or later, http://www.eclipse.org/legal
//
// Please contact the author if you need another license.
// This module is provided "as is", without warranties of any kind.
//
// Home page: http://www.source-code.biz/filemirrorsync

import java.nio.file.Path;
import java.nio.file.Paths;
import Orig.CommandLineParser;

// FileMirrorSync - A file mirror synchronization tool (one-way file sync, incremental file copy).
// Driver for the command line interface.
public class FileSync {

    private static Path sourcePath;
    private static Path targetPath;
    private static Options options;
    private static boolean displayHelpOption;
    private static int exitStatusCode;
    private long lastSync = Long.MAX_VALUE;
    
    
    public int makeSync(String source, String destination, boolean syncTwoWay){
        options = new Options();
        //options.debugLevel = 9;
        sourcePath = Paths.get(source);
        targetPath = Paths.get(destination);
        
        if (sourcePath == null) throw new CommandLineParser.CommandLineException("Missing source path parameter.");
        if (targetPath == null) throw new CommandLineParser.CommandLineException("Missing target path parameter.");
    
        FileSyncProcessor fsp = new FileSyncProcessor();
        UserInterface ui = new UserInterface();
        Statistics statistics = new Statistics();
        
        try{
            fsp.main(sourcePath, targetPath, options , ui, statistics, syncTwoWay, lastSync);
        } 
        catch (Exception e){
            
        }
        displayStatistics(statistics);
        
        
        lastSync = System.currentTimeMillis();
        return 0;
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