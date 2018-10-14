
import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.awt.*; 
import java.awt.event.*;
import javax.swing.*;


public class MenuBarSupport {
    
    private static JFrame mainFrame;
    private static TrayIcon trayIcon = null;
    
    public MenuBarSupport(JFrame mainFrame){
        this.mainFrame = mainFrame;
    }
    
    public static void createMenuBar(){
        
        if (SystemTray.isSupported()) {
            // get the SystemTray instance
            SystemTray tray = SystemTray.getSystemTray();
            // load an image
            Image image = null;
            try{
                image = Toolkit.getDefaultToolkit().getImage(new URL("https://cdn3.iconfinder.com/data/icons/forall/110/synced-512.png"));
            }
            catch (Exception e)
            {
                System.err.println(e);
            }

            // create a popup menu
            PopupMenu popup = new PopupMenu();
            
            MenuItem defaultItem = new MenuItem("Show Main App");
            defaultItem.addActionListener(new ActionListener() 
            {
                public void actionPerformed(ActionEvent e) {
                    mainFrame.setVisible(true);
                    mainFrame.setAlwaysOnTop(true);
                    mainFrame.requestFocus();
                    mainFrame.requestFocusInWindow();
                }
            });
            popup.add(defaultItem);
            
            MenuItem watcherItem = new MenuItem("Enable Watcher");
            watcherItem.addActionListener(new ActionListener() 
            {
                public void actionPerformed(ActionEvent e) {
                    //Toggle
                    ((MainFrame) mainFrame).autoSyncEnabled = !((MainFrame) mainFrame).autoSyncEnabled;
                    
                    if(((MainFrame) mainFrame).autoSyncEnabled){
                        watcherItem.setLabel("Disable Watcher");
                        setSyncState(false, false);
                    } 
                    else{
                        watcherItem.setLabel("Enable Watcher");
                        setSyncState(false, true);
                    }
                }
            });
            popup.add(watcherItem);
            
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(new ActionListener() 
            {
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
            popup.add(exitItem);
            /// ... add other items
            
            
            // construct a TrayIcon
            trayIcon = new TrayIcon(image, "MacCloudSync", popup);
            // set the TrayIcon properties
            //trayIcon.addActionListener(listener);
            
            // add the tray image
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println(e);
            }
            
            setSyncState(false, true);
            // ...
        } else {
            // disable tray option in your application or
            // perform other actions
            //...
        }
        // ...
        // some time later
        // the application state has changed - update the image
        if (trayIcon != null) {
            //trayIcon.setImage(updatedImage);
        }
    }
    
    public static void setSyncState(boolean doesSync, boolean isPause){
        if(doesSync){
            if (trayIcon != null) {
                Image image = null;
                try{
                    image = Toolkit.getDefaultToolkit().getImage(new URL("https://findicons.com/files/icons/1581/silk/16/arrow_refresh.png"));
                }
                catch (Exception e)
                {
                    System.err.println(e);
                }
                trayIcon.setImage(image);
                
            }
            
        }
        else{
            if (trayIcon != null) {
                Image image = null;
                try{
                    if(isPause){
                        image = Toolkit.getDefaultToolkit().getImage(new URL("https://cdn1.iconfinder.com/data/icons/material-audio-video/20/pause-circle-outline-512.png"));
                    }else
                    {
                        image = Toolkit.getDefaultToolkit().getImage(new URL("https://cdn3.iconfinder.com/data/icons/forall/110/synced-512.png"));
                    }
                }
                catch (Exception e)
                {
                    System.err.println(e);
                }
                trayIcon.setImage(image);
                
            }
        }
    }
}