// Lua 5.4 grammar for Antlr 4
// Based on the official grammar (https://www.lua.org/manual/5.4/manual.html#9)
// Lexer copied from (https://github.com/antlr/grammars-v4/blob/master/lua/Lua.g4)
// NOTE: This is a pure Lua 5.4 grammar, other versions (or forks) are NOT supported!

grammar Lua;

@header {
package fi.benjami.code4jvm.lua.parser;
}

chunk : block;
block : stat*;

stat : ';' # empty |
	targets=varlist '=' values=explist # assignVar |
	functioncall # functionCallStmt |
	label # gotoLabel |
	'break' # breakStmt |
	'goto' target=Name # gotoStmt |
	'do' block 'end' # doEndBlock |
	'while' condition=exp 'do' block 'end' # whileLoop |
	'repeat' block 'until' condition=exp # repeatLoop |
	'if' exp 'then' block ('elseif' exp 'then' block)* ('else' block)? 'end' # ifBlock |
	'for' counter=Name '=' start=exp ',' end=exp (',' step=exp)? 'do' block 'end' # countedForLoop |
	'for' entries=namelist 'in' iterable=explist 'do' block 'end' # forInLoop |
	'function' Name ('.' Name)* (':' oopPart=Name)? funcbody # function |
	'local' 'function' name=Name funcbody # localFunction |
	'local' names=attnamelist ('=' values=explist)? # assignLocal |
	'return' (values=explist)? # return
	;

attnamelist : Name attrib (',' Name attrib)*;
attrib : ('<' Name '>')?;

label : '::' Name '::';

varlist : var (',' var)*;
var : Name | table=prefixexp '[' exp ']' | table=prefixexp '.' Name;

namelist : Name (',' Name)*;

explist : exp (',' exp)*;
exp : 'nil' # nilLiteral |
	'false' # falseLiteral |
	'true' # trueLiteral |
	Numeral # numberLiteral |
	LiteralString # stringLiteral |
	'...' # varargs |
	functiondef # functionDefExp_ |
	prefixexp # prefixExp_ |
	tableconstructor # tableConstructor_ |
	<assoc=right> lhs=exp op='^' rhs=exp # arithmeticOp |
	unop exp # unaryOp |
	lhs=exp op=('*' | '/' | '%' | '//') rhs=exp # arithmeticOp |
	lhs=exp op=('+' | '-') rhs=exp # arithmeticOp |
	<assoc=right> lhs=exp '..' rhs=exp # stringConcat |
	lhs=exp op=('<' | '>' | '<=' | '>=' | '~=' | '==') rhs=exp # compareOp |
	lhs=exp op='and' rhs=exp # logicalOp |
	lhs=exp op='or' rhs=exp # logicalOp |
	lhs=exp ('&' | '|' | '~' | '<<' | '>>') lhs=exp # bitwiseOp
	;
// NOTE: to avoid indirect left recursion, do not use var or functioncall in prefixexp
prefixexp : Name # varReference |
	table=prefixexp '[' key=exp ']' # arrayAccess |
	table=prefixexp '.' key=Name # tableAccess |
	func=prefixexp (':' oopPart=Name)? args # functionCall_ |
	'(' exp ')' # group
	;

functioncall : func=prefixexp (':' oopPart=Name)? args;
args : '(' (explist)? ')' | tableconstructor | LiteralString;
functiondef : 'function' funcbody;
funcbody : '(' (argList=parlist)? ')' block 'end';
parlist : names=namelist (',' rest='...')? | rest='...';

tableconstructor : '{' (fieldlist)? '}';
fieldlist : field (fieldsep field)* (fieldsep)?;
field : '[' keyExp=exp ']' '=' value=exp | key=Name '=' value=exp | value=exp;
fieldsep : ',' | ';';

unop : '-' | 'not' | '#' | '~';

// Lexer
Name : [a-zA-Z_][a-zA-Z_0-9]*;

LiteralString : NORMAL_STRING | CHAR_STRING | LONG_STRING;
NORMAL_STRING : '"' ( EscapeSequence | ~('\\'|'"') )* '"';
CHAR_STRING : '\'' ( EscapeSequence | ~('\''|'\\') )* '\'';
LONG_STRING : '[' NESTED_STR ']';

fragment
NESTED_STR
    : '=' NESTED_STR '='
    | '[' .*? ']'
    ;

Numeral: INT | HEX | FLOAT | HEX_FLOAT;
INT: Digit+;
HEX : '0' [xX] HexDigit+;
FLOAT
	: Digit+ '.' Digit* ExponentPart?
	| '.' Digit+ ExponentPart?
	| Digit+ ExponentPart
	;

HEX_FLOAT
	: '0' [xX] HexDigit+ '.' HexDigit* HexExponentPart?
	| '0' [xX] '.' HexDigit+ HexExponentPart?
	| '0' [xX] HexDigit+ HexExponentPart
	;

fragment
ExponentPart
    : [eE] [+-]? Digit+
    ;

fragment
HexExponentPart
    : [pP] [+-]? Digit+
    ;

fragment
EscapeSequence
    : '\\' [abfnrtvz"'\\]
    | '\\' '\r'? '\n'
    | DecimalEscape
    | HexEscape
    | UtfEscape
    ;

fragment
DecimalEscape
	: '\\' Digit
	| '\\' Digit Digit
	| '\\' [0-2] Digit Digit
	;

fragment
HexEscape: '\\' 'x' HexDigit HexDigit;

fragment
UtfEscape: '\\' 'u{' HexDigit+ '}';

fragment
Digit: [0-9];

fragment
HexDigit : [0-9a-fA-F];

COMMENT: '--[' NESTED_STR ']' -> channel(HIDDEN);

LINE_COMMENT
	: '--'
	(                                               // --
	| '[' '='*                                      // --[==
	| '[' '='* ~('='|'['|'\r'|'\n') ~('\r'|'\n')*   // --[==AA
	| ~('['|'\r'|'\n') ~('\r'|'\n')*                // --AAA
	) ('\r\n'|'\r'|'\n'|EOF)
	-> channel(HIDDEN)
	;

WS
    : [ \t\u000C\r\n]+ -> skip
    ;

SHEBANG : '#' '!' ~('\n'|'\r')* -> channel(HIDDEN);