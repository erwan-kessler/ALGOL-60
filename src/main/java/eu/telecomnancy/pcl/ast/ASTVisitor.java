package eu.telecomnancy.pcl.ast;

public interface ASTVisitor<T> {
    T visit(ASTAdd node);

    T visit(ASTAnd node);

    T visit(ASTAssign node);

    T visit(ASTBlock node);

    T visit(ASTDecl node);

    T visit(ASTDeclArray node);

    T visit(ASTDeclFcn node);

    T visit(ASTDeclSwitch node);

    T visit(ASTDiv node);

    T visit(ASTDummy node);

    T visit(ASTEq node);

    T visit(ASTFcn node);

    T visit(ASTFor node);

    T visit(ASTGe node);

    T visit(ASTGoto node);

    T visit(ASTGt node);

    T visit(ASTIdiv node);

    T visit(ASTIf node);

    T visit(ASTIfe node);

    T visit(ASTIff node);

    T visit(ASTImpl node);

    T visit(ASTLabel node);

    T visit(ASTLe node);

    T visit(ASTLogical node);

    T visit(ASTLt node);

    T visit(ASTMinus node);

    T visit(ASTMul node);

    T visit(ASTNeq node);

    T visit(ASTNot node);

    T visit(ASTNumber node);

    T visit(ASTOr node);

    T visit(ASTPlus node);

    T visit(ASTPow node);

    T visit(ASTRoot node);

    T visit(ASTString node);

    T visit(ASTSub node);

    T visit(ASTVar node);

    T visit(ASTVarSubscript node);
}
