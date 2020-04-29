package eu.telecomnancy.pcl;

import eu.telecomnancy.pcl.antlr.ANTLRParser;
import eu.telecomnancy.pcl.antlr.ANTLRTreeException;
import eu.telecomnancy.pcl.assembly.AsmCompilerVisitor;
import eu.telecomnancy.pcl.assembly.AsmRecursiveVisitor;
import eu.telecomnancy.pcl.assembly.register.GraphColoringRegister;
import eu.telecomnancy.pcl.assembly.register.RegisterAllocator;
import eu.telecomnancy.pcl.ast.ASTRoot;
import eu.telecomnancy.pcl.debug.ASTDebuggerVisitor;
import eu.telecomnancy.pcl.debug.DisplayGraph;
import eu.telecomnancy.pcl.debug.Logger;
import eu.telecomnancy.pcl.optimization.ASTConstantFolder;
import eu.telecomnancy.pcl.semantics.SemanticChecker;
import eu.telecomnancy.pcl.symbolTable.SymbolTable;
import eu.telecomnancy.pcl.symbolTable.TableCreation;
import eu.telecomnancy.pcl.syntactic.SyntacticChecker;
import projetIUP.Lanceur;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import static eu.telecomnancy.pcl.debug.DisplayTable.createAndShowGUI;

public class Main {
    public static RegisterAllocator registerAllocator = null;
    public static Logger logger = new Logger(true);
    private boolean launch = false;
    private boolean debugMain = false;
    private boolean silent = false;
    private boolean contantFolding = true;
    private boolean outputInPlace = false;
    private boolean isRessouce = false;
    public static boolean debugCompiler = false;
    private boolean compile = true;
    private boolean registerOptimization = false;
    public static boolean debugAll = false;

    public static void main(String[] args) throws FileNotFoundException {
        Set<String> arguments = new HashSet<>(Arrays.asList(args));
        (new Main()).parseArgs(arguments);
    }

    public void usage() {
        System.out.println("Usage is java -jar <filename>.jar <file> (options)*");
        System.out.println("<file> can either be a .al or .a60 file then it will be compiled to assembly into a .src and into binary into a .iup");
        System.out.println("or be .iup or .piup file that will be run only (need --run option)");
        System.out.println("    --help or -help show this help");
        System.out.println("    --run allow to only run a .iup or .piup file, will not compile anything");
        System.out.println("    --debug-all output all debug with no distinction");
        System.out.println("    --debug output only simple debug as usual");
        System.out.println("    --opti activate the register optimization");
        System.out.println("    --no-folding deactivate constant folding");
        System.out.println("    --resource use only a resource program as input");
        System.out.println("    --silent silent the output of .iup, only use when debug_all to silence piupk");
        System.out.println("    --in-place compile in place the file, do not create the outputCode folder");
        System.out.println("    --no-compile do not compile assembly to binary");
        System.out.println("    --quick run quickly, enable silent output, compile file and run it");
        System.out.println("    --launch launch afterwards the compiled binary file, incomptatible with --no-compile");
        System.out.println("    --table show table");
        System.out.println("    --graph show graph");
        System.out.println("    --debug-compiler debug only the compiler");
    }

    public void parseArgs(Set<String> arguments) throws FileNotFoundException {
        // make sure we get correct files
        Set<String> files = new HashSet<>(arguments);
        files.removeIf(str -> str.startsWith("--"));
        if (arguments.contains("-help") || arguments.contains("--help")) {
            // show usage
            usage();
            return;
        }
        if (files.size() > 1) {
            logger.logCritical("You should not provide more than 1 file parameters, currently provided are: " + files.toString());
            usage();
            logger.getLogsCritical();
            return;
        }
        if (arguments.contains("--run")) {
            Predicate<String> isIUP = str -> str.endsWith(".iup");
            Predicate<String> isPIUP = str -> str.endsWith(".piup");
            String filename = files.stream().filter(isIUP.or(isPIUP)).findFirst().orElse(null);
            if (filename == null) {
                logger.logCritical("No matching .iup or .piup file found");
                logger.getLogsCritical();
            }
            Lanceur.main(new String[]{"-batch", filename});
            return;
        }

        Predicate<String> isA60 = str -> str.endsWith(".a60");
        Predicate<String> isAL = str -> str.endsWith(".al");
        String filename = files.stream().filter(isA60.or(isAL)).findFirst().orElse(null);
        if (filename == null) {
            logger.logCritical("No matching .a60 or .al file found");
            logger.getLogsCritical();
        }

        if (arguments.contains("--debug-all")) {
            // activate all debug
            debugAll = true;
            debugMain = true;
        }
        if (arguments.contains("--debug")) {
            // activate light debug
            debugMain = true;
        }
        if (arguments.contains("--opti")) {
            // activate register optimization
            registerOptimization = true;
        }
        if (arguments.contains("--no-folding")) {
            // desactivate folding
            contantFolding = false;

        }
        if (arguments.contains("--resource")) {
            isRessouce = true;
        }

        if (arguments.contains("--silent")) {
            // silence output, only show relevant information
            silent = true;
        }

        if (arguments.contains("--in-place")) {
            outputInPlace = true;
        }

        if (arguments.contains("--no-compile")) {
            compile = false;
        }

        if (arguments.contains("--debug-compiler")) {
            debugCompiler = true;
        }

        if (arguments.contains("--launch")) {
            // run afterwards
            launch = true;
            compile = true;
        }

        if (arguments.contains("--quick")) {
            // run quickly
            try {
                runQuick(filename);
            } catch (IOException | ANTLRTreeException e) {
                logger.logCritical(e);
                logger.getLogsCritical();
            }
            return;
        }
        if (arguments.contains("--graph")) {
            // display register graph
            try {
                runGraph(filename);
            } catch (IOException | ANTLRTreeException e) {
                logger.logCritical(e);
                logger.getLogs();
            }
            return;
        }
        if (arguments.contains("--table")) {
            // display symbol table
            runTable(filename, isRessouce);
            return;
        }
        try {
            assert filename != null;
            run(filename);
        } catch (IOException e) {
            logger.logCritical(e);
            logger.getLogs();
        }
    }

    public void runGraph(String filename) throws IOException, ANTLRTreeException {
        ANTLRParser parser = new ANTLRParser();
        parser.readSource(isRessouce ? Main.class.getClassLoader().getResourceAsStream(filename) : new FileInputStream(filename));
        ASTRoot ast = parser.buildAST();
        assert ast != null;
        ast = (new ASTConstantFolder()).visit(ast).getOptimizedAST();
        SymbolTable TDS = (new TableCreation()).create(ast);
        (new SyntacticChecker()).visit(ast);
        (new SemanticChecker(TDS)).visit(ast);
        GraphColoringRegister graphColoringRegister = new GraphColoringRegister(ast, TDS);
        new DisplayGraph(graphColoringRegister.getListEdgeInterf(), graphColoringRegister.getListEdgePref(), graphColoringRegister.getListVertex());
    }

    public void runTable(String filename, boolean shouldPrompt) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                createAndShowGUI(filename, shouldPrompt);
            } catch (ANTLRTreeException | IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void run(String input) throws FileNotFoundException {
        ANTLRParser parser = new ANTLRParser(); // create new parser
        ASTRoot ast = null;
        String filename = input.split("\\.")[0];
        InputStream is = isRessouce ? Main.class.getClassLoader().getResourceAsStream(input) : new FileInputStream(input);

        try {
            parser.readSource(is);  // read the supplied file and check if it respect the grammar
            ast = parser.buildAST(); //build the ast from the grammar parsed
        } catch (IOException | ANTLRTreeException | RuntimeException e) {
            logger.logCritical(e);
            logger.getLogsCritical();
        }
        // check in case the ast was wrongfully initialized and nothing was catched
        if (ast == null) {
            logger.logCritical("AST was not initialized");
            logger.getLogsCritical();
        }
        assert ast != null;

        // Constant fold the ast
        if (contantFolding) {
            ASTConstantFolder astConstantFolder = new ASTConstantFolder();
            astConstantFolder.visit(ast);
            ast = astConstantFolder.getOptimizedAST();
        }

        // debug AST
        if (debugMain) {
            ASTDebuggerVisitor astDebugger = new ASTDebuggerVisitor();
            astDebugger.visit(ast);
        }

        SymbolTable TDS = (new TableCreation()).create(ast);
        if (debugAll) {
            TDS.printScopeTree();  // debug TDS
        }

        //  syntactics checks not done in the grammar due to the LL(1) restriction
        SyntacticChecker syntacticChecker = new SyntacticChecker();
        syntacticChecker.visit(ast);

        // semantics checks that can be done statically
        SemanticChecker semanticChecker = new SemanticChecker(TDS);
        semanticChecker.visit(ast);

        //making sure there is no logs due to a semantic or syntactic error
        cleanLog();

        // create the register allocator
        registerAllocator = new RegisterAllocator(ast, TDS, registerOptimization);

        // generate the compile code from the symbol table and the ast
        (new AsmRecursiveVisitor()).visit(ast);
        AsmCompilerVisitor compiler = new AsmCompilerVisitor(TDS);
        compiler.visit(ast);
        if (debugAll) {
            System.out.println(registerAllocator.getGraphColoringRegister().getLeftoverActions());
        }
        if (debugMain) {
            System.out.println("// SOURCE CODE");
            System.out.println(compiler.getSrc().toString());
            System.out.println("// HEAP PART");
            System.out.println(compiler.getHeap().toString());
            System.out.println("\n\n");
        }

        String outputFileName = outputInPlace ? String.format("%s.src", filename) : String.format("outputCode/%s/%s.src", filename, filename);
        try {
            writeCode(compiler, outputFileName);
        } catch (IOException e) {
            logger.logCritical(e);
            logger.getLogsCritical();
        }
        System.out.printf("File compiled to assembly was written here: %s\n", outputFileName);
        logger.getLogs();
        if (compile) {
            PrintStream original = System.out;
            if (!debugAll && !silent) {
                System.setOut(new PrintStream(new OutputStream() {
                    public void write(int b) {
                    }
                }));
            }else {
                System.out.println("\n\n");
            }
            Lanceur.main(new String[]{"-ass", outputFileName});
            System.setOut(original);
            System.out.printf("File compiled to binary was written here: %s\n", outputInPlace ? String.format("%s.iup", filename) : String.format("outputCode/%s/%s.iup", filename, filename));
        }
        if (launch) {

            Lanceur.main(new String[]{"-batch", String.format("outputCode/%s/%s.iup", filename, filename)});
        }
    }

    public void cleanLog() {
        logger.getLogs();
        if (logger.isThereLogs()) {
            System.out.println("System exited with return code (-1) different than 0");
            System.exit(0);
        }
        logger.cleanLogs();
    }

    public void runQuick(String filename) throws IOException, ANTLRTreeException {
        ANTLRParser parser = new ANTLRParser();
        parser.readSource(new FileInputStream(filename));
        ASTRoot ast = parser.buildAST();
        if (ast == null) {
            logger.logCritical("AST was not initialized");
            logger.getLogsCritical();
        }
        assert ast != null;
        ast = (new ASTConstantFolder()).visit(ast).getOptimizedAST();
        SymbolTable TDS = (new TableCreation()).create(ast);
        (new SyntacticChecker()).visit(ast);
        (new SemanticChecker(TDS)).visit(ast);
        cleanLog();
        registerAllocator = new RegisterAllocator(ast, TDS, registerOptimization);
        AsmRecursiveVisitor recursive = new AsmRecursiveVisitor();
        recursive.visit(ast);
        AsmCompilerVisitor compiler = new AsmCompilerVisitor(TDS);
        compiler.visit(ast);
        filename = filename.replace(".a60", "");
        String outputFileName = String.format("outputCode/%s/%s.src", filename, filename);
        writeCode(compiler, outputFileName);
        System.out.printf("File compiled to assembly was written here: %s\n", outputFileName);
        System.out.println("\n");
        logger.getLogs();
        PrintStream original = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            public void write(int b) {
            }
        }));
        Lanceur.main(new String[]{"-ass", outputFileName});
        System.setOut(original);
        Lanceur.main(new String[]{"-batch", String.format("outputCode/%s/%s.iup", filename, filename)});

    }

    public void writeCode(AsmCompilerVisitor compiler, String filename) throws IOException {
        InputStream libInputStream = Main.class.getClassLoader().getResourceAsStream("lib.src");
        ByteArrayOutputStream libres = new ByteArrayOutputStream();
        byte[] libbuf = new byte[1024];
        int liblen;
        assert libInputStream != null;
        while ((liblen = libInputStream.read(libbuf)) != -1) {
            libres.write(libbuf, 0, liblen);
        }
        String output = libres.toString(StandardCharsets.UTF_8);
        String target = "///////////////////////////////////--APP_SRC--//////////////////////////////////";
        output = output.replace(target, compiler.getSrc().toString());
        target = "////////////////////////////////--HEAP--///////////////////////////////////////";
        output = output.replace(target, compiler.getHeap().toString());
        output = output.replaceAll("\\r\\n", "\n");
        output = output.replaceAll("\\r", "\n");
        Path pathToAssemblyFile = Paths.get(filename);
        if (!outputInPlace) {
            Files.createDirectories(pathToAssemblyFile.getParent());
        }
        File assemblyFile = new File(filename);
        if (!assemblyFile.createNewFile()) System.out.println("File exists and was replaced!");
        Writer fw = new FileWriter(pathToAssemblyFile.toString(), false);
        fw.write(output);
        fw.close();

    }
}
