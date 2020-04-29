/*******************************************************************************

                                    ALGOL 60
                                 LL(1) GRAMMAR
                                                                      <Redacted>
                                                                      <Redacted>
                                                                   Erwan Kessler
                                                                      <Redacted>
    TELECOM Nancy
    PCL 2019-2020

        Rules that are more permissive than the language specification
    are annotated with "XXX(+):"

*******************************************************************************/

grammar algol60;

options {
    k           = 1     ; /* LL(1) */
    backtrack   = false ;
    output      = AST   ;
    language    = Java  ;
}


tokens {
    ADD                 ;
    AND                 ;
    ASSIGN              ;
    BLOCK               ;
    DECL                ;
    DECL_ARRAY          ;
    DECL_ARRAY_SEG      ;
    DECL_ARRAY_BOUND    ;
    DECL_ARRAY_BOUNDS   ;
    DECL_SWITCH         ;
    DECL_FCN            ;
    DECL_FCN_PARAMS     ;
    DECL_FCN_PARAMS_SEG ;
    DECL_FCN_PARAM      ;
    DECL_FCN_SPECS      ;
    DECL_FCN_SPEC       ;
    DECL_FCN_VALUES     ;
    DIV                 ;
    DUMMY               ;
    EQ                  ; /* equal */
    EXPR                ;
    FCN                 ;
    FOR                 ;
    GE                  ; /* greater than or equal */
    GOTO                ;
    GT                  ; /* greater than */
    ID                  ;
    IDIV                ;
    IF                  ;
    IFE                 ; /* if for expressions */
    IFF                 ; /* equivalence */
    ITERATOR            ;
    IMPL                ; /* implication */
    LABEL               ;
    LE                  ; /* less than or equal */
    LOGICAL             ;
    LT                  ; /* less than */
    MINUS               ; /* unary */
    MUL                 ;
    NEQ                 ; /* not equal */
    NOT                 ;
    NUMBER              ;
    OR                  ;
    OWN                 ;
    PARAMS              ;
    PARAMS_SEG          ;
    PARAM               ;
    PLUS                ; /* unary */
    POW                 ;
    STEPUNTIL           ;
    STRING              ;
    SUB                 ;
    SUBSCRIPT           ;
    TYPE                ;
    VAR                 ;
    WHILE               ;
}

@lexer::header {
    package eu.telecomnancy.pcl.antlr;
}

@parser::header {
    package eu.telecomnancy.pcl.antlr;
}

@rulecatch {
    // ANTLR does not generate its normal rule try/catch
    catch( RecognitionException e) {
        throw e;
    }
    catch(RuntimeException e){
        throw e;
    }
}

@parser::members {
    @Override
    public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
        String hdr = getErrorHeader(e);
        String msg = getErrorMessage(e, tokenNames);
        throw new RuntimeException(hdr + ":" + msg);
    }
}

@lexer::members {
    @Override
    public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
        String hdr = getErrorHeader(e);
        String msg = getErrorMessage(e, tokenNames);
        throw new RuntimeException(hdr + ":" + msg);
    }
}

prog
    : block EOF
        -> block
    ;

block
    : UREAL ':' block
        -> ^(LABEL ^(NUMBER UREAL)) block
    | IDF ':' block
        -> ^(LABEL ^(ID IDF)) block
    | block_anon
    ;

block_anon
    : 'begin' comment* (decl ';' comment*)* stat (';' comment* stat)* 'end'
        -> ^(BLOCK decl* stat+)
    ;

decl
    : 'own'
        ( type
            ( decl_simple
                -> ^(DECL OWN ^(TYPE type) decl_simple)
            | decl_array
                -> ^(DECL_ARRAY OWN ^(TYPE type) decl_array)
            )
        | decl_array
            -> ^(DECL_ARRAY OWN decl_array)
        )
    | type
        ( decl_simple
            -> ^(DECL ^(TYPE type) decl_simple)
        | decl_array
            -> ^(DECL_ARRAY ^(TYPE type) decl_array)
        | decl_fcn
            -> ^(DECL_FCN ^(TYPE type) decl_fcn)
        )
    | decl_array
        -> ^(DECL_ARRAY decl_array)
    | decl_switch
        -> ^(DECL_SWITCH decl_switch)
    | decl_fcn
        -> ^(DECL_FCN decl_fcn)
    ;

decl_fcn
    : 'procedure' IDF decl_fcn_params ';' comment* decl_fcn_values decl_fcn_specs stat
        -> ^(ID IDF) decl_fcn_params decl_fcn_values decl_fcn_specs stat
    ;

decl_fcn_params
    : '(' IDF (',' IDF)* ')' decl_fcn_params_seg_others?
        -> ^(DECL_FCN_PARAMS ^(DECL_FCN_PARAMS_SEG ^(DECL_FCN_PARAM ^(ID IDF))+)
             decl_fcn_params_seg_others?)
    |
        -> ^(DECL_FCN_PARAMS)
    ; /* XXX(+): Letter string delimiter should not contain any digit */

decl_fcn_params_seg_others
    : (IDF ':' '(' decl_fcn_params_seg_others_args ')')+
        -> ^(DECL_FCN_PARAMS_SEG ^(ID IDF) decl_fcn_params_seg_others_args)+
    ;

decl_fcn_params_seg_others_args
    : IDF (',' IDF)*
        -> ^(DECL_FCN_PARAM ^(ID IDF))+
    ;

decl_fcn_values
    : 'value' IDF (',' IDF)* ';' comment*
        -> ^(DECL_FCN_VALUES ^(ID IDF)+)
    |
        -> ^(DECL_FCN_VALUES)
    ;

decl_fcn_specs
    : decl_fcn_spec*
        -> ^(DECL_FCN_SPECS decl_fcn_spec*)
    ;

decl_fcn_spec
    : decl_fcn_spec_type IDF (',' IDF)* ';' comment*
        -> ^(DECL_FCN_SPEC ^(TYPE decl_fcn_spec_type) ^(ID IDF)+)
    ;

decl_fcn_spec_type
    : type ('array' | 'procedure')?
    | 'array'
    | 'switch'
    | 'procedure'
    | 'label'
    | 'string'
    ;

decl_switch
    : 'switch' IDF ':=' expr (',' expr)*
        -> ^(ID IDF) ^(EXPR expr)+
    ;

decl_array
    : 'array' decl_array_seg (',' decl_array_seg)*
        -> decl_array_seg+
    ;

decl_array_seg
    : IDF (',' IDF)* '[' decl_array_bounds (',' decl_array_bounds)* ']'
        -> ^(DECL_ARRAY_SEG ^(ID IDF)+ ^(DECL_ARRAY_BOUNDS decl_array_bounds+))
    ;

decl_array_bounds
    : expr ':' expr
        -> ^(DECL_ARRAY_BOUND ^(EXPR expr) ^(EXPR expr))
    ;

decl_simple
    : IDF (',' IDF)*
        -> ^(ID IDF)+
    ;

stat
    : stat_simple
    | stat_if
    | stat_for
    ;

stat_simple
    : UREAL ':' stat
        -> ^(LABEL ^(NUMBER UREAL)) stat
    | IDF
        ( ':' stat
            -> ^(LABEL ^(ID IDF)) stat
        | subscript ':=' stat_assign
            -> ^(ASSIGN ^(VAR ^(ID IDF) ^(SUBSCRIPT subscript)) stat_assign)
        | ':=' stat_assign
            -> ^(ASSIGN ^(VAR ^(ID IDF)) stat_assign)
        | params
            -> ^(FCN ^(ID IDF) params)
        |
            -> ^(FCN ^(ID IDF) ^(PARAMS))
        )
    | stat_goto
    | block_anon
    /* dummy statement */
    |
        -> ^(DUMMY)
    ;

stat_assign
    : expr (':=' expr)*
        -> ^(EXPR expr)+
; /* XXX(+): only last element should be expr */

stat_goto
    : 'go to' expr
        -> ^(GOTO expr)
    ;

stat_if
    : 'if' expr 'then'
        ( stat_if_simple
          ( 'else' stat
              -> ^(IF ^(EXPR expr) stat_if_simple stat)
          |
              -> ^(IF ^(EXPR expr) stat_if_simple)
          )
        | stat_for
            -> ^(IF ^(EXPR expr) stat_for)
        )
    ;

stat_if_simple
    : UREAL ':' stat_if_simple
        -> ^(LABEL ^(NUMBER UREAL)) stat_if_simple
    | IDF
        ( ':' stat_if_simple
            -> ^(LABEL ^(ID IDF)) stat_if_simple
        | subscript ':=' stat_assign
            -> ^(ASSIGN ^(VAR ^(ID IDF) ^(SUBSCRIPT subscript)) stat_assign)
        | ':=' stat_assign
            -> ^(ASSIGN ^(VAR ^(ID IDF)) stat_assign)
        | params
            -> ^(FCN ^(ID IDF) params)
        |
            -> ^(FCN ^(ID IDF) ^(PARAMS))
        )
    | stat_goto
    | block_anon
    /* dummy statement */
    |
        -> ^(DUMMY)
    ; /* stat_if_simple is stat without if */

stat_for
    : 'for' IDF
        ( subscript ':=' stat_for_cond (',' stat_for_cond)* 'do' stat
            -> ^(FOR ^(VAR ^(ID IDF) ^(SUBSCRIPT subscript)) ^(ITERATOR stat_for_cond+) stat)
        | ':=' stat_for_cond (',' stat_for_cond)* 'do' stat
            -> ^(FOR ^(VAR ^(ID IDF)) ^(ITERATOR stat_for_cond+) stat)

        )
    ;

stat_for_cond
    : expr
        ( 'step' expr 'until' expr
            -> ^(STEPUNTIL ^(EXPR expr) ^(EXPR expr) ^(EXPR expr))
        | 'while' expr
            -> ^(WHILE ^(EXPR expr) ^(EXPR expr))
        |
            -> ^(EXPR expr)
        )
    ;

expr
    : expr_simple
    | 'if' expr 'then' expr_simple 'else' expr
        -> ^(IFE ^(EXPR expr) ^(EXPR expr_simple) ^(EXPR expr))
    ; /* XXX(+): all expressions are treated as expr */



expr_simple
    : ( expr_9
          -> expr_9
      )
      ( ('<=>' expr_9
            -> ^(IFF $expr_simple expr_9)
        )*
      )
    ;

expr_9
    : ( expr_8
          -> expr_8
      )
      ( ('=>' expr_8
            -> ^(IMPL $expr_9 expr_8)
        )*
      )
    ;

expr_8
    : ( expr_7
          -> expr_7
      )
      ( ('\\/' expr_7
            -> ^(OR $expr_8 expr_7)
        )*
      )
    ;

expr_7
    : ( expr_6
          -> expr_6
      )
      ( ('/\\' expr_6
            -> ^(AND $expr_7 expr_6)
        )*
      )
    ;

expr_6
    : expr_5
    | '~' expr_5
        -> ^(NOT expr_5)
    ;

expr_5
    : ( expr_4
          -> expr_4
      )
      ( ('<' expr_4
            -> ^(LT $expr_5 expr_4)
        )
      | ('<=' expr_4
            -> ^(LE $expr_5 expr_4)
        )
      | ('=' expr_4
            -> ^(EQ $expr_5 expr_4)
        )
      | ('>=' expr_4
            -> ^(GE $expr_5 expr_4)
        )
      | ('>' expr_4
            -> ^(GT $expr_5 expr_4)
        )
      | ('<>' expr_4
            -> ^(NEQ $expr_5 expr_4)
        )
      )?
    ;

expr_4
    : ( expr_3
          -> expr_3
      )
      ( ('+' expr_3
            -> ^(ADD $expr_4 expr_3)
        )
      | ('-' expr_3
            -> ^(SUB $expr_4 expr_3)
        )
      )*
    ;

expr_3
    : '+' expr_2
        -> ^(PLUS expr_2)
    | '-' expr_2
        -> ^(MINUS expr_2)
    | expr_2
    ;

expr_2
    : ( expr_1
          -> expr_1
      )
      ( ('*' expr_1
            -> ^(MUL $expr_2 expr_1)
        )
      | ('/' expr_1
            -> ^(DIV $expr_2 expr_1)
        )
      | ('//' expr_1
            -> ^(IDIV $expr_2 expr_1)
        )
      )*
    ;

expr_1
    : ( expr_0
          -> expr_0
      )
      ( ('**' expr_0
            -> ^(POW $expr_1 expr_0)
        )*
      )
    ;

expr_0
    : UREAL
        -> ^(NUMBER UREAL)
    | 'false'
        -> ^(LOGICAL 'false')
    | 'true'
        -> ^(LOGICAL 'true')
    | IDF
        ( subscript
            -> ^(VAR ^(ID IDF) ^(SUBSCRIPT subscript))
        | params
            -> ^(VAR ^(ID IDF) params)
        |
            -> ^(VAR ^(ID IDF))
        )
    | '(' expr ')'
        -> expr
    ;

params
    : '(' params_part ')' params_seg_others?
        -> ^(PARAMS ^(PARAMS_SEG params_part) params_seg_others?)
    ;

params_seg_others
    : (IDF ':' '(' params_part ')')+
        -> ^(PARAMS_SEG ^(ID IDF) params_part)+
    ;

params_part
    : params_part_arg (',' params_part_arg)*
        -> params_part_arg+
    ;

params_part_arg
    : expr
        -> ^(PARAM ^(EXPR expr))
    | string
        -> ^(PARAM string)
    ;

subscript
    : '[' expr (',' expr)* ']'
        -> ^(EXPR expr)+
    ;

string
    : STR
        -> ^(STRING STR)
    ; /* XXX(+): only basic strings are recognized */

type
    : 'Boolean'
    | 'integer'
    | 'real'
    ;

comment
    : 'comment' (~(';'))* ';'
    ;

fragment DIGIT
    : '0'..'9'
    ;

fragment LETTER
    : 'A'..'Z'
    | 'a'..'z'
    ;

STR
    : '`' (~('`' | '\''))* '\''
    ;

UREAL
    : (DIGIT+ ('.' DIGIT+)? | '.' DIGIT+) ('e' ('+' | '-')? DIGIT+)?
    ; /* XXX(+): all numbers are treated as UREAL */

IDF
    : LETTER (DIGIT | LETTER)*
    ;

WS
    : (' ' | '\t' | '\r' | '\n')+
    {$channel=HIDDEN;}
    ;
