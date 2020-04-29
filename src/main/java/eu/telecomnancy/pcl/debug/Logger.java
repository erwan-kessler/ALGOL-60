package eu.telecomnancy.pcl.debug;

import eu.telecomnancy.pcl.ast.ASTNode;
import eu.telecomnancy.pcl.symbolTable.Record;

import java.util.LinkedList;
import java.util.Queue;

public class Logger {
    Queue<String> queue = new LinkedList<>();
    Queue<String> queueCritical = new LinkedList<>();
    Queue<String> queueWarning = new LinkedList<>();
    private boolean isCritical = false;
    private boolean showWarnings = false;
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public Logger() {
    }

    public Logger(boolean showWarnings) {
        this.showWarnings = showWarnings;
    }

    public void log(String msg, ASTNode node) {
        queue.add(ANSI_RED + msg + ANSI_RESET + " at line " + node.getSourceLine() + ", character " + node.getSourceChar());
    }

    public void log(String msg) {
        queue.add(ANSI_RED + msg);
    }

    public void log(Exception exception) {
        queue.add(ANSI_BLUE + exception.getClass().getSimpleName() + ANSI_RED + " : " + exception.getMessage() + ANSI_RESET);
    }

    public void log(Exception exception, ASTNode node) {
        queue.add(ANSI_BLUE + exception.getClass().getSimpleName() + ANSI_RED + " : " + exception.getMessage() + ANSI_RESET + " at line " + node.getSourceLine() + ", character " + node.getSourceChar());
    }

    public void log(Exception exception, Record node) {
        queue.add(ANSI_BLUE + exception.getClass().getSimpleName() + ANSI_RED + " : " + exception.getMessage() + ANSI_RESET + " at line " + node.getLine() + ", character " + node.getSource());
    }

    public void logCritical(String msg, ASTNode node) {
        isCritical = true;
        queueCritical.add(ANSI_RED + msg + ANSI_RESET + " at line " + node.getSourceLine() + ", character " + node.getSourceChar());
    }

    public void logCritical(String msg) {
        isCritical = true;
        queueCritical.add(ANSI_RED + msg+ANSI_RESET);
    }

    public void logCritical(Exception exception) {
        isCritical = true;
        queueCritical.add(ANSI_BLUE + exception.getClass().getSimpleName() + ANSI_RED + " : " + exception.getMessage() + ANSI_RESET);
    }

    public void logCritical(Exception exception, ASTNode node) {
        isCritical = true;
        queueCritical.add(ANSI_BLUE + exception.getClass().getSimpleName() + ANSI_RED + " : " + exception.getMessage() + ANSI_RESET + " at character " + node.getSourceChar() + ", line " + node.getSourceLine());
    }

    public void logCritical(Exception exception, Record node) {
        isCritical = true;
        queueCritical.add(ANSI_BLUE + exception.getClass().getSimpleName() + ANSI_RED + " : " + exception.getMessage()+ ANSI_RESET + " at line " + node.getLine() + ", character " + node.getSource());
    }

    public void logWarning(String msg, ASTNode node) {
        queueWarning.add(ANSI_PURPLE + msg + ANSI_RESET + " at line " + node.getSourceLine() + ", character " + node.getSourceChar());
    }

    public void logWarning(String msg) {
        queueWarning.add(ANSI_PURPLE + msg+ANSI_RESET);
    }

    public void logWarning(Exception exception) {
        queueWarning.add(ANSI_YELLOW + exception.getClass().getSimpleName() + ANSI_PURPLE + " : " + exception.getMessage() + ANSI_RESET);
    }

    public void logWarning(Exception exception, ASTNode node) {
        queueWarning.add(ANSI_YELLOW + exception.getClass().getSimpleName() + ANSI_PURPLE + " : " + exception.getMessage() + ANSI_RESET + " at character " + node.getSourceChar() + ", line " + node.getSourceLine());
    }

    public void logWarning(Exception exception, Record node) {
        queueWarning.add(ANSI_YELLOW + exception.getClass().getSimpleName() + ANSI_PURPLE + " : " + exception.getMessage()+ ANSI_RESET + " at line " + node.getLine() + ", character " + node.getSource());
    }

    public boolean isCritical() {
        return isCritical;
    }

    public void getLogs() {
        for (String s : queue) {
            System.out.println(ANSI_PURPLE + "ERROR: " + s);
        }
        for (String s : queueCritical) {
            System.out.println(ANSI_YELLOW + "CRITICAL ERROR: " + s);
        }
        if (showWarnings) {
            for (String s : queueWarning) {
                System.out.println(ANSI_BLUE + "WARNING: " + s);
            }
        }
    }
    public void cleanLogs(){
        queue.clear();
        queueCritical.clear();
        queueWarning.clear();
    }

    public void getLogsCritical() {
        for (String s : queueCritical) {
            System.out.println(ANSI_YELLOW + "CRITICAL ERROR: " + s);
        }
        System.exit(0);
    }

    public boolean isThereLogs() {
        return !(queue.isEmpty() && queueCritical.isEmpty());
    }

    public String getInternalLogs() {
        StringBuilder out = new StringBuilder();
        for (String s : queue) {
            out.append( "ERROR: ").append(s.replaceAll("\u001B\\[[;\\d]*m", "")).append("\n");
        }
        for (String s : queueCritical) {
            out.append("CRITICAL ERROR: ").append(s.replaceAll("\u001B\\[[;\\d]*m", "")).append("\n");
        }
        return out.toString();
    }

}
