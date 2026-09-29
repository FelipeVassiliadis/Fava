grammar Fava;

// =========================
// Parser rules
// =========================

prog
    : globalDecl* funcDecl+ EOF
    ;

globalDecl
    : type declItem (COMMA declItem)* SEMI
    ;

funcDecl
    : FUNCTION ID LPAREN paramList? RPAREN returnType? block
    ;

paramList
    : param (COMMA param)*
    ;

param
    : type ID
    ;

returnType
    : ARROW type
    ;

declItem
    : ID (ASSIGN expr)?
    ;

type
    : BOOLTYPE
    | INTTYPE
    | REALTYPE
    | STRINGTYPE
    ;

block
    : LBRACE localDecl* stat* RBRACE
    ;

localDecl
    : type declItem (COMMA declItem)* SEMI
    ;

stat
    : PRINT expr SEMI                        #PrintStat
    | ID ASSIGN expr SEMI                    #AssignStat
    | block                                  #BlockStat
    | WHILE LPAREN expr RPAREN stat          #WhileStat
    | IF LPAREN expr RPAREN stat ELSE stat   #IfElseStat
    | IF LPAREN expr RPAREN stat             #IfStat
    | RETURN expr? SEMI                      #ReturnStat
    | funcCall SEMI                          #CallStat
    | SEMI                                   #EmptyStat
    ;

expr
    : LPAREN expr RPAREN                     #ParenExpr
    | op=(MINUS | NOT) expr                  #UnaryExpr
    | expr op=(MULT | DIV | MOD) expr        #MultDivModExpr
    | expr op=(PLUS | MINUS) expr            #AddSubExpr
    | expr CONCAT expr                       #ConcatExpr
    | expr op=(LT | GT | LEQ | GEQ) expr     #RelExpr
    | expr op=(EQ | NEQ) expr                #EqExpr
    | expr AND expr                          #AndExpr
    | expr OR expr                           #OrExpr
    | funcCall                               #CallExpr
    | literal                                #LiteralExpr
    | ID                                     #IdExpr
    ;

funcCall
    : ID LPAREN argList? RPAREN
    ;

argList
    : expr (COMMA expr)*
    ;

literal
    : INTEGER                                #IntLiteral
    | REAL                                   #RealLiteral
    | STRING                                 #StringLiteral
    | TRUE                                   #TrueLiteral
    | FALSE                                  #FalseLiteral
    ;

// =========================
// Lexer rules
// =========================

// keywords
FUNCTION   : [fF][uU][nN][cC][tT][iI][oO][nN];
RETURN     : [rR][eE][tT][uU][rR][nN];
PRINT      : [pP][rR][iI][nN][tT];
WHILE      : [wW][hH][iI][lL][eE];
IF         : [iI][fF];
ELSE       : [eE][lL][sS][eE];

BOOLTYPE   : [bB][oO][oO][lL];
INTTYPE    : [iI][nN][tT][eE][gG][eE][rR];
REALTYPE   : [rR][eE][aA][lL];
STRINGTYPE : [sS][tT][rR][iI][nN][gG];

TRUE       : [tT][rR][uU][eE];
FALSE      : [fF][aA][lL][sS][eE];

NOT        : [nN][oO][tT];
MOD        : [mM][oO][dD];
AND        : [aA][nN][dD];
OR         : [oO][rR];

// operators and punctuation
ARROW      : '->';
ASSIGN     : ':=';
CONCAT     : '||';

LEQ        : '<=';
GEQ        : '>=';
NEQ        : '<>';
LT         : '<';
GT         : '>';
EQ         : '=';

PLUS       : '+';
MINUS      : '-';
MULT       : '*';
DIV        : '/';

LPAREN     : '(';
RPAREN     : ')';
LBRACE     : '{';
RBRACE     : '}';
COMMA      : ',';
SEMI       : ';';

// literals
INTEGER
    : DIGIT+
    ;

REAL
    : DIGIT+ '.' DIGIT* EXP?
    | '.' DIGIT+ EXP?
    | DIGIT+ EXP
    ;

STRING
    : '"' ( ESC_SEQ | ~["\\\r\n] )* '"'
    ;

ID
    : LETTER (LETTER | DIGIT | '_')*
    | '_' (LETTER | DIGIT | '_')+
    ;

// comments / whitespace
SL_COMMENT
    : '//' .*? (EOF | '\n') -> skip
    ;

ML_COMMENT
    : '/*' .*? '*/' -> skip
    ;

WS
    : [ \t\r\n]+ -> skip
    ;

// invalid chars
ERROR
    : .
    ;

// fragments
fragment LETTER
    : [a-zA-Z]
    ;

fragment DIGIT
    : [0-9]
    ;

fragment EXP
    : [eE] [+\-]? DIGIT+
    ;

fragment ESC_SEQ
    : '\\' [btnfr"\\]
    ;