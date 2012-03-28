grammar ScopedPromises;
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
  ScopedPromise;
  AnyTarget;
  AndTarget;
  OrTarget;
  NotTarget;
  ConstructorDeclPattern;
  MethodDeclPattern;
  FieldDeclPattern;
  TypeDeclPattern;
  NamedTypePattern;
  Parameters;
  Throws;
  InPattern;
  InPackagePattern;
  InAndPattern;
  InOrPattern;
  InNotPattern;
  WildcardTypeQualifierPattern;
  END_IMAGINARY;
  
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
    for(int i=0; i<DFA18_transition.length; i++) {
      DFA18_transition[i] = null;
    }
    for(int i=0; i<DFA31_transition.length; i++) {
      DFA31_transition[i] = null;
    }
    for(int i=0; i<DFA34_transition.length; i++) {
      DFA34_transition[i] = null;
    }
  }
  public static synchronized void init() {
    if (initialized) {
      return;
    }
    for (int i=0; i<DFA18_transition.length; i++) {
      DFA18_transition[i] = DFA.unpackEncodedString(DFA18_transitionS[i]);
    }
    for (int i=0; i<DFA31_transition.length; i++) {
      DFA31_transition[i] = DFA.unpackEncodedString(DFA31_transitionS[i]);
    }
    for (int i=0; i<DFA34_transition.length; i++) {
      DFA34_transition[i] = DFA.unpackEncodedString(DFA34_transitionS[i]);
    }
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
 * Rules supporting scoped promises (@Promise, @Assume, @Module)
 *************************************************************************************/	

// Still have to check for parens
scopedPromise
  : '@' identifier -> ^(ScopedPromise identifier ^(AnyTarget))
  | '@' identifier promiseContent -> ^(ScopedPromise identifier promiseContent ^(AnyTarget))
  | '@' identifier FOR promiseTarget -> ^(ScopedPromise identifier promiseTarget)  
  | '@' identifier promiseContent FOR promiseTarget -> ^(ScopedPromise identifier promiseContent promiseTarget)
  ;  

promiseContent
  : '(' ( ~FOR )* 
  ;

promiseTarget
  : (andTarget -> andTarget) 
    ('|' a=andTarget -> ^(OrTarget $promiseTarget $a))*  
  ; 

andTarget
  : (baseTarget -> baseTarget) 
    ('&' b=baseTarget -> ^(AndTarget $andTarget $b))*
  ; 
    	
baseTarget
  : (constructorDeclPattern) => constructorDeclPattern
  // No real need to match against return value
  | (noReturnMethodDeclPattern) => noReturnMethodDeclPattern
  | (fieldDeclPattern) => fieldDeclPattern
  | typeDeclPattern
  | '!' '(' promiseTarget ')' -> ^(NotTarget promiseTarget)
  | '(' promiseTarget ')' -> promiseTarget
  ;

/******************************************************************
 ** Constructs for basic patterns for Java declarations
 ******************************************************************/

constructorDeclPattern
  : accessModPattern 'new' methodSigPattern inPattern ->
    ^(ConstructorDeclPattern accessModPattern inPattern methodSigPattern)
  ;

// Factors out the main part of a method pattern
// from the MethodDeclPatterns below
methodMatchPattern
  : methodNamePattern methodSigPattern inPattern -> inPattern methodNamePattern methodSigPattern
  ;

/* Now unused
methodDeclPattern
  : modifierPattern returnTypeSigPattern methodMatchPattern ->
    ^(MethodDeclPattern modifierPattern returnTypeSigPattern methodMatchPattern)
  ;
*/

noReturnMethodDeclPattern
  : modifierPattern methodMatchPattern ->
    ^(MethodDeclPattern modifierPattern ^(NamedTypePattern STAR) methodMatchPattern)
  ;

fieldDeclPattern
  : modifierPattern typeSigPattern simpleNamePattern inPattern ->
    ^(FieldDeclPattern modifierPattern typeSigPattern inPattern simpleNamePattern)
  ;

// Other patterns handled by second line  
typeDeclPattern
  : modifierPattern qualifiedName ->
    ^(TypeDeclPattern modifierPattern qualifiedName ^(InPattern))  
  | modifierPattern simpleNamePattern inPackagePattern ->
	    ^(TypeDeclPattern modifierPattern simpleNamePattern ^(InPattern inPackagePattern))
  ;

//In operator targets  
inPattern
  : IN wildcardTypeQualifierPattern inPackagePattern -> ^(InPattern wildcardTypeQualifierPattern inPackagePattern)
  | IN '(' inTypePattern ')' inPackagePattern -> ^(InPattern inTypePattern inPackagePattern)
  | -> ^(InPattern)
  ;

inTypePattern
  : (inAndPattern -> inAndPattern) ( '|' b=inAndPattern -> ^(InOrPattern $inTypePattern $b))*
  ;

inAndPattern
  :  (inBasePattern -> inBasePattern) ('&' b=inBasePattern -> ^(InAndPattern $inAndPattern $b))*
  ;
  
inBasePattern
  : wildcardTypeQualifierPattern
  | '!' '(' inTypePattern ')' -> ^(InNotPattern inTypePattern)
  | '(' inTypePattern ')' -> inTypePattern
  ;
  
inPackagePattern
  : IN wildcardPkgQualifierPattern -> wildcardPkgQualifierPattern
  | IN '(' inPackageRootPattern ')' -> inPackageRootPattern
  | -> ^(InPackagePattern)
  ;

inPackageRootPattern
  : (inAndPkgPattern -> inAndPkgPattern) ( '|' b=inAndPkgPattern -> ^(InOrPattern $inPackageRootPattern $b))*
  ;

inAndPkgPattern
  :  (inBasePkgPattern -> inBasePkgPattern) ('&' b=inBasePkgPattern -> ^(InAndPattern $inAndPkgPattern $b))*
  ;
  
inBasePkgPattern
  : wildcardPkgQualifierPattern
  | '!' '(' inPackageRootPattern ')' -> ^(InNotPattern inPackageRootPattern)
  | '(' inPackageRootPattern ')' -> inPackageRootPattern
  ;

wildcardTypeQualifierPattern
  :	methodNamePattern (DOT methodNamePattern)* -> ^(WildcardTypeQualifierPattern methodNamePattern (DOT methodNamePattern)*)
  ;
  
wildcardPkgQualifierPattern
  :	methodNamePattern (DOT methodNamePattern)* -> ^(InPackagePattern methodNamePattern (DOT methodNamePattern)*)
  ;
//Done 'in' operator
  
methodNamePattern
  : WildcardIdentifier
  | identifier
  | STAR
  | DSTAR
  ;
  
methodSigPattern
  : '(' ')' -> ^(Parameters)
  | '(' paramTypeSigPattern (',' paramTypeSigPattern)* ')' -> ^(Parameters paramTypeSigPattern+)
  | '(' DSTAR ')' -> ^(Parameters ^(NamedTypePattern DSTAR))
  ;
  
paramTypeSigPattern
  : typeSigPattern
  ;
  
returnTypeSigPattern
  : VOID | typeSigPattern
  ;
  
typeSigPattern
  : typeSigPattern2 ('[' ']')+ -> ^(ArrayType typeSigPattern2 ('[' ']')+)
  | typeSigPattern2 
  ;
typeSigPattern2
  : BOOLEAN -> ^(BooleanType)
  | BYTE -> ^(ByteType)
  | CHAR -> ^(CharType)
  | SHORT -> ^(ShortType)
  | INT -> ^(IntType)
  | FLOAT -> ^(FloatType)
  | DOUBLE -> ^(DoubleType)
  | LONG -> ^(LongType)
  | STAR -> ^(NamedTypePattern STAR)
  | namedTypePattern 
  ;

simpleNamePattern
  : WildcardIdentifier
  | identifier
  | STAR  
  ;

namedTypePattern
  : WildcardIdentifier -> ^(NamedTypePattern WildcardIdentifier)
  | identifier -> ^(NamedTypePattern identifier)
  | qualifiedName -> ^(NamedTypePattern qualifiedName)  
  ;	

qualifiedName
  : identifier (DOT identifier)+
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
  : (modPattern)*
  ;

modPattern
  : PUBLIC | PROTECTED | PRIVATE
  | STATIC 
  | '!' STATIC -> INSTANCE
  | FINAL 
  | '!' FINAL -> MUTABLE
  ;

accessModPattern
  : (PUBLIC | PROTECTED | PRIVATE)?
  ;
 
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
	: typeExpression '.' identifier -> ^(FieldRef typeExpression identifier)
	;

namedType
  	: name -> ^(NamedType name)	 	
  	;

typeName
  	: {isType("", ScopedPromisesParser.this.input.LT(1))}? identifier
  	| qualifiedTypeName  	 	
  	;

qualifiedTypeName
	: identifier ({isType($qualifiedTypeName.text, ScopedPromisesParser.this.input.LT(2))}? 
	              '.' identifier)*	              
	;
	
simpleFieldRef
  : thisExpr DOT identifier -> ^(FieldRef thisExpr identifier) 
	| identifier -> ^(FieldRef ^(ThisExpression THIS) identifier)
	;	

/*************************************************************************************
 * Simple Java constructs
 *************************************************************************************/	
 
simpleExpression
	: thisExpr 
	| varUse
	;

varUse
    	: identifier ->	^(VariableUseExpression identifier)
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
	: // identifier ( options {greedy=false;} : ('.' identifier))* 
	  identifier (DOT name)*
	;

identifier
  : IDENTIFIER | IN 
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
