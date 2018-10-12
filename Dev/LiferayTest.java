package Dev;

import java.util.*; 
import java.util.List; 
import java.util.Arrays;
import com.liferay.nativity.*;
import com.liferay.nativity.control.*;
import com.liferay.nativity.modules.fileicon.*;
import com.liferay.nativity.modules.contextmenu.*;
import com.liferay.nativity.modules.contextmenu.model.*;
import com.liferay.nativity.util.*;

/**
 * Write a description of class LiferayTest here.
 *
 * @author (your name)
 * @version (a version number or a date)
 */
public class LiferayTest
{
    static int testIconId;
    
    public static void main(String[] args)
    {
        NativityControl nativityControl = NativityControlUtil.getNativityControl();

        nativityControl.connect();

        
        // Setting filter folders is required for Mac's Finder Sync plugin
        nativityControl.setFilterFolder("/Users/imazze/liferay-sync");

        /* File Icons */

        testIconId = 1;

        // FileIconControlCallback used by Windows and Mac
        FileIconControlCallback fileIconControlCallback = new FileIconControlCallback() {
                @Override
                public int getIconForFile(String path) {
                    return testIconId;
                }
            };

        FileIconControl fileIconControl = FileIconControlUtil.getFileIconControl(
                nativityControl, fileIconControlCallback);

        fileIconControl.enableFileIcons();

        String testFilePath = "/Users/imazze/liferay-sync/testFile.txt";

        if (OSDetector.isWindows()) {
            // This id is determined when building the DLL
            testIconId = 1;
        }
        else if (OSDetector.isMinimumAppleVersion(OSDetector.MAC_YOSEMITE_10_10)) {
            // Used by Mac Finder Sync. This unique id can be set at runtime.
            System.out.println("10.10");
            testIconId = 1;

            fileIconControl.registerIconWithId("/Users/imazze/test.icns", "test label22", testIconId+"");
             }
        else if (OSDetector.isLinux() || OSDetector.isMinimumAppleVersion("10.0")) {
            // Used by Mac Injector and Linux
            testIconId = fileIconControl.registerIcon("/Users/imazze/test.icns");
        }

        // FileIconControl.setFileIcon() method only used by Linux
        fileIconControl.setFileIcon(testFilePath, testIconId);

        
        /* Context Menus */

        ContextMenuControlCallback contextMenuControlCallback = new ContextMenuControlCallback() {
                @Override
                public List<ContextMenuItem> getContextMenuItems(String[] paths) {
                    ContextMenuItem contextMenuItem = new ContextMenuItem("NativityTest");

                    ContextMenuAction contextMenuAction = new ContextMenuAction() {
                            @Override
                            public void onSelection(String[] paths) {
                                for (String path : paths) {
                                    System.out.print(path + ", ");
                                }

                                System.out.println("selected");
                            }
                        };

                    contextMenuItem.setContextMenuAction(contextMenuAction);

                    List<ContextMenuItem> contextMenuItems = new ArrayList<ContextMenuItem>() {};
                    contextMenuItems.add(contextMenuItem);

                    // Mac Finder Sync will only show the parent level of context menus
                    return contextMenuItems;
                }
            };

        ContextMenuControlUtil.getContextMenuControl(nativityControl, contextMenuControlCallback);
        
        System.out.println("ends");
    }
}
