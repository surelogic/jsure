grammar SLThreadRoleAnnotations;
options {
  output=AST;
  TokenLabelType=Token;
  ASTLabelType=Tree;  
}
tokens {
  START_IMAGINARY;
  //Imaginary Nodes
	
  // Stolen from ScopedPromises.g
  TestResult;	
 
  
  // stolen from SLAnnotations.g
  QualifiedRegionName;
  RegionSpecifications;
  RegionName;
  NamedType;
  
  // Original to SLThreadRoleAnnotations.g
  ThreadRoleName;
  ThreadRoleSimpleNames;
  ThreadRoleDeclaration;
  ThreadRoleGrant;
  ThreadRoleRevoke;
  ThreadRoleIncompatible;
  ThreadRoleNot;
  ThreadRoleAnd;
  ThreadRoleOr;
  ThreadRoleExpr;
  ThreadRoleImport;
  ThreadRoleRename;
  ThreadRoleCardSpec;
  RegionReportRoles;
  ThreadRoleCR;
  ThreadRoleConstraint;
  ThreadRole;
  ThreadRoleTransparent;
  Nothing;
  
  // Module annos
  ModulePromise;
  ModuleWrapper;
  ModuleScope;
  ModuleChoice;
  VisClause;
  NoVisClause;
  Export;
  BlockImport;
  ExportTo;
  OfNamesClause;
  Names;
  END_IMAGINARY;
  
  COLORDECLARE = '@ThreadRoleDeclare';
  COLOR = '@ThreadRole';
  COLORGRANT = '@ThreadRoleGrant';
  COLORREVOKE = '@ThreadRoleRevoke';
  COLORCONSTRAINT = '@ThreadRoleConstraint';
  COLORINCOMPATIBLE = '@IncompatibleThreadRoles';
  COLORRENAME = '@ThreadRoleRename';
  COLORIMPORT = '@ThreadRoleImport';
  COLORCARD = '@ThreadRoleCardinality';
  COLORIZED = '@RegionReportThreadRoles';
  COLORCONSTRAINEDREGIONS = '@ThreadRoleConstrainedRegions';
  COLORTRANSPARENT = '@ThreadRoleTransparent';
  
  MODULE = '@Module';
  VIS = '@Vis';
  NOVIS = '@NoVis';
  EXPORT = '@Export';
  BLOCKIMPORT = '@BlockImport';
  
  //keywords
  CONTAINS = 'contains';
  FROM = 'from';
  OF = 'of';
  TO = 'to';
  FOR = 'for';
  
  INSTANCE;
}
@header{
package com.surelogic.annotation.parse;

import edu.cmu.cs.fluid.ir.*;
}  
@lexer::header{
package com.surelogic.annotation.parse;

import com.surelogic.parse.*;
}
@lexer::members{
  /**
   * Makes it create TreeTokens, instead of CommonTokens
   */
  @Override 
  public Token emit() {
    Token t = new TreeToken(input, state.type, state.channel, state.tokenStartCharIndex, getCharIndex()-1);
    t.setLine(state.tokenStartLine);
    t.setText(state.text);
    t.setCharPositionInLine(state.tokenStartCharPositionInLine);
    emit(t);
    return t;
  }
  
  private static boolean initialized = true;
  
  public static synchronized void clear() {
    initialized = false;
    for(int i=0; i<DFA3_transition.length; i++) {
      DFA3_transition[i] = null;
    }
  }
  public static synchronized void init() {
    if (initialized) {
      return;
    }
    for (int i=0; i<DFA3_transition.length; i++) {
      DFA3_transition[i] = DFA.unpackEncodedString(DFA3_transitionS[i]);
    }
    initialized = true;
  }
}

/* Disables the default error handling so we can get the error immediately */
@members{
/*
@Override
protected void mismatch(IntStream input, int ttype, BitSet follow)
	throws RecognitionException{
	throw new MismatchedTokenException(ttype, input);
}
*/
@Override
public Object recoverFromMismatchedSet(IntStream input, RecognitionException e, BitSet follow)
	throws RecognitionException{
	reportError(e);
	throw e;
}

  IRNode context;
  
  void setContext(IRNode n) {
    context = n;
  }
  
  boolean isType(String prefix, Token id) { 
    return ParseUtil.isType(context, prefix, id.getText());
  }
}

@rulecatch{
	catch(RecognitionException e){
		throw e;
	}
}
/**
 * Test support rules
 */

testResult
  : 'is' IDENTIFIER '&' IDENTIFIER ':' -> ^(TestResult IDENTIFIER '&' IDENTIFIER ':')
  | 'is' IDENTIFIER ':' -> ^(TestResult IDENTIFIER ':')
  | 'is' IDENTIFIER '&' IDENTIFIER EOF -> ^(TestResult IDENTIFIER '&' IDENTIFIER)
  | 'is' IDENTIFIER EOF -> ^(TestResult IDENTIFIER)
  ;
  
testResultComment
  : ('/*' | '/**') 
    testResult -> testResult
  ;
	
/*************************************************************************************
 * Typical syntax to be parsed 
 *************************************************************************************/	
qualifiedName
	: IDENTIFIER ( options {greedy=false;} : ('.' IDENTIFIER))+ 
	;
	
simpleName
     	: IDENTIFIER	
     	;		
	
name
	: // IDENTIFIER ( /* options {greedy=false;} : */ ('.' IDENTIFIER))* 
	  IDENTIFIER ('.' IDENTIFIER)*
	;

/*************************************************************************************
 * Syntax for naming regions
 *************************************************************************************/
 identifier
      : IDENTIFIER | 'is' | 'protects' | 
        'none' | 'reads' | 'writes' | 'any' | 'readLock' | 'writeLock' |
        'into' | 'nothing' | 'itself'
      ;
qualifiedNamedType
	: qualifiedName -> ^(NamedType qualifiedName)
	;
	
simpleNamedType	
	: identifier -> ^(NamedType identifier)
	;
	
qualifiedRegionName
	: qualifiedNamedType ':' identifier -> ^(QualifiedRegionName qualifiedNamedType identifier)
	| simpleNamedType ':' identifier -> ^(QualifiedRegionName simpleNamedType identifier)
	;

regionSpecifications
	: regionSpecification (',' regionSpecification)* -> ^(RegionSpecifications regionSpecification+)
	;

regionSpecification
	: qualifiedRegionName 
	| simpleRegionSpecification
	;
	
simpleRegionSpecification	
	: regionName
	| (LBRACKET RBRACKET) -> ^(RegionName LBRACKET RBRACKET)
	;

regionName
	: IDENTIFIER -> ^(RegionName IDENTIFIER)
	;

/*************************************************************************************
 * Syntax for ThreadRole Names (simple, qualified, either) and lists of same
 *************************************************************************************/	
 
 threadRoleName
 	:	qualifiedName -> ^(ThreadRoleName qualifiedName)
 	|	simpleName  -> ^(ThreadRoleName simpleName)
 	;
 
 threadRoleNames
 	: threadRoleName (',' threadRoleName)* -> threadRoleName+
 	;
 	
 threadRoleSimpleNames
 	:	simpleName (',' simpleName)* -> simpleName+
 	;
 	
 /*************************************************************************************
 * Syntax for ThreadRole Expressions
 *************************************************************************************/	
 
 threadRoleLit
 	:	threadRoleNot -> threadRoleNot
 	|	LPAREN threadRoleNot RPAREN -> threadRoleNot
 	|	threadRoleName -> threadRoleName
 	|	LPAREN threadRoleName RPAREN -> threadRoleName
 	;
 
 threadRoleNot
 	:	'!' threadRoleName -> ^(ThreadRoleNot threadRoleName);
 	
 threadRoleAnd 
 	:	threadRoleLit ('&' threadRoleLit)+ -> ^(ThreadRoleAnd threadRoleLit+)
 	| LPAREN threadRoleLit ('&' threadRoleLit)+ RPAREN -> ^(ThreadRoleAnd threadRoleLit+)
 	;

 threadRoleOrElem
 	: threadRoleLit
 	| threadRoleAnd	
 	;
 	
 threadRoleOr 
 	:	
 	|	threadRoleOrElem ('|' threadRoleOrElem)+ -> ^(ThreadRoleOr threadRoleOrElem+)
 	|	LPAREN threadRoleOrElem ('|' threadRoleOrElem)+ RPAREN -> ^(ThreadRoleOr threadRoleOrElem+)
 	;
 	
 threadRoleExpr
 	:	threadRoleLit -> ^(ThreadRoleExpr threadRoleLit)
 	|	threadRoleAnd -> ^(ThreadRoleExpr threadRoleAnd)
 	|	threadRoleOr -> ^(ThreadRoleExpr threadRoleOr)
 	;	

/*************************************************************************************
 * ThreadRole annotations taking NO arguments
 *************************************************************************************/	
 nothing
    : EOF -> ^(Nothing)
    ;		
 
 threadRoleTransparent
 	:	nothing EOF -> ^(ThreadRoleTransparent nothing)
 	;

/*************************************************************************************
 * ThreadRole annotations taking lists of names as arguments
 *************************************************************************************/	

 threadRoleDeclare 
 	: threadRoleNames EOF -> ^(ThreadRoleDeclaration threadRoleNames)
 	;
 	
 threadRoleGrant 
 	:	threadRoleNames EOF -> ^(ThreadRoleGrant threadRoleNames)
 	;
 	
 threadRoleRevoke
 	:	threadRoleNames EOF -> ^(ThreadRoleRevoke threadRoleNames)	
 	;
 	
threadRoleIncompatible
	:	threadRoleNames EOF -> ^(ThreadRoleIncompatible threadRoleNames)
	; 	

/*************************************************************************************
 * ThreadRole annotations taking expressions as their sole argument
 *************************************************************************************/	
threadRole
	:	threadRoleExpr EOF -> ^(ThreadRole threadRoleExpr)
	;
	
threadRoleConstraint
	:	threadRoleExpr EOF -> ^(ThreadRoleConstraint threadRoleExpr)	
	;

/*************************************************************************************
 * Other ThreadRole annotations
 *************************************************************************************/	
threadRoleImport
  : qualifiedName '.' '*' EOF -> ^(ThreadRoleImport qualifiedName '.' '*')
	|	qualifiedName EOF -> ^(ThreadRoleImport qualifiedName)
	;
	
threadRoleRename
	:	threadRoleName FOR threadRoleExpr -> ^(ThreadRoleRename threadRoleName threadRoleExpr) 
	;
	
regionReportRoles
	:	regionSpecifications EOF -> ^(RegionReportRoles regionSpecifications)
	;
	
	
threadRoleConstrainedRegions
	:	threadRoleExpr FOR regionSpecifications EOF -> ^(ThreadRoleCR threadRoleExpr regionSpecifications)
	;
	
	
threadRoleCard
	: threadRoleCardSpec EOF -> threadRoleCardSpec
	;
	

threadRoleCardSpec
	:	'1' FOR threadRoleSimpleNames -> ^(ThreadRoleCardSpec threadRoleSimpleNames)
	;
	
/*************************************************************************************
 * Module annotations
 *************************************************************************************/	

moduleChoice 
	:	name -> ^(ModulePromise name)
	|	name CONTAINS names  -> ^(ModuleWrapper name names)
	|	name FOR '*' -> ^(ModuleScope name)
	;
	
module
	:	moduleChoice EOF -> ^(ModuleChoice moduleChoice)
	;
	
vis 
	:	(name)? EOF -> ^(VisClause name?)
	;

noVis 
	:	nothing EOF -> ^(NoVisClause)
	;

export 
	:	names FROM name EOF -> ^(Export names name)
	|	names TO names EOF -> ^(ExportTo names names)
	;

names
 	: name (',' name)* -> ^(Names name+)
 	;
 	
 blockImport 
 	:	ofNamesClause? FROM names EOF -> ^(BlockImport ofNamesClause? names)
 	;
 	
 ofNamesClause 
 	:	OF names -> ^(OfNamesClause names)
 	;
 	

/*************************************************************************************
 * Standard Java tokens
 *************************************************************************************/	

//ABSTRACT : 'abstract';
//CLASS : 'class';
//FINAL : 'final';
FOR : 'for';
//PRIVATE : 'private';
//PROTECTED : 'protected';
//PUBLIC : 'public';
//STATIC : 'static';
//THIS : 'this';
//VOID : 'void';
//BOOLEAN : 'boolean';
//BYTE : 'byte';
//CHAR : 'char';
//SHORT : 'short';
//INT : 'int';
//FLOAT : 'float';
//DOUBLE : 'double';
//LONG : 'long';

COLON : ':';
SEMI  : ';';
DOT   : '.';
LBRACKET : '[';
RBRACKET : ']';
AT : '@';
LPAREN : '(';
RPAREN : ')';
QUOTE : '\'';
DQUOTE : '"';
LBRACE : '{';
RBRACE : '}';
COMMA : ',';
STAR  : '*';
//DSTAR : '**';
//IN    : 'in';

//HexLiteral : '0' ('x'|'X') HexDigit+ IntegerTypeSuffix? ;

//DecimalLiteral : ('0' | '1'..'9' '0'..'9'*) IntegerTypeSuffix? ;

//OctalLiteral : '0' ('0'..'7')+ IntegerTypeSuffix? ;

fragment
HexDigit : ('0'..'9'|'a'..'f'|'A'..'F') ;

//fragment
//IntegerTypeSuffix : ('l'|'L') ;

//FloatingPointLiteral
//    :   (('0'..'9')+ '.') => ('0'..'9') '.' ('0'..'9')* Exponent? FloatTypeSuffix?
//    |   '.' ('0'..'9')+ Exponent? FloatTypeSuffix?
//    |   (('0'..'9')+ Exponent) =>  ('0'..'9')+ Exponent FloatTypeSuffix?
//    |   ('0'..'9')+ Exponent? FloatTypeSuffix
//	;

//fragment
//Exponent : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

//fragment
//FloatTypeSuffix : ('f'|'F'|'d'|'D') ;

//CharacterLiteral
//    :   '\'' ( EscapeSequence | ~('\''|'\\') ) '\''
//    ;

//StringLiteral
//    :  '"' ( EscapeSequence | ~('\\'|'"') )* '"'
//    ;

//fragment
//EscapeSequence
//    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
//    |   UnicodeEscape
//    |   OctalEscape
//    ;

//fragment
//OctalEscape
//    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
//    |   '\\' ('0'..'7') ('0'..'7')
//    |   '\\' ('0'..'7')
//    ;

//fragment
//UnicodeEscape
//    :   '\\' 'u' HexDigit HexDigit HexDigit HexDigit
//    ;

fragment
ID
    : LETTER (LETTER|JavaIDDigit)*
    ;

//fragment
//ID2
//    : (LETTER|JavaIDDigit)+
//    ;

//WildcardIdentifier
//    : ID (STAR ID2)* STAR
//    | STAR ID2 (STAR ID2)* STAR?
//    | ID (STAR ID2)+
//    ;

IDENTIFIER
    : ID
    ;
    
/**I found this char range in JavaCC's grammar, but Letter and Digit overlap.
   Still works, but...
 */
fragment
LETTER
    :  '\u0024' |
       '\u0041'..'\u005a' |
       '\u005f' |
       '\u0061'..'\u007a' |
       '\u00c0'..'\u00d6' |
       '\u00d8'..'\u00f6' |
       '\u00f8'..'\u00ff' |
       '\u0100'..'\u1fff' |
       '\u3040'..'\u318f' |
       '\u3300'..'\u337f' |
       '\u3400'..'\u3d2d' |
       '\u4e00'..'\u9fff' |
       '\uf900'..'\ufaff'
    ;

fragment
JavaIDDigit
    :  '\u0030'..'\u0039' |
       '\u0660'..'\u0669' |
       '\u06f0'..'\u06f9' |
       '\u0966'..'\u096f' |
       '\u09e6'..'\u09ef' |
       '\u0a66'..'\u0a6f' |
       '\u0ae6'..'\u0aef' |
       '\u0b66'..'\u0b6f' |
       '\u0be7'..'\u0bef' |
       '\u0c66'..'\u0c6f' |
       '\u0ce6'..'\u0cef' |
       '\u0d66'..'\u0d6f' |
       '\u0e50'..'\u0e59' |
       '\u0ed0'..'\u0ed9' |
       '\u1040'..'\u1049'
   ;
	
WS  :  (' '|'\r'|'\t'|'\u000C'|'\n')+ {skip();}
    ;

// This makes it a token? 
// For some reason, this doesn't take 1-character strings
//PromiseStringLiteral
//  : QUOTE ( 'I'|~QUOTE )* QUOTE
//  ; 
