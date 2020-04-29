package eu.telecomnancy.pcl.symbolTable.Records;

import eu.telecomnancy.pcl.ast.ASTNode;
import eu.telecomnancy.pcl.ast.ASTType;
import eu.telecomnancy.pcl.symbolTable.Record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwitchRecord extends Record {
    private ArrayList<ASTNode> switchList;

    public SwitchRecord(String id, ASTType type) {
        super(id, type, Genre.SWITCH);
        switchList = new ArrayList<>();
    }

    public void printList() {
        System.out.print("LIST( ");
        for (ASTNode spec : switchList) {
            System.out.print("NAME: " + spec.getName() + ", ");
        }
        System.out.print(" )\n");
    }


    public void addSpec(ASTNode specs) {
        this.switchList.add(specs);
    }

    public void addSpecs(List<ASTNode> specs) {
        this.switchList.addAll(specs);
    }

    public ArrayList<ASTNode> getSwitchList() {
        return switchList;
    }
}
