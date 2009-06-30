grammar SLAnnotations;
options {
  output=AST;
  TokenLabelType=Token;
  ASTLabelType=Tree;  
}
tokens {
  START_IMAGINARY;

	//Imaginary Nodes	
	TestResult;
	LockDeclaration;
	PolicyLockDeclaration;
	QualifiedLockName;
	RegionName;
	QualifiedRegionName;
	QualifiedClassLockExpression;
	QualifiedThisExpression;
	ImplicitClassLockExpression; 
	EffectSpecifications;
	EffectSpecification;
	RequiresLock;
	ReturnsLock;
	InRegion;
	Aggregate;
	RegionEffects;
	Reads;
	Writes; 
	IsLock;
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
  AnyInstanceExpression;
  ClassExpression;
  SimpleLockName;
  LockNames;
  StartsSpecification;
  MappedRegionSpecification;
  RegionMapping;
  FieldMappings;
  NewRegionDeclaration;
  RegionSpecifications;
  Nothing;
  Expressions;
  LockSpecifications;
  ReadLock;
  WriteLock;
  ScopedPromise;
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
  END_IMAGINARY;
  
	//Locking
	LOCK = '@Lock';
	LOCKS = '@Locks';
	REQUIRESLOCK='@RequiresLock';
	ISLOCK='@IsLock';
	POLICYLOCK='@PolicyLock';
	RETURNSLOCK='@ReturnsLock';
	
	//Effects
	INREGION='@InRegion';
	MAPFIELDS='@MapFields';
	MAPREGION='@MapRegion';
	AGGREGATE='@Aggregate';
	REGION='@Region';
	READS='@Reads';
	WRITES='@Writes';
	
	//keywords
	PROTECTS='protects';
	NOTHING='nothing';
	SUPER='super';
	ANY='any';
	IS='is';
	INTO='into';
	NOTHING='nothing';
	READ_LOCK='readLock';
	WRITE_LOCK='writeLock';
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
}

/* Disables the default error handling so we can get the error immediately */
@members{
@Override
protected void mismatch(IntStream input, int ttype, BitSet follow)
	throws RecognitionException{
	throw new MismatchedTokenException(ttype, input);
}

  /**
   * Don't try to recover from mismatch errors.
   * Need this to undo a new feature of ANTLR 3.1.
   */
  @Override
  protected Object recoverFromMismatchedToken(IntStream input, int ttype, BitSet follow)
		throws RecognitionException
	{
	  mismatch(input, ttype, follow);
	  // won't get here, mismatch always throws an exception
	  return null;
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
 * Uniqueness rules
 *************************************************************************************/	
	
borrowedFunction
    : thisExpr EOF -> thisExpr
    ;
    
borrowedList
    : borrowedExpressionList EOF -> borrowedExpressionList
   	| borrowedExpression EOF -> borrowedExpression
    ;

borrowedExpressionList
	  : borrowedExpression (',' borrowedExpression)+ -> ^(Expressions borrowedExpression+)
    ;

borrowedExpression
    : varUse | thisExpr 
    ;    

uniqueJava5Method
    : thisExpr EOF -> thisExpr
    | returnValue EOF -> returnValue
    | thisExpr ',' returnValue EOF -> ^(Expressions thisExpr returnValue)
    | returnValue ',' thisExpr EOF -> ^(Expressions thisExpr returnValue)
    ;

uniqueJavadocMethod
    : uniqueExpressionList EOF -> uniqueExpressionList
   	| uniqueExpression EOF -> uniqueExpression
    ;
    
uniqueJavadocConstructor
    : uniqueConstructorExpressionList EOF -> uniqueConstructorExpressionList
   	| uniqueConstructorExpression EOF -> uniqueConstructorExpression
    ;

uniqueExpressionList
	  : uniqueExpression (',' uniqueExpression)+ -> ^(Expressions uniqueExpression+)
    ;

uniqueExpression
    : varUse | thisExpr | returnValue 
    ;

uniqueConstructorExpressionList
	  : uniqueConstructorExpression (',' uniqueConstructorExpression)+ -> ^(Expressions uniqueConstructorExpression+)
    ;

uniqueConstructorExpression
    : varUse 
    ;
    
/*************************************************************************************
 * Thread effects rules
 *************************************************************************************/	

starts
    	: NOTHING EOF -> ^(StartsSpecification NOTHING)	
    	;

/*************************************************************************************
 * Locking rules
 *************************************************************************************/	

locks 
    : '(' '{' lockJava5 (',' lockJava5)* '}' ')' -> lockJava5+
	;

lockJava5
	: LOCK '(' '"' lock '"' ')' -> lock
	;

lock
	:IDENTIFIER 'is' lockExpression PROTECTS regionName EOF -> ^(LockDeclaration IDENTIFIER lockExpression regionName)
	;

requiresLock
	: lockSpecifications -> ^(RequiresLock lockSpecifications)
	| -> ^(RequiresLock)
	;
	
isLock
	: simpleLockSpecification EOF -> ^(IsLock simpleLockSpecification)
	;

policyLock
	: policyLockDeclaration EOF -> policyLockDeclaration
	;

returnsLock
    	: lockSpecification EOF -> ^(ReturnsLock lockSpecification)
    	;
    	
/************* Locking supporting rules **************************/

policyLockDeclaration
	: IDENTIFIER 'is' lockExpression EOF -> ^(PolicyLockDeclaration IDENTIFIER lockExpression)
	;

lockSpecifications
	: lockSpecification (',' lockSpecification)* -> lockSpecification+
	;

lockSpecification
  : qualifiedLockSpecification
  | simpleLockSpecification
  ;
  
simpleLockSpecification
  : simpleLockName DOT READ_LOCK '(' ')' -> ^(ReadLock simpleLockName)
  | simpleLockName DOT WRITE_LOCK '(' ')' -> ^(WriteLock simpleLockName)
  | simpleLockName
  ;
  
qualifiedLockSpecification
  : qualifiedLockName DOT READ_LOCK '(' ')' -> ^(ReadLock qualifiedLockName)
  | qualifiedLockName DOT WRITE_LOCK '(' ')' -> ^(WriteLock qualifiedLockName)
  | qualifiedLockName
  ;  

/* Only used by tests in SLParse, but not by any other grammar rules */
lockName
	: qualifiedLockName
	| simpleLockName
	;
	
lockExpression
	: qualifiedLockExpressian 
	| simpleLockExpression
	;
	
qualifiedLockName
	: (parameterLockName) => parameterLockName
	| typeQualifiedLockName 
	| innerClassLockName
	;
	
parameterLockName
  : simpleExpression ':' IDENTIFIER -> ^(QualifiedLockName simpleExpression IDENTIFIER)  
	;

qualifiedLockExpressian
	: (qualifiedClassLockExpression) => qualifiedClassLockExpression 
	| (qualifiedThisExpression) => qualifiedThisExpression
	| qualifiedFieldRef 
	;

qualifiedClassLockExpression
	: namedType '.' CLASS -> ^(QualifiedClassLockExpression namedType)
	;

simpleLockExpression
	: classExpr 
	| thisExpr
	| simpleFieldRef
	;
	

/*************************************************************************************
 * Effects rules
 *************************************************************************************/	
aggregate
    	: mappedRegionSpecification EOF -> ^(Aggregate mappedRegionSpecification)
    	;
    	
region
	: fullRegionDecl -> fullRegionDecl
	| regionDecl -> regionDecl
	;
	
regionDecl
  : /* FIX This next line shouldn't be necessary */
	  IDENTIFIER -> ^(NewRegionDeclaration IDENTIFIER)  
  | accessModifiers IDENTIFIER -> ^(NewRegionDeclaration accessModifiers IDENTIFIER)
  ;
	
fullRegionDecl
  /* FIX This next line shouldn't be necessary */
  : IDENTIFIER EXTENDS regionSpecification -> ^(NewRegionDeclaration IDENTIFIER regionSpecification)
  | accessModifiers IDENTIFIER EXTENDS regionSpecification -> ^(NewRegionDeclaration accessModifiers IDENTIFIER regionSpecification)
  ;
	
inRegion
	: regionSpecification EOF -> ^(InRegion regionSpecification)
	;

mapFields
    : fieldMappings EOF -> fieldMappings
   	;

mapRegion
   	: regionMapping EOF -> regionMapping
   	;

regionEffects
	: 'none' -> ^(RegionEffects)
	| readsEffect (';' writesEffect)? -> ^(RegionEffects readsEffect writesEffect?)
	| writesEffect (';' readsEffect)? -> ^(RegionEffects writesEffect readsEffect?)
	;


/************* Effects supporting rules **************************/

	
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

innerClassLockName
	: qualifiedThisExpression ':' IDENTIFIER -> ^(QualifiedLockName qualifiedThisExpression IDENTIFIER)
	;

typeQualifiedLockName
	: typeExpression ':' IDENTIFIER -> ^(QualifiedLockName typeExpression IDENTIFIER)
	;

simpleLockName
	: IDENTIFIER -> ^(SimpleLockName IDENTIFIER)
	;
	
regionName
	: IDENTIFIER -> ^(RegionName IDENTIFIER)
	;
	
mappedRegionSpecification
	: regionMapping (',' regionMapping)* -> ^(MappedRegionSpecification regionMapping+)
	;
	
fieldMappings
	: regionSpecifications 'into' regionSpecification -> ^(FieldMappings regionSpecifications regionSpecification)
	;

regionMapping
	: simpleRegionSpecification 'into' regionSpecification -> ^(RegionMapping simpleRegionSpecification regionSpecification)
	;
	
qualifiedRegionName
	: qualifiedNamedType ':' IDENTIFIER -> ^(QualifiedRegionName qualifiedNamedType IDENTIFIER)
	| simpleNamedType ':' IDENTIFIER -> ^(QualifiedRegionName simpleNamedType IDENTIFIER)
	;
	
effectsSpecification
	: NOTHING
	| effectSpecification (',' effectSpecification)* ->  effectSpecification+
	;

effectSpecification
	: simpleEffectExpression ':' simpleRegionSpecification -> ^(EffectSpecification simpleEffectExpression simpleRegionSpecification)
	| implicitThisEffectSpecification
	;

implicitThisEffectSpecification
	: regionName -> ^(EffectSpecification ^(ThisExpression THIS) regionName)
	;

simpleEffectExpression
	: anyInstanceExpression
	| qualifiedThisExpression
	| qualifiedNamedType -> ^(TypeExpression qualifiedNamedType)
	| simpleExpression
	;
	
anyInstanceExpression
	: ANY '(' namedType ')' -> ^(AnyInstanceExpression namedType)
	;
 
readsEffect
	:'reads' effectsSpecification -> ^(Reads effectsSpecification)
	;
writesEffect
	:'writes' effectsSpecification -> ^(Writes effectsSpecification)
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
	: typeExpression '.' IDENTIFIER -> ^(FieldRef typeExpression IDENTIFIER)
	| qualifiedThisExpression '.' IDENTIFIER -> ^(FieldRef qualifiedThisExpression IDENTIFIER)
	;

namedType
  	: simpleNamedType
  	| qualifiedNamedType  	 	
  	;
  	
qualifiedNamedType
	: qualifiedName -> ^(NamedType qualifiedName)
	;

typeName
  	: {isType("", SLAnnotationsParser.this.input.LT(1))}? IDENTIFIER
  	| qualifiedTypeName  	 	
  	;

qualifiedTypeName
	: IDENTIFIER ({isType($qualifiedTypeName.text, SLAnnotationsParser.this.input.LT(2))}? 
	              '.' IDENTIFIER)*	              
	;

simpleNamedType	
	: IDENTIFIER -> ^(NamedType IDENTIFIER)
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

classExpr 
    : CLASS -> ^(ImplicitClassLockExpression THIS)
    | (THIS '.' CLASS) -> ^(ImplicitClassLockExpression THIS)
    ; 
 	
thisExpr
    : THIS -> ^(ThisExpression THIS)
    ;
    
returnValue
    : RETURN -> ^(ReturnValueDeclaration RETURN)
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

qualifiedName
	: IDENTIFIER ( options {greedy=false;} : ('.' IDENTIFIER))+ 
	;
	
simpleName
     	: IDENTIFIER	
     	;	

/*************************************************************************************
 * Standard Java tokens
 *************************************************************************************/	

ABSTRACT : 'abstract';
BOOLEAN : 'boolean';
BREAK : 'break';
BYTE : 'byte';
CASE : 'case';
CATCH : 'catch';
CHAR : 'char';
CLASS : 'class';
CONST : 'const';
CONTINUE : 'continue';
DEFAULT : 'default';
DO : 'do';
DOUBLE : 'double';
ELSE : 'else';
EXTENDS : 'extends';
FALSE : 'false';
FINAL : 'final';
FINALLY : 'finally';
FLOAT : 'float';
FOR : 'for';
GOTO : 'goto';
IF : 'if';
IMPLEMENTS : 'implements';
IMPORT : 'import';
INSTANCEOF : 'instanceof';
INT : 'int';
INTERFACE : 'interface';
LONG : 'long';
NATIVE : 'native';
NEW : 'new';
NULL : 'null';
ONLY : 'only';
PACKAGE : 'package';
PRIVATE : 'private';
PROTECTED : 'protected';
PUBLIC : 'public';
RETURN : 'return';
SHORT : 'short';
STATIC : 'static';
SUPER : 'super';
SWITCH : 'switch';
SYNCHRONIZED : 'synchronized';
THIS : 'this';
THROW : 'throw';
THROWS : 'throws';
TRANSIENT : 'transient';
TRUE : 'true';
TRY : 'try';
VOID : 'void';
VOLATILE : 'volatile';
WHILE : 'while';

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
DASH : '-';
LANGLE : '<';
RANGLE : '>';
EQUALS : '=';
POUND : '#';

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

IDENTIFIER 
    : LETTER (LETTER|JavaIDDigit)*
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
