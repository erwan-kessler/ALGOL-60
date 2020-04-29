package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.Record;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import java.util.List;

public class ASTFcn extends ASTNode {
    private String identifier;
    private List<ASTNode> parameters;

    public ASTFcn(int sourceLine, int sourceChar, String identifier,
                  List<ASTNode> parameters) {
        super(sourceLine, sourceChar);

        this.identifier = identifier;
        this.parameters = parameters;
    }

    public String getIdentifier() {
        return identifier;
    }

    public List<ASTNode> getParameters() {
        return parameters;
    }

    @Override
    public ASTType getExprType(SymbolTable currentSymbolTable) {

        Record record = currentSymbolTable.lookup(this.identifier);

        if (record == null) {
            // Ideally another function in this class would handle that aprt
            return ASTType.ERROR;
        }
        return record.getType();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
