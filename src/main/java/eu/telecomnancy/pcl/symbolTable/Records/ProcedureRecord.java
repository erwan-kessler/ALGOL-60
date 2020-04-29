package eu.telecomnancy.pcl.symbolTable.Records;

import eu.telecomnancy.pcl.ast.ASTDeclFcnSpec;
import eu.telecomnancy.pcl.ast.ASTType;
import eu.telecomnancy.pcl.symbolTable.Record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcedureRecord extends Record {
    private ArrayList<String> parameters;
    private ArrayList<String> values;
    private ArrayList<ASTDeclFcnSpec> specifications;
    private boolean unknown;
    /* unknown is set to true if the procedure is a parameter of another procedure,
     * so we can't know any property of this function as it will be treated dynamically */

    public ProcedureRecord(String id, ASTType type) {
        super(id, type, Genre.PROCEDURE);
        parameters = new ArrayList<>();
        values = new ArrayList<>();
        specifications = new ArrayList<>();
        unknown = false;
    }

    public void addParameter(String param) {
        this.parameters.add(param);
    }

    public void addParameters(List<String> params) {
        this.parameters.addAll(params);
    }

    public void addValue(String value) {
        this.values.add(value);
    }

    public void addValues(List<String> values) {
        this.values.addAll(values);
    }

    public void addSpec(ASTDeclFcnSpec specs) {
        this.specifications.add(specs);
    }

    public void addSpecs(List<ASTDeclFcnSpec> specs) {
        this.specifications.addAll(specs);
    }

    public void setUnknown(boolean unknown) {
        this.unknown = unknown;
    }

    public ArrayList<String> getParameters() {
        return parameters;
    }

    public ArrayList<String> getValues() {
        return values;
    }

    public ArrayList<ASTDeclFcnSpec> getSpecifications() {
        return specifications;
    }

    public boolean isUnknown() {
        return unknown;
    }
}
