package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import java.util.List;

public class ASTDeclFcn extends ASTNode {
    private String identifier;
    private ASTType type;
    private List<String> parameters;
    private List<String> values;
    private List<ASTDeclFcnSpec> specs;
    private List<ASTNode> declarations;
    private ASTNode statement;
    private boolean isRecursive;

    public ASTDeclFcn(int sourceLine, int sourceChar, String identifier,
                      ASTType type, List<String> parameters,
                      List<String> values, List<ASTDeclFcnSpec> specs,
                      ASTNode statement) {
        super(sourceLine, sourceChar);

        this.identifier = identifier;
        this.type = type;
        this.parameters = parameters;
        this.values = values;
        this.specs = specs;
//        this.declarations = declarations;
        this.statement = statement;
        this.isRecursive = false;
    }

    public String getIdentifier() {
        return identifier;
    }

    public ASTType getType() {
        return type;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public List<String> getValues() {
        return values;
    }

    public List<ASTDeclFcnSpec> getSpecs() {
        return specs;
    }

//    public List<ASTNode> getDeclarations() {
//        return declarations;
//    }

    public ASTNode getStatement() {
        return statement;
    }

    public void setRecursive() {
        isRecursive = true;
    }

    public boolean isRecursive() {
        return isRecursive;
    }

    @Override
    public ASTType getExprType(SymbolTable currentSymbolTable) {
        return ASTType.NONE;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
