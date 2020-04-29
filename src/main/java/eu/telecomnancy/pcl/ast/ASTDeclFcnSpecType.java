package eu.telecomnancy.pcl.ast;

import eu.telecomnancy.pcl.symbolTable.Record;

public enum ASTDeclFcnSpecType {
    NONE,
    BOOLEAN,
    INTEGER,
    REAL,
    ARRAY_NONE,
    ARRAY_BOOLEAN,
    ARRAY_INTEGER,
    ARRAY_REAL,
    PROCEDURE_NONE,
    PROCEDURE_BOOLEAN,
    PROCEDURE_INTEGER,
    PROCEDURE_REAL,
    LABEL,
    STRING,
    SWITCH;

    @Override
    public String toString() {
        switch (this) {
            case BOOLEAN:
                return "BOOLEAN";
            case INTEGER:
                return "INTEGER";
            case REAL:
                return "REAL";
            case LABEL:
                return "LABEL";
            case STRING:
                return "STRING";
            case SWITCH:
                return "SWITCH";
            case ARRAY_NONE:
                return "ARRAY_NONE";
            case ARRAY_BOOLEAN:
                return "ARRAY_BOOLEAN";
            case ARRAY_INTEGER:
                return  "ARRAY_INTEGER";
            case ARRAY_REAL:
                return "ARRAY_REAL";
            case PROCEDURE_BOOLEAN:
                return "PROCEDURE_BOOLEAN";
            case PROCEDURE_INTEGER:
                return "PROCEDURE_INTEGER";
            case PROCEDURE_NONE:
                return "PROCEDURE_NONE";
            case PROCEDURE_REAL:
                return "PROCEDURE_REAL";
            default:
                return "NONE";
        }
    }

    public static ASTDeclFcnSpecType getType(String type, String genre) {
        switch (genre) {
            case "array":
                switch (type) {
                    case "Boolean":
                        return ARRAY_BOOLEAN;
                    case "integer":
                        return ARRAY_INTEGER;
                    case "real":
                        return ARRAY_REAL;
                    default:
                        return NONE;
                }
            case "procedure":
                switch (type) {
                    case "Boolean":
                        return PROCEDURE_BOOLEAN;
                    case "integer":
                        return PROCEDURE_INTEGER;
                    case "real":
                        return PROCEDURE_REAL;
                    default:
                        return NONE;
                }
            default:
                switch (type) {
                    case "Boolean":
                        return BOOLEAN;
                    case "integer":
                        return INTEGER;
                    case "real":
                        return REAL;
                    case "array":
                        return ARRAY_REAL;
                    case "procedure":
                        return PROCEDURE_NONE;
                    case "label":
                        return LABEL;
                    case "string":
                        return STRING;
                    case "switch":
                        return SWITCH;
                    default:
                        return NONE;
                }
        }
    }

    public ASTType toASTType() {
        switch (this) {
            case BOOLEAN:
            case ARRAY_BOOLEAN:
            case PROCEDURE_BOOLEAN:
                return ASTType.BOOLEAN;
            case INTEGER:
            case ARRAY_INTEGER:
            case PROCEDURE_INTEGER:
                return ASTType.INTEGER;
            case REAL:
            case ARRAY_REAL:
            case PROCEDURE_REAL:
                return ASTType.REAL;
            case LABEL:
            case SWITCH:
                return ASTType.LABEL;
            case STRING:
                return ASTType.STRING;
            default:
                return ASTType.NONE;
        }
    }

    public Record.Genre toRecordGenre() {
        switch (this) {
            case ARRAY_BOOLEAN:
            case ARRAY_INTEGER:
            case ARRAY_NONE:
            case ARRAY_REAL:
                return Record.Genre.ARRAY;
            case PROCEDURE_BOOLEAN:
            case PROCEDURE_INTEGER:
            case PROCEDURE_NONE:
            case PROCEDURE_REAL:
                return Record.Genre.PROCEDURE;
            case LABEL:
                return Record.Genre.LABEL;
            case SWITCH:
                return Record.Genre.SWITCH;
            default:
                return Record.Genre.VARIABLE;
        }
    }


}
