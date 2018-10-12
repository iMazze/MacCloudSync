

public class UserInterface implements FileSyncProcessor.UserInterface {
        boolean tickerStarted;
        public void writeInfo(String s) {
            System.out.println(s);
        }

        public void writeInfoTicker() {
            System.out.print('.');
            tickerStarted = true;
        }

        public void endInfoTicker() {
            if (!tickerStarted) return;
            System.out.println();
            tickerStarted = false;
        }

        public void writeDebug(String s) {
            System.out.println(s);
        }

        public void listItem(String relativePath, DiffStatus diffStatus) {
            System.out.println(diffStatus.code + "  " + relativePath);
        }
    }