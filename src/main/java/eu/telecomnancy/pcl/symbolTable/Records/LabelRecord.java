package eu.telecomnancy.pcl.symbolTable.Records;

import eu.telecomnancy.pcl.ast.ASTType;
import eu.telecomnancy.pcl.symbolTable.Record;


public class LabelRecord extends Record {

    public LabelRecord(String id, ASTType type) {
        super(id, type, Genre.LABEL);
    }

}
