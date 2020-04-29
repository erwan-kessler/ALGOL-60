package eu.telecomnancy.pcl.symbolTable.Records;

import eu.telecomnancy.pcl.ast.ASTType;
import eu.telecomnancy.pcl.symbolTable.Record;

import java.util.HashMap;
import java.util.Map;

public class VariableRecord extends Record {

    private HashMap<String, String> fields = new HashMap<>();
    private int fieldNumber = 0;
    private boolean own;

    public VariableRecord(String id, ASTType type, Boolean own) {
        super(id, type, Genre.VARIABLE);
        this.own = own;
    }

    public boolean getOwn() {
        return own;
    }

    public void addField(String type, String value) {
        this.fields.put(type, value);
        fieldNumber++;
    }

    public String getParameter(String type) {
        String paramRec = fields.get(type);
        if (type != null && paramRec != null) {
            return fields.get(type);
        }
        return null;
    }

    public void printFields() {
        System.out.print("( ");
        for (Map.Entry<String, String> field : fields.entrySet()) {
            System.out.print("KEY: " + field.getKey() + " VALUE: " + field.getValue() + ", ");
        }
        System.out.print(" )\n");
    }

}
