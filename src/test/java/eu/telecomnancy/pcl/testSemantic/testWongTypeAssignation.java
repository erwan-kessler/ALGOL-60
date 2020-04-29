package eu.telecomnancy.pcl.testSemantic;


import eu.telecomnancy.pcl.antlr.ANTLRParser;
import eu.telecomnancy.pcl.antlr.ANTLRTreeException;
import eu.telecomnancy.pcl.ast.ASTRoot;
import eu.telecomnancy.pcl.debug.Logger;
import eu.telecomnancy.pcl.optimization.ASTConstantFolder;
import eu.telecomnancy.pcl.semantics.SemanticChecker;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;
import eu.telecomnancy.pcl.symbolTable.TableCreation;
import eu.telecomnancy.pcl.syntactic.SyntacticChecker;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;

import static eu.telecomnancy.pcl.Main.logger;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class testWongTypeAssignation {
    private ASTRoot ast = null;

    @BeforeEach
    void init(TestInfo info) {
        InputStream is = getClass().getResourceAsStream(info.getTags().iterator().next());
        ANTLRParser parser = new ANTLRParser();
        try {
            parser.readSource(is);
            ast = parser.buildAST();
        } catch (IOException | ANTLRTreeException | RuntimeException e) {
            logger.logCritical(e);
            logger.getLogsCritical();
        }
        if (ast == null) {
            logger.logCritical("AST was not initialized");
            logger.getLogsCritical();
        }
        assert ast != null;
        ASTConstantFolder astConstantFolder = new ASTConstantFolder();
        astConstantFolder.visit(ast);
        ast = astConstantFolder.getOptimizedAST();

        SymbolTable TDS = (new TableCreation()).create(ast);

        SyntacticChecker syntacticChecker = new SyntacticChecker();
        syntacticChecker.visit(ast);

        SemanticChecker semanticChecker = new SemanticChecker(TDS);
        semanticChecker.visit(ast);

    }

    @AfterEach
    public void removeLogs() {
        logger.cleanLogs();
    }

    @Test
    @DisplayName("Assignations of unmatching types")
    @Tag("typeMissmatch.a60")
    public void parsingTypeMismatch() {

        assertEquals("ERROR: SyntacticException : Invalid assignment at line 7, character 9\n" +
                        "ERROR: StaticSemanticException : Type missmatch in assignment: integer expected at line 8, character 4\n" +
                        "ERROR: StaticSemanticException : Subscript is missing for \"A\" at line 9, character 9\n" +
                        "ERROR: StaticSemanticException : Type missmatch in assignment: Boolean expected at line 10, character 4\n" +
                        "ERROR: StaticSemanticException : Number expected in subscript at line 11, character 25\n" +
                        "ERROR: StaticSemanticException : Type missmatch in assignment: integer expected at line 11, character 4\n",
                logger.getInternalLogs());

    }

}
