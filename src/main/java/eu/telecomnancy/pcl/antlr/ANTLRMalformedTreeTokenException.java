package eu.telecomnancy.pcl.antlr;

import java.lang.Integer;

public class ANTLRMalformedTreeTokenException extends ANTLRTreeException {
    public ANTLRMalformedTreeTokenException() {
        super();
    }

    public ANTLRMalformedTreeTokenException(int token) {
        super(Integer.toString(token));
    }
}
