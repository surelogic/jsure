grammar LayerPromises;
options {
  output=AST;
  TokenLabelType=Token;
  ASTLabelType=Tree;  
}
tokens {
  START_IMAGINARY;
	//Imaginary Nodes	
	QualifiedThisExpression;	
	BooleanType;
  ByteType;
  CharType;
  ShortType;
  IntType;
  FloatType;
  DoubleType;
  LongType;
  NamedType;
  TypeRef;
  ArrayType;
  TypeQualifierPattern;
  Annotations;
  ThisExpression;
  SuperExpression;
  ReturnValueDeclaration;
  FieldRef;
  TypeExpression;
  VariableUseExpression;
  ClassExpression;
  Nothing;
  Parameters;
  Throws;
  AnyTarget;
  AndTarget;
  OrTarget;
  NoTarget;
  NotTarget;
  UnionTarget;
  UnidentifiedTarget;
  TargetList;
  MayReferTo;
  Layer;
  InLayer;
  TypeSet;
  AllowsReferencesFrom;
  END_IMAGINARY;
  
  NOTHING='nothing';
  // Needed for modifier clauses
  INSTANCE;
  MUTABLE;
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
    /*
    for(int i=0; i<DFA18_transition.length; i++) {
      DFA18_transition[i] = null;
    }
    for(int i=0; i<DFA31_transition.length; i++) {
      DFA31_transition[i] = null;
    }
    for(int i=0; i<DFA34_transition.length; i++) {
      DFA34_transition[i] = null;
    }
    */
  }
  public static synchronized void init() {
    if (initialized) {
      return;
    }
    /*
    for (int i=0; i<DFA18_transition.length; i++) {
      DFA18_transition[i] = DFA.unpackEncodedString(DFA18_transitionS[i]);
    }
    for (int i=0; i<DFA31_transition.length; i++) {
      DFA31_transition[i] = DFA.unpackEncodedString(DFA31_transitionS[i]);
    }
    for (int i=0; i<DFA34_transition.length; i++) {
      DFA34_transition[i] = DFA.unpackEncodedString(DFA34_transitionS[i]);
    }
    */
    initialized = true;
  }
}

/* Disables the default error handling so we can get the error immediately */
@members{
@Override
protected void mismatch(IntStream input, int ttype, BitSet follow)
	throws RecognitionException{
	throw new MismatchedTokenException(ttype, input);
}
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

/*************************************************************************************
 * Rules supporting layers
 *************************************************************************************/	

mayReferTo
  : NOTHING EOF -> ^(MayReferTo ^(NoTarget))
  | typeTarget EOF -> ^(MayReferTo typeTarget)
  ;

layer
  : identifier 'may' 'refer' 'to' typeTarget EOF -> ^(Layer identifier typeTarget)
//  | identifier noTarget -> ^(Layer identifier noTarget)
  | identifier EOF -> ^(Layer identifier)
  ;

/*
noTarget
  : EOF -> ^(NoTarget)   
  ;
*/

inLayer
  : inLayerList EOF -> ^(InLayer inLayerList)
  ;

inLayerList
  : inLayerTarget (',' inLayerTarget)* -> ^(TargetList inLayerTarget (inLayerTarget)*)
  ;

inLayerTarget  
  : unionTargets 
  | unidentifiedTarget
  ;

typeSet
  : identifier '=' typeTarget EOF -> ^(TypeSet identifier typeTarget)
  ;

allowsReferencesFrom
  : NOTHING EOF -> ^(AllowsReferencesFrom ^(NoTarget))  
  | typeTarget EOF -> ^(AllowsReferencesFrom typeTarget)
  ;

/*************************************************************************************
 * Targets to match layers, typesets, packages and types
 *************************************************************************************/ 

typeTarget
  : (andTarget -> andTarget) 
    ('|' a=andTarget -> ^(OrTarget $typeTarget $a))*  
  ; 

andTarget
  : (baseTarget -> baseTarget) 
    ('&' b=baseTarget -> ^(AndTarget $andTarget $b))*
  ; 

baseTarget
  : '!' '(' typeTarget ')' -> ^(NotTarget typeTarget)
  | unionTargets
  | qualifiedName '+' -> ^(UnidentifiedTarget qualifiedName '+')
  | unidentifiedTarget
  | '(' typeTarget ')' -> typeTarget
  ;

// Could be a layer, typeset, package, or type
unidentifiedTarget
  : qualifiedName -> ^(UnidentifiedTarget qualifiedName)
  ;

unionTargets
  : qualifiedName '.' '{' targetList '}' -> ^(UnionTarget qualifiedName targetList)
  ;

targetList
  : unidentifiedTarget (',' unidentifiedTarget)* -> unidentifiedTarget (unidentifiedTarget)*
  ;

/******************************************************************
 ** Constructs for basic patterns for Java declarations
 ******************************************************************/

simpleNamePattern
  : WildcardIdentifier
  | IDENTIFIER
  | STAR  
  ;
  
qualifiedName
  : IDENTIFIER (DOT IDENTIFIER)*
  ;

namedTypes
  : namedType (',' namedType)* -> namedType+
  ;

// For testing
wildcardIdentifier
  : WildcardIdentifier
  ;	

/******************************************************************
 ** Constructs for modifiers
 **********************
 ********************************************/

/*
  ( "public" 
  | "protected"
  | "private"
  | "abstract"
  | "synchronized"
  | "final"
  )*
*/

modifierPattern
  : accessModPattern staticPattern? 
// finalPattern?
  ;

accessModPattern
  : (PUBLIC | PROTECTED | PRIVATE)? 
  ;
	
staticPattern
  : STATIC 
  | '!' STATIC -> INSTANCE
  ;

/*
finalPattern
  : FINAL 
  | '!' FINAL -> MUTABLE
  ;
*/
 
/*************************************************************************************
 * All Rules Supporting rules
 *************************************************************************************/	
 
qualifiedClassExpression
	: namedType '.' CLASS -> ^(ClassExpression namedType)
	; 
 
qualifiedThisExpression
	: namedType '.' THIS -> ^(QualifiedThisExpression namedType)
	; 
 
typeExpression
	: namedType -> ^(TypeExpression namedType)
	;

fieldRef
  : qualifiedFieldRef
  | simpleFieldRef
  ;

qualifiedFieldRef
	: typeExpression '.' IDENTIFIER -> ^(FieldRef typeExpression IDENTIFIER)
	;

namedType
  	: name -> ^(NamedType name)	 	
  	;

typeName
  	: {isType("", LayerPromisesParser.this.input.LT(1))}? IDENTIFIER
  	| qualifiedTypeName  	 	
  	;

qualifiedTypeName
	: IDENTIFIER ({isType($qualifiedTypeName.text, LayerPromisesParser.this.input.LT(2))}? 
	              '.' IDENTIFIER)*	              
	;
	
simpleFieldRef
  : thisExpr DOT IDENTIFIER -> ^(FieldRef thisExpr IDENTIFIER) 
	| IDENTIFIER -> ^(FieldRef ^(ThisExpression THIS) IDENTIFIER)
	;	

/*************************************************************************************
 * Simple Java constructs
 *************************************************************************************/	
 
simpleExpression
	: thisExpr 
	| varUse
	;

varUse
    	: IDENTIFIER ->	^(VariableUseExpression IDENTIFIER)
    	;
 	
thisExpr
    : THIS -> ^(ThisExpression THIS)
    ;
    
nothing
    : EOF -> ^(Nothing)
    ;		

accessModifiers
	: (PUBLIC | PROTECTED | PRIVATE)? STATIC?
	;

/*************************************************************************************
 * Typical syntax to be parsed 
 *************************************************************************************/	

name
	: // IDENTIFIER ( options {greedy=false;} : ('.' IDENTIFIER))* 
	  IDENTIFIER (DOT name)*
	;

identifier
  : IDENTIFIER 
  | 'may' 
  | 'refer' 
  | 'to'
  | 'nothing'
  ;

/*************************************************************************************
 * Standard Java tokens
 *************************************************************************************/	

ABSTRACT : 'abstract';
CLASS : 'class';
FINAL : 'final';
FOR : 'for';
PRIVATE : 'private';
PROTECTED : 'protected';
PUBLIC : 'public';
STATIC : 'static';
THIS : 'this';
VOID : 'void';
BOOLEAN : 'boolean';
BYTE : 'byte';
CHAR : 'char';
SHORT : 'short';
INT : 'int';
FLOAT : 'float';
DOUBLE : 'double';
LONG : 'long';

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
DSTAR : '**';
IN    : 'in';
PLUS  : '+';

HexLiteral : '0' ('x'|'X') HexDigit+ IntegerTypeSuffix? ;

DecimalLiteral : ('0' | '1'..'9' '0'..'9'*) IntegerTypeSuffix? ;

OctalLiteral : '0' ('0'..'7')+ IntegerTypeSuffix? ;

fragment
HexDigit : ('0'..'9'|'a'..'f'|'A'..'F') ;

fragment
IntegerTypeSuffix : ('l'|'L') ;

FloatingPointLiteral
    :   (('0'..'9')+ '.') => ('0'..'9') '.' ('0'..'9')* Exponent? FloatTypeSuffix?
    |   '.' ('0'..'9')+ Exponent? FloatTypeSuffix?
    |   (('0'..'9')+ Exponent) =>  ('0'..'9')+ Exponent FloatTypeSuffix?
    |   ('0'..'9')+ Exponent? FloatTypeSuffix
	;

fragment
Exponent : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

fragment
FloatTypeSuffix : ('f'|'F'|'d'|'D') ;

CharacterLiteral
    :   '\'' ( EscapeSequence | ~('\''|'\\') ) '\''
    ;

StringLiteral
    :  '"' ( EscapeSequence | ~('\\'|'"') )* '"'
    ;

fragment
EscapeSequence
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
    |   UnicodeEscape
    |   OctalEscape
    ;

fragment
OctalEscape
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7')
    ;

fragment
UnicodeEscape
    :   '\\' 'u' HexDigit HexDigit HexDigit HexDigit
    ;

fragment
ID
    : LETTER (LETTER|JavaIDDigit)*
    ;

fragment
ID2
    : (LETTER|JavaIDDigit)+
    ;

WildcardIdentifier
    : ID (STAR ID2)* STAR
    | STAR ID2 (STAR ID2)* STAR?
    | ID (STAR ID2)+
    ;

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
PromiseStringLiteral
  : QUOTE ( 'I'|~QUOTE )* QUOTE
  ; 
