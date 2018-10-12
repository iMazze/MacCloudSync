package Dev;

import javax.swing.*;

import com.thizzer.jtouchbar.*;
import com.thizzer.jtouchbar.item.*;
import com.thizzer.jtouchbar.item.view.*;
import com.thizzer.jtouchbar.item.view.action.*;
import com.thizzer.jtouchbar.common.*;
import com.thizzer.jtouchbar.common.Image.*;


public class TouchbarTest {

    public static void main(String[] args)
    {
        /* Erzeugung eines neuen Frames mit dem 
           Titel "Beispiel JFrame " */       
        JFrame meinFrame= new JFrame("Beispiel JFrame");
        
        JTouchBar jTouchBar = new JTouchBar();
        jTouchBar.setCustomizationIdentifier("MySwingJavaTouchBar");

        // Customize your touchbar

        TouchBarButton touchBarButtonImg = new TouchBarButton();
        touchBarButtonImg.setTitle("Hallo Johannes");
        touchBarButtonImg.setAction(new TouchBarViewAction() {
            @Override
            public void onCall( TouchBarView view ) {
		System.out.println("Hi Johannes!!!");
            }
        });
        jTouchBar.addItem(new TouchBarItem("Button_1", touchBarButtonImg, true));

        TouchBarButton touchBarButtonImg2 = new TouchBarButton();
        touchBarButtonImg2.setTitle("Hallo Jakob");
        touchBarButtonImg2.setAction(new TouchBarViewAction() {
            @Override
            public void onCall( TouchBarView view ) {
		System.out.println("Hi!!!");
            }
        });
        jTouchBar.addItem(new TouchBarItem("Button_2", touchBarButtonImg2, true));

        

        // label
        TouchBarTextField touchBarTextField = new TouchBarTextField();
        touchBarTextField.setStringValue("Created on Java Application");
        
        jTouchBar.addItem(new TouchBarItem("TextField_1", touchBarTextField, true));


        
        
         
        /* Wir setzen die Breite und die Höhe 
           unseres Fensters auf 200 Pixel */        
        meinFrame.setSize(200,200);
        // Wir lassen unseren Frame anzeigen
        meinFrame.setVisible(true);
        
        jTouchBar.show(meinFrame);
    }
}
