package eu.telecomnancy.pcl.testTDS;

import eu.telecomnancy.pcl.antlr.ANTLRParser;
import eu.telecomnancy.pcl.antlr.ANTLRTreeException;
import eu.telecomnancy.pcl.ast.ASTRoot;
import eu.telecomnancy.pcl.debug.Logger;
import eu.telecomnancy.pcl.optimization.ASTConstantFolder;
import eu.telecomnancy.pcl.symbolTable.TableCreation;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static eu.telecomnancy.pcl.Main.logger;

public class testDoubleDeclaration {
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
    }

    @AfterEach
    public void removeLogs() {
        logger.cleanLogs();
    }

    @Test
    @DisplayName("Double declaration on same scope")
    @Tag("doubleDecl.a60")
    public void parsingDoubleDeclSameScope() {
        (new TableCreation()).create(ast);
        assertEquals("ERROR: StaticSemanticException : TDS: Variable a is already defined at line 3, character 4\n"
                , logger.getInternalLogs());
    }

    @Test
    @DisplayName("Double or more declaration on different scope")
    @Tag("multipleDecl.a60")
    public void parsingDoubleDeclMultipleScope() {
        (new TableCreation()).create(ast);
        assertEquals("ERROR: StaticSemanticException : TDS: Variable a is already defined at line 3, character 4\n" +
                        "ERROR: StaticSemanticException : TDS: Variable a is already defined at line 7, character 8\n" +
                        "ERROR: StaticSemanticException : TDS: Variable b is already defined at line 11, character 12\n"
                , logger.getInternalLogs());
    }

    @Test
    @DisplayName("Double or more declaration on different scope for different types")
    @Tag("multipleTypeDecl.a60")
    public void parsingDoubleDeclMultipleScopeDifferentTypes() {
        (new TableCreation()).create(ast);
        assertEquals("ERROR: StaticSemanticException : TDS: Variable a is already defined at line 3, character 4\n" +
                        "ERROR: StaticSemanticException : TDS: Variable a is already defined at line 4, character 4\n" +
                        "ERROR: StaticSemanticException : TDS: Variable a is already defined at line 5, character 4\n" +
                        "ERROR: StaticSemanticException : TDS: Variable a is already defined at line 6, character 4\n" +
                        "ERROR: StaticSemanticException : TDS: Variable a is already defined at line 7, character 4\n" +
                        "ERROR: StaticSemanticException : TDS: Variable a is already defined at line 8, character 14\n" +
                        "ERROR: StaticSemanticException : TDS: Variable a is already defined at line 9, character 11\n" +
                        "ERROR: StaticSemanticException : TDS: Variable a is already defined at line 10, character 4\n" +
                        "ERROR: StaticSemanticException : TDS: Variable a is already defined at line 13, character 8\n" +
                        "ERROR: StaticSemanticException : TDS: Variable a is already defined at line 14, character 8\n" +
                        "ERROR: StaticSemanticException : TDS: Variable a is already defined at line 15, character 8\n" +
                        "ERROR: StaticSemanticException : TDS: Variable a is already defined at line 16, character 8\n" +
                        "ERROR: StaticSemanticException : TDS: Variable a is already defined at line 17, character 8\n" +
                        "ERROR: StaticSemanticException : TDS: Variable a is already defined at line 18, character 18\n" +
                        "ERROR: StaticSemanticException : TDS: Variable a is already defined at line 19, character 15\n" +
                        "ERROR: StaticSemanticException : TDS: Variable a is already defined at line 20, character 8\n"
                , logger.getInternalLogs());
    }
}
