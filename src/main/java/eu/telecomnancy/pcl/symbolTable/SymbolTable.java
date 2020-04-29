package eu.telecomnancy.pcl.symbolTable;

import eu.telecomnancy.pcl.semantics.StaticSemanticException;
import eu.telecomnancy.pcl.symbolTable.Records.ArrayRecord;
import eu.telecomnancy.pcl.symbolTable.Records.LabelRecord;
import eu.telecomnancy.pcl.symbolTable.Records.ProcedureRecord;
import eu.telecomnancy.pcl.symbolTable.Records.VariableRecord;

import java.util.*;
import java.util.stream.Collectors;

import static eu.telecomnancy.pcl.Main.logger;

public class SymbolTable {

    boolean debug = false;
    private final Scope root; // the root scope
    private Scope current; // current scope

    public SymbolTable() {
        this.root = new Scope(null);
        this.current = root;
    }

    public List<Integer> getCurrentScopeName() {
        return this.current.getScopeName();
    }

    public String getCurrentScopeType() {
        return this.current.getScopeType();
    }

    public void setCurrentScopeNameAndType(List<Integer> scopeIndex, String scopeType) {
        this.current.setScopeNameAndType(scopeIndex, scopeType);
    }

    public String getFormattedName() {
        return getCurrentScopeName().subList(0, getCurrentScopeName().size() ).stream().map(Object::toString).collect(Collectors.joining("_"));
    }

    public Scope getCurrent() {
        return current;
    }

    public Scope getRoot() {
        return root;
    }

    // create a new scope if necessary
    public void enterScope() {
        current = current.nextChild();
    }

    public void exitScope() {
        current = current.getParent();
    }

    public void put(String key, Record item) {
        current.put(key, item);
    }

    public Object[] lookupCount(String key) {
        if (debug)
            System.out.println("\t LOOKUPCOUNT : Current SCOPE: " + current.getScopeName() + " | " + current.getScopeType());
        return current.lookupCount(key);
    }

    public Record lookup(String key) {
        if (debug)
            System.out.println("\t LOOKUP: Current SCOPE: " + current.getScopeName() + " | " + current.getScopeType());
        return current.lookup(key);
    }

    public Object[] lookupCountGenre(String key, Record.Genre genre) {
        if (debug)
            System.out.println("\t LOOKUP: Current SCOPE: " + current.getScopeName() + " | " + current.getScopeType());
        return current.lookupCountGenre(key, genre);
    }


    public Record lookup(String key, Record.Genre genre) {
        if (debug)
            System.out.println("\t LOOKUP: Current SCOPE: " + current.getScopeName() + " | " + current.getScopeType());
        return current.lookup(key, genre);
    }


    // diagnostics
    public void printTable() {
        System.out.println("\n\t\tPrinting the Symbol Table:\n");
        System.out.printf("%s %n", "+-------------------------------------------------------------------------------------------+");
        System.out.printf("%s %s %s %n", "ID", "RECORD", "SCOPE");
        System.out.printf("%s %n", "+-------------------------------------------------------------------------------------------+");
        root.printScope();
        System.out.printf("%s %n", "+-------------------------------------------------------------------------------------------+");
    }

    private void printLine(String id, String record, String scope) {
        System.out.printf("%s %s %s %n", id, record, scope);
    }

    // called before each traversal
    public void resetTable() {
        root.resetScope();
    }

    // iterate the scope tree and print everything
    public void printScopeTree() {
        System.out.println("\n\n");
        root.printScopeDebug();
    }


    public class Scope {
        // for visual identification
        List<Integer> scopeIndex;
        String scopeType = "";
        String procedureName;
        public int incString = 0;
        private int next = 0; // next child to visit
        private final Scope parent; // parent scope
        // children scopes
        private final ArrayList<Scope> children = new ArrayList<>();
        // symbol to record map
        private final Map<String, Record> records = new HashMap<>();

        public Scope(Scope parent) {
            this.parent = parent;
        }

        public void setScopeNameAndType(List<Integer> scopeIndex, String scopeType) {
            this.scopeIndex = List.copyOf(scopeIndex);
            this.scopeType = scopeType;
        }
        public String getFormattedName() {
            return scopeIndex.subList(0, scopeIndex.size() ).stream().map(Object::toString).collect(Collectors.joining("_"));
        }

        public List<Integer> getScopeName() {
            return this.scopeIndex;
        }

        public String getScopeType() {
            return scopeType;
        }

        public String getProcedureName() {
            return this.procedureName;
        }

        public void setProcedureName(String identifier) {
            this.procedureName = identifier;
        }

        public void printScope() {
            // print all the containing records
            for (Map.Entry<String, Record> stringRecordEntry : records.entrySet()) {
                Record temp = stringRecordEntry.getValue();
                printLine(stringRecordEntry.getKey(), temp.getId() + " - " + temp.getType(), scopeIndex.toString() + " index: " + scopeIndex.size() + " [ " + scopeType + " ]");
            }
            // print children
            for (Scope scopeIt : children) {
                scopeIt.printScope();
            }
        }

        public void printScopeDebug() {
            System.out.println("SCOPE: " + scopeIndex.toString() + " index: " + scopeIndex.size()+ " nameFormatted: "+this.getFormattedName());
            System.out.println("  RECORDS: ");
            for (Map.Entry<String, Record> stringRecordEntry : records.entrySet()) {
                Record temp = stringRecordEntry.getValue();
                if (temp instanceof ProcedureRecord) {
                    System.out.println("\t PROCEDURE RECORD");
                } else if (temp instanceof ArrayRecord) {
                    System.out.println("\t ARRAY RECORD");
                } else if (temp instanceof LabelRecord) {
                    System.out.println("\t LABEL RECORD");
                } else if (temp instanceof VariableRecord) {
                    System.out.println("\t VARIABLE RECORD");
                } else {
                    System.out.println("\t NOT IMPLEMENTED RECORD");
                }
                printLine(stringRecordEntry.getKey(), temp.getId() + " - " + temp.getType(), scopeIndex.toString() + " index: " + scopeIndex.size() + " [ " + scopeType + " ]");
            }
            System.out.println("-");
            for (Scope scopeIt : children) {
                System.out.println(scopeIndex.toString() + " index: " + scopeIndex.size() + " | " + scopeType + " -> CHILDREN: " + scopeIt.getScopeName() + " | " + scopeIt.getScopeType());
                scopeIt.printScopeDebug();
            }

        }

        // add a new record to the current scope
        public void put(String key, Record item) {
            if (records.containsKey(key)) {
                logger.log(new StaticSemanticException("TDS: Variable "+key+" is already defined"),item);
            }
            records.put(key, item);
        }

        public Scope nextChild() {
            Scope nextChild;
            if (next >= children.size()) {
                nextChild = new Scope(this); // create a new Scope passing the parent scope
                children.add(nextChild);
            } else {
                // child exists
                nextChild = children.get(next); // visited the next
                // child (Scope)
            }
            next++;
            return nextChild;
        }

        public Object[] lookupCount(String key) { //same as lookup but returns the amount of hops done
            if (records.containsKey(key)) { // is the key in current scope?
                Record rec = records.get(key);
                if (debug)
                    System.out.println("\tRecord found on: " + scopeIndex.toString() + " index: " + scopeIndex.size() + " [ " + scopeType + " ]");
                return new Object[]{rec, 0};
            } else {
                // move the scope to parent scope
                if (parent == null) {
                    return null; // identifier is not contained
                } else {
                    Object[] temp = parent.lookupCount(key);
                    if (temp == null) {
                        return null;
                    }
                    return new Object[]{temp[0], 1 + (int) temp[1]}; // send the req to parent
                }
            }
        }

        public Record lookup(String key) {
            if (records.containsKey(key)) { // is the key in current scope?
                Record rec = records.get(key);
                if (debug)
                    System.out.println("\tRecord found on: " + scopeIndex.toString() + " index: " + scopeIndex.size() + " [ " + scopeType + " ]");
                return rec;
            } else {
                // move the scope to parent scope
                if (parent == null) {
                    return null; // identifier is not contained
                } else {
                    return parent.lookup(key); // send the req to parent
                }
            }
        }

        public Object[] lookupCountGenre(String key, Record.Genre genre) {
            //same as lookup but returns the amount of hops done
            if (records.containsKey(key) && records.get(key).getGenre() == genre) { // is the key in current scope?
                Record rec = records.get(key);
                if (debug)
                    System.out.println("\tRecord found on: " + scopeIndex.toString() + " index: " + scopeIndex.size() + " [ " + scopeType + " ]");
                return new Object[]{rec, 0};
            } else {
                // move the scope to parent scope
                if (parent == null) {
                    return null; // identifier is not contained
                } else {
                    Object[] temp = parent.lookupCountGenre(key, genre);
                    if (temp == null) {
                        return null;
                    }
                    return new Object[]{temp[0], 1 + (int) temp[1]}; // send the req to parent
                }
            }
        }

        public Record lookup(String key, Record.Genre genre) {
            if (records.containsKey(key) && records.get(key).getGenre() == genre) { // is the key in current scope?
                Record rec = records.get(key);
                if (debug)
                    System.out.println("\tRecord found on: " + scopeIndex.toString() + " index: " + scopeIndex.size() + " [ " + scopeType + " ]");
                return rec;
            } else if (genre != Record.Genre.LABEL && genre != Record.Genre.SWITCH) {
                // move the scope to parent scope
                if (parent == null) {
                    return null; // identifier is not contained
                } else {
                    return parent.lookup(key, genre); // send the req to parent
                }
            }
            return null;
        }

        public int getDepth() {
            if (parent == null)
                return 0;
            else
                return 1 + parent.getDepth();
        }

        public void resetScope() {
            next = 0; // first child to visit next
            for (Scope child : children) {
                child.resetScope();
            }
        }

        public Scope getParent() {
            return this.parent;
        }

        public Set<Map.Entry<String, Record>> getRecords() {
            return records.entrySet();
        }

        public ArrayList<Scope> getChildren() {
            return children;
        }
    }
}
