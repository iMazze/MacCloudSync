import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;

import com.thizzer.jtouchbar.*;
import com.thizzer.jtouchbar.item.*;
import com.thizzer.jtouchbar.item.view.*;
import com.thizzer.jtouchbar.item.view.action.*;
import com.thizzer.jtouchbar.common.*;
import com.thizzer.jtouchbar.common.Image.*;

import java.util.concurrent.TimeUnit;


public class MacCloudSync
{
    public static void main ( String[] args )
    {
        // Create new Frame
        MainFrame frm = new MainFrame("MacCloudSync V 1.0");

        frm.setSize( 250, 200 );
        frm.setVisible( true );
        frm.addTouchBar();
        frm.setVisible( false );

        MenuBarSupport menuBar = new MenuBarSupport(frm);
        menuBar.createMenuBar();

        
        
        TimerTask task = new DirWatcher(frm.getDestinationPath()) 
        {
            protected void onChange( File file, String action ) {
                if(!frm.syncJobEnabled){
                    return;
                }
                
                System.out.println( "-> File " + file.getName() + " action: " + action );
                
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
        timer.schedule( task , new Date(), 1000 );
    }
}
