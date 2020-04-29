package eu.telecomnancy.pcl.ast;

public class ASTDeclFcnSpec {
    private String identifier;
    private ASTDeclFcnSpecType type;
    private boolean value;

    public ASTDeclFcnSpec(String identifier, ASTDeclFcnSpecType type, boolean value) {
        this.identifier = identifier;
        this.type = type;
        this.value = value;
    }

    public String getIdentifier() {
        return identifier;
    }

    public ASTDeclFcnSpecType getType() {
        return type;
    }

    public boolean isValue() {
        return this.value;
    }
}
