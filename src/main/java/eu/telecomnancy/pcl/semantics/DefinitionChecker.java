package eu.telecomnancy.pcl.semantics;

import eu.telecomnancy.pcl.ast.*;
import eu.telecomnancy.pcl.symbolTable.Record;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;

import static eu.telecomnancy.pcl.Main.logger;

public class DefinitionChecker {


    public DefinitionChecker() {
    }

    public void checkIfDefine(SymbolTable currentST, ASTVar var) {
        if (currentST.lookup(var.getIdentifier()) == null) {
            logger.log(new StaticSemanticException("Variable not defined"), var);
        }
    }

    public void checkLabel(SymbolTable symbolTable, ASTNode node) {
        // Label de type nombre
        if (node instanceof ASTNumber) {
            ASTNumber labelNb = (ASTNumber) node;
            Record r = symbolTable.lookup(String.valueOf((int)labelNb.getValue()), Record.Genre.LABEL);
            if (r == null) {
                logger.log(new StaticSemanticException("Label is not declared"), labelNb);
            }
        } // Label de type "variable"
        else if (node instanceof ASTVar) {
            ASTVar labelVar = (ASTVar) node;
            Record r = symbolTable.lookup(labelVar.getIdentifier(), Record.Genre.LABEL);
            if (r == null) {
                logger.log(new StaticSemanticException("Label \"" + labelVar.getIdentifier() + "\" is not declared"), labelVar);
            }
        } // Label retourné par un switch
        else if (node instanceof ASTVarSubscript) {
            ASTVarSubscript labelVarSub = (ASTVarSubscript) node;
            Record r = symbolTable.lookup(labelVarSub.getIdentifier(), Record.Genre.SWITCH);
            if (r == null) {
                logger.log(new StaticSemanticException("Switch \"" + labelVarSub.getIdentifier() + "\" is not declared"), labelVarSub);
            } else if (labelVarSub.getExpressions().size() != 1) {
                logger.log(new StaticSemanticException("Switch \"" + labelVarSub.getIdentifier() + "\" takes exactly one argument"), labelVarSub);
            }
        } else if (node.getExprType(symbolTable) != ASTType.LABEL)
            logger.log(new StaticSemanticException("Label value error"), node);
    }
}
