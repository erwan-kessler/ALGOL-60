package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.SymbolTable;
import eu.telecomnancy.pcl.syntactic.SyntacticException;

import static eu.telecomnancy.pcl.Main.logger;

public class ASTLabel extends ASTNode {
    private String identifier;

    public ASTLabel(int sourceLine, int sourceChar, String identifier) {
        super(sourceLine, sourceChar);

        try {
            if (identifier.matches("[0-9](.*)")) {
                identifier = String.valueOf(Integer.parseInt(identifier));
            }
        } catch (NumberFormatException e) {
            /* Attention ça pique : on catch l'exception de conversion
             * pour renvoyer une autre exception (qui est syntaxique).
             */
            logger.logCritical(new SyntacticException("Malformed label"), this);

        }
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public ASTType getExprType(SymbolTable currentSymbolTable) {
        return ASTType.LABEL;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
