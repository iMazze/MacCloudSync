import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;
import java.nio.file.WatchEvent;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.thizzer.jtouchbar.*;
import com.thizzer.jtouchbar.item.*;
import com.thizzer.jtouchbar.item.view.*;
import com.thizzer.jtouchbar.item.view.action.*;
import com.thizzer.jtouchbar.common.*;
import com.thizzer.jtouchbar.common.Image.*;

import java.util.concurrent.TimeUnit;


public class MacCloudSync
{
    static FileSync fsync;
    static final boolean useOnyOneFileForSync = false;
    
    
    public static void main ( String[] args ) throws IOException
    {
        fsync = new FileSync();
        
        
        // Create new Frame ---------------------------------------------------------
        MainFrame frm = new MainFrame("MacCloudSync V 1.1", fsync);
        frm.setSize( 250, 200 );
        frm.setVisible( true );
        frm.addTouchBar();
        frm.setVisible( false );

        MenuBarSupport menuBar = new MenuBarSupport(frm);
        menuBar.createMenuBar();
        
        TimerTask task = new DirectoryWatchService(Paths.get(frm.getDestinationPath()))
        {
            protected void onChange( Path file, WatchEvent.Kind kind ) {
                // Do nothing if deactivated or is syncing now
                if(!frm.autoSyncEnabled || fsync.isSyncing()){
                    return;
                }
                
                System.out.println("File " + file + " was " + kind);
                
                //Change icon of MenuBar
                menuBar.setSyncState(true, false);
                
                // here we code the action on a change
                fsync.setSyncPath(frm.getSourcePath(), frm.getDestinationPath());
                
                if(useOnyOneFileForSync){
                    //Replace filename
                    fsync.makeSyncAfterModify(true, Paths.get(file.toString().replace(frm.getDestinationPath(), "")), kind);
                } else {
                    fsync.makeManualSync(true);
                }
                
                menuBar.setSyncState(false, false);
            }
        };
        
        java.util.Timer timer = new java.util.Timer();
        timer.schedule( task , new Date(), 1000 );
    }
}
