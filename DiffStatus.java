public enum DiffStatus {
        add('A'),           // add to target      - source exists, target does not exist
        addSource('S'),     // add to source      - target exists, source not
        modify('M'),        // modify target      - source and target exist but are different
        modifySource('H'),  // modify source      - source and target exist but are different
        rename('R'),        // rename target      - source and target exist and are equal, but file name upper/lower case characters differ
        renameSource('B'),  // rename source      - source and target exist and ...
        delete('D'),        // delete from target - source does not exist, target exists
        deleteSource('C');  // delete from target - source does not exist, target exists
        
        public final char code;
        DiffStatus(char code) {
            this.code = code;
        }
        
        @Override
        public String toString(){
            switch (code){
                case 'A': return "Add to Target";
                case 'S': return "Add to Source";
                case 'M': return "Modify Target";
                case 'H': return "Modify Source";
                case 'R': return "Rename Target";
                case 'B': return "Rename Source";
                case 'D': return "Delete from Target";
                case 'C': return "Delete from Source";
                default: return "N/A";
            }
        }
    }