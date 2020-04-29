package eu.telecomnancy.pcl.debug;

import eu.telecomnancy.pcl.ast.*;

public class ASTDebuggerVisitor implements ASTVisitor<ASTDebuggerVisitor> {
    private int tabs;

    public ASTDebuggerVisitor() {
        tabs = 0;
    }

    private void debug(String message) {
        for (int i = 0; i < tabs; ++i)
            System.out.print("  ");
        System.out.println(message);
    }

    @Override
    public ASTDebuggerVisitor visit(ASTAdd node) {
        debug("ADD");
        tabs++;
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTAnd node) {
        debug("AND");
        tabs++;
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTAssign node) {
        debug("ASSIGN");
        tabs++;
        for (ASTNode nodeChild : node.getDesignators())
            nodeChild.accept(this);
        node.getExpression().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTBlock node) {
        debug("BLOCK");
        tabs++;
        for (ASTNode nodeChild : node.getDeclarations())
            nodeChild.accept(this);
        for (ASTNode nodeChild : node.getStatements())
            nodeChild.accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTDecl node) {
        debug("DECL (" + (node.isOwned() ? "OWN " : "")
                + node.getType() + " "
                + node.getIdentifier()
                + ")");
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTDeclArray node) {
        debug("DECL_ARRAY (" + (node.isOwned() ? "OWN " : "")
                + node.getType() + " "
                + node.getIdentifier()
                + ")");
        tabs++;
        for (ASTDeclArrayBound bound : node.getBounds()) {
            debug("BOUND_PAIR");
            tabs++;
            bound.getBoundInf().accept(this);
            bound.getBoundSup().accept(this);
            tabs--;
        }
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTDeclFcn node) {
        debug("DECL_FCN (" + node.getIdentifier() + ")");
        tabs++;
        for (String parameter : node.getParameters())
            debug("PARAM (" + parameter + ")");
        for (String value : node.getValues())
            debug("VALUE (" + value + ")");
        for (ASTDeclFcnSpec spec : node.getSpecs())
            debug("SPEC (" + spec.getType() + " " + spec.getIdentifier() + ")");
        node.getStatement().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTDeclSwitch node) {
        debug("DECL_SWITCH (" + node.getIdentifier() + ")");
        tabs++;
        for (ASTNode nodeChild : node.getExpressions())
            nodeChild.accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTDiv node) {
        debug("DIV");
        tabs++;
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTDummy node) {
        debug("DUMMY");
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTEq node) {
        debug("EQ");
        tabs++;
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTFcn node) {
        debug("FCN (" + node.getIdentifier() + ")");
        tabs++;
        for (ASTNode nodeChild : node.getParameters())
            nodeChild.accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTFor node) {
        debug("FOR");
        tabs++;
        for (ASTForIterator iterator : node.getIterators()) {
            if (iterator instanceof ASTForIteratorExpr) {
                debug("ITERATOR_EXPR");
                tabs++;
                iterator.getExpression().accept(this);
                tabs--;
            } else if (iterator instanceof ASTForIteratorWhile) {
                debug("ITERATOR_WHILE");
                tabs++;
                iterator.getExpression().accept(this);
                ((ASTForIteratorWhile) iterator).getCondition().accept(this);
                tabs--;
            } else if (iterator instanceof ASTForIteratorStepUntil) {
                debug("ITERATOR_STEPUNTIL");
                tabs++;
                iterator.getExpression().accept(this);
                ((ASTForIteratorStepUntil) iterator).getStepExpression()
                        .accept(this);
                ((ASTForIteratorStepUntil) iterator).getUntilExpression()
                        .accept(this);
                tabs--;
            }
        }
        node.getStatement().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTGe node) {
        debug("GE");
        tabs++;
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTGoto node) {
        debug("GOTO");
        tabs++;
        node.getExpression().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTGt node) {
        debug("GT");
        tabs++;
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTIdiv node) {
        debug("IDIV");
        tabs++;
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTIf node) {
        debug("IF");
        tabs++;
        node.getCondition().accept(this);
        node.getThenStatement().accept(this);
        node.getElseStatement().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTIfe node) {
        debug("IFE");
        tabs++;
        node.getCondition().accept(this);
        node.getThenExpression().accept(this);
        node.getElseExpression().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTIff node) {
        debug("IFF");
        tabs++;
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTImpl node) {
        debug("IMPL");
        tabs++;
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTLabel node) {
        debug("LABEL (" + node.getIdentifier() + ")");
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTLe node) {
        debug("LE");
        tabs++;
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTLogical node) {
        debug("LOGICAL (" + node.getValue() + ")");
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTLt node) {
        debug("LT");
        tabs++;
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTMinus node) {
        debug("MINUS");
        tabs++;
        node.getNode().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTMul node) {
        debug("MUL");
        tabs++;
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTNeq node) {
        debug("NEQ");
        tabs++;
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTNot node) {
        debug("NOT");
        tabs++;
        node.getNode().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTNumber node) {
        debug("NUMBER (" + node.getValue() + ")");
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTOr node) {
        debug("OR");
        tabs++;
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTPlus node) {
        debug("PLUS");
        tabs++;
        node.getNode().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTPow node) {
        debug("POW");
        tabs++;
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTRoot node) {
        debug("ROOT");
        tabs++;
        for (ASTNode nodeChild : node.getNodes())
            nodeChild.accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTString node) {
        debug("STRING (" + node.getValue() + ")");
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTSub node) {
        debug("SUB");
        tabs++;
        node.getNodeLeft().accept(this);
        node.getNodeRight().accept(this);
        tabs--;
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTVar node) {
        debug("VAR (" + node.getIdentifier() + ")");
        return this;
    }

    @Override
    public ASTDebuggerVisitor visit(ASTVarSubscript node) {
        debug("VAR_SUBSCRIPT (" + node.getIdentifier() + ")");
        tabs++;
        for (ASTNode nodeChild : node.getExpressions())
            nodeChild.accept(this);
        tabs--;
        return this;
    }
}
