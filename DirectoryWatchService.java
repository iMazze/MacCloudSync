 
import java.util.*;
import java.io.*;
 
import static java.nio.file.StandardWatchEventKinds.*;
 
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
 
public abstract class DirectoryWatchService extends TimerTask{
 
    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
 
    /**
     * Creates a WatchService and registers the given directory
     */
    DirectoryWatchService(Path dir) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
 
        walkAndRegisterDirectories(dir);
    }
 
    /**
     * Register the given directory with the WatchService; This function will be called by FileVisitor
     */
    private void registerDirectory(Path dir) throws IOException
    {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
    }
 
    /**
     * Register the given directory, and all its sub-directories, with the WatchService.
     */
    private void walkAndRegisterDirectories(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerDirectory(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * Process all events for keys queued to the watcher
     */
    public final void run() {

        // wait for key to be signalled
        WatchKey key;
        try {
            key = watcher.take();
        } catch (InterruptedException x) {
            return;
        }

        Path dir = keys.get(key);
        if (dir == null) {
            System.err.println("WatchKey not recognized!!");
            return;
        }

        for (WatchEvent<?> event : key.pollEvents()) {
            @SuppressWarnings("rawtypes")
            WatchEvent.Kind kind = event.kind();

            // Context for directory entry event is the file name of entry
            @SuppressWarnings("unchecked")
            Path name = ((WatchEvent<Path>)event).context();
            Path child = dir.resolve(name);

            // event on change
            onChange(child, event.kind());

            // if directory is created, and watching recursively, then register it and its sub-directories
            if (kind == ENTRY_CREATE) {
                try {
                    if (Files.isDirectory(child)) {
                        walkAndRegisterDirectories(child);
                    }
                } catch (IOException x) {
                    // do something useful
                }
            }
        }

        // reset key and remove from set if directory no longer accessible
        boolean valid = key.reset();
        if (!valid) {
            keys.remove(key);

            // all directories are inaccessible
            if (keys.isEmpty()) {
                return;
            }
        }
    }
    
    protected abstract void onChange(Path file, WatchEvent.Kind kind);
 
    public static void main(String[] args) throws IOException {
        Path dir = Paths.get("/Users/imazze/Google Drive/50_Software/MacCloudSync/Testarea/Ziel/");
        
        TimerTask ds = new DirectoryWatchService(dir){
            protected void onChange( Path file, WatchEvent.Kind kind ) {
                System.out.println(kind + ": " + file);
            }
        };
        
        java.util.Timer timer = new java.util.Timer();
        timer.schedule( ds , new Date(), 1000 );
    }
}