package eu.telecomnancy.pcl.ast;

public enum ASTType {
    NONE,
    BOOLEAN,
    INTEGER,
    REAL,
    LABEL,
    STRING,
    ERROR;

    public static ASTType getType(String type) {
        switch (type) {
            case "Boolean":
                return BOOLEAN;
            case "integer":
                return INTEGER;
            case "real":
                return REAL;
            default:
                return NONE;
        }
    }

    @Override
    public String toString() {
        switch (this) {
            case BOOLEAN:
                return "Boolean";
            case INTEGER:
                return "integer";
            case REAL:
                return "real";
            case LABEL:
                return "label";
            case STRING:
                return "string";
            case ERROR:
                return "error";
            default:
                return "none";
        }
    }
}
