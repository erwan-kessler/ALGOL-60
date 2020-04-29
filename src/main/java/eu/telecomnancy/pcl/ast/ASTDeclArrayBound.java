package eu.telecomnancy.pcl.ast;

public class ASTDeclArrayBound {
    private ASTNode boundInf;
    private ASTNode boundSup;

    public ASTDeclArrayBound(ASTNode boundInf, ASTNode boundSup) {
        this.boundInf = boundInf;
        this.boundSup = boundSup;
    }

    public ASTNode getBoundInf() {
        return boundInf;
    }

    public ASTNode getBoundSup() {
        return boundSup;
    }
}
