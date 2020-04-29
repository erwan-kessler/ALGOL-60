package eu.telecomnancy.pcl.syntactic;

import eu.telecomnancy.pcl.ast.*;

import static eu.telecomnancy.pcl.Main.logger;

public class SyntacticChecker implements ASTVisitor<SyntacticChecker> {
    private boolean validAssign;

    public SyntacticChecker visit(ASTAdd node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTAnd node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTAssign node) {
        for (ASTNode nodeChild : node.getDesignators()) {
            validAssign = false;
            nodeChild.accept(this);
            if (!validAssign) {
                logger.log(new SyntacticException("Invalid assignment"),
                        nodeChild);
            }
        }
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTBlock node) {
        for (ASTNode nodeChild : node.getStatements())
            nodeChild.accept(this);
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTDecl node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTDeclArray node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTDeclFcn node) {
        node.getStatement().accept(this);
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTDeclSwitch node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTDiv node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTDummy node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTEq node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTFcn node) {
        // procedure identifier only
        if (node.getParameters().size() == 0)
            validAssign = true;
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTFor node) {
        node.getStatement().accept(this);
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTGe node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTGoto node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTGt node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTIdiv node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTIf node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTIfe node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTIff node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTImpl node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTLabel node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTLe node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTLogical node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTLt node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTMinus node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTMul node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTNeq node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTNot node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTNumber node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTOr node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTPlus node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTPow node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTRoot node) {
        for (ASTNode nodeChild : node.getNodes())
            nodeChild.accept(this);
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTString node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTSub node) {
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTVar node) {
        validAssign = true;
        return this;
    }

    @Override
    public SyntacticChecker visit(ASTVarSubscript node) {
        validAssign = true;
        return this;
    }
}
