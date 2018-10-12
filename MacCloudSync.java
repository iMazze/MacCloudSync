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
    public static void main ( String[] args ) throws IOException
    {
        // Create new Frame
        MainFrame frm = new MainFrame("MacCloudSync V 1.0");

        frm.setSize( 250, 200 );
        frm.setVisible( true );
        frm.addTouchBar();
        frm.setVisible( false );

        MenuBarSupport menuBar = new MenuBarSupport(frm);
        menuBar.createMenuBar();

       

        
        Path dir = Paths.get("/Users/imazze/Google Drive/50_Software/MacCloudSync/Testarea/Ziel/");
        
        
        TimerTask ds = new DirectoryWatchService(dir){
            protected void onChange( Path file, WatchEvent.Kind kind ) {
                if(!frm.syncJobEnabled){
                    return;
                }
                
                //System.out.println( "-> File " + file.getName() + " action: " + action );
                System.out.println(kind + ": " + file);
                
                //Change icon of MenuBar
                menuBar.setSyncState(true, false);
                // here we code the action on a change
                try
                {
                    TimeUnit.SECONDS.sleep(3);
                }
                catch (Exception e){
                }
                
                menuBar.setSyncState(false, false);
            }
        };
        
        java.util.Timer timer = new java.util.Timer();
        timer.schedule( ds , new Date(), 1000 );
    }
}
