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

class MainFrame extends JFrame
{
    JButton bSyncOneWay;
    JButton bSyncTwoWay;
    JTextField tfSource;      //Quelle
    JTextField tfDestination; //Ziel
    JList listLog;
    DefaultListModel listModel;

    FileSync fs;
   
    public boolean autoSyncEnabled = false;

    // constructor for ButtonFrame
    MainFrame(String title, FileSync fs) 
    {
        super( title ); 
        this.fs = fs;
        
        // invoke the JFrame constructor
        setLayout( new FlowLayout() );      // set the layout manager

        bSyncOneWay = new JButton("Sync One Way"); 
        bSyncOneWay.addActionListener( new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    SyncOneWay(tfSource.getText(), tfDestination.getText());
                }
            });
        add(bSyncOneWay);

        bSyncTwoWay = new JButton("Sync Two Way");
        bSyncTwoWay.addActionListener( new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    SyncTwoWay(tfSource.getText(), tfDestination.getText());
                }
            });
        add(bSyncTwoWay);

        tfSource = new JTextField("/Users/imazze/Google Drive/50_Software/MacCloudSync/Testarea/Quelle/");
        add(tfSource);

        tfDestination = new JTextField("/Users/imazze/Google Drive/50_Software/MacCloudSync/Testarea/Ziel/");
        add(tfDestination);

        listModel = new DefaultListModel();
        listLog = new JList(listModel);

        JScrollPane sp = new JScrollPane(listLog);
        sp.setPreferredSize(new Dimension(200, 200));
        add(sp);

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); 
    }

    public void addTouchBar(){
        JTouchBar jTouchBar = new JTouchBar();
        jTouchBar.setCustomizationIdentifier("MySwingJavaTouchBar");

        // Customize your touchbar
        // label
        TouchBarTextField touchBarTextField = new TouchBarTextField();
        touchBarTextField.setStringValue("MacCloudSync ->");

        jTouchBar.addItem(new TouchBarItem("TextField_1", touchBarTextField, true));

        // fixed space
        jTouchBar.addItem(new TouchBarItem(TouchBarItem.NSTouchBarItemIdentifierFixedSpaceSmall));

        // Button 1
        TouchBarButton touchBarButtonImg = new TouchBarButton();
        touchBarButtonImg.setTitle("SyncOneWay");
        touchBarButtonImg.setAction(new TouchBarViewAction() {
                @Override
                public void onCall( TouchBarView view ) {
                    SyncOneWay(tfSource.getText(), tfDestination.getText());
                }
            });
        jTouchBar.addItem(new TouchBarItem("Button_1", touchBarButtonImg, true));

        // fixed space
        jTouchBar.addItem(new TouchBarItem(TouchBarItem.NSTouchBarItemIdentifierFixedSpaceSmall));

        // Button 2
        TouchBarButton touchBarButtonImg2 = new TouchBarButton();
        touchBarButtonImg2.setTitle("SyncTwoWay");
        touchBarButtonImg2.setAction(new TouchBarViewAction() {
                @Override
                public void onCall( TouchBarView view ) {
                    SyncTwoWay(tfSource.getText(), tfDestination.getText());
                }
            });
        jTouchBar.addItem(new TouchBarItem("Button_2", touchBarButtonImg2, true));


        jTouchBar.show(this);   
    }

    public void addLog(String logMsg){
        listModel.addElement("Log: " + logMsg);
    }

    private void SyncOneWay(String source, String destination){
        addLog("Sync One Way");
        fs.setSyncPath(source, destination);
        
        // Make a one way Sync
        fs.makeManualSync(false);
    }

    private void SyncTwoWay(String source, String destination){
        addLog("Sync Two Way");
        fs.setSyncPath(source, destination);
        
        // Make a two way Sync
        fs.makeManualSync(true);
    }
    
    public String getDestinationPath(){
        return tfDestination.getText();
    }
    
    public String getSourcePath(){
        return tfSource.getText();
    }
}
