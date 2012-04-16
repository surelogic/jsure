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
	ImplicitQualifier;
	ProhibitsLock;
	RequiresLock;
	ReturnsLock;
	InRegion;
	UniqueMapping;
	UniqueInRegion;
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
  MethodCall;
  GuardedBy;
  Itself;
  AnnoParameters;
  ExplicitBorrowedInRegion;
  SimpleBorrowedInRegion;
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
	//MAPREGION='@MapRegion';
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
  ITSELF='itself';
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
    for(int i=0; i<DFA27_transition.length; i++) {
      DFA27_transition[i] = null;
    }
  }
  public static synchronized void init() {
    if (initialized) {
      return;
    }
    for (int i=0; i<DFA18_transition.length; i++) {
      DFA18_transition[i] = DFA.unpackEncodedString(DFA18_transitionS[i]);
    }
    for (int i=0; i<DFA27_transition.length; i++) {
      DFA27_transition[i] = DFA.unpackEncodedString(DFA27_transitionS[i]);
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
  : 'is' identifier '&' identifier ':' -> ^(TestResult identifier '&' identifier ':')
  | 'is' identifier ':' -> ^(TestResult identifier ':')
  | 'is' identifier '&' identifier EOF -> ^(TestResult identifier '&' identifier)
  | 'is' identifier EOF -> ^(TestResult identifier)
  ;
  
testResultComment
  : ('/*' | '/**') 
    testResult -> testResult
  ;
	
/*************************************************************************************
 * Parameters for annotations
 *************************************************************************************/ 
 
annoParameters
  : annoParameter (',' annoParameter)* -> ^(AnnoParameters annoParameter+)
  ;
    
annoParameter
  : 'implementationOnly' '=' TRUE -> 'implementationOnly'
  | 'implementationOnly' '=' FALSE -> FALSE // Same as default
  | 'verify' '=' TRUE -> TRUE               // Same as default
  | 'verify' '=' FALSE -> 'verify'
  ;

/*************************************************************************************
 * Uniqueness rules
 *************************************************************************************/	
	
borrowedType
    : qualifiedThisExpression EOF -> qualifiedThisExpression 
	  ;
	  
borrowedFunction
    : thisExpr EOF -> thisExpr
    | qualifiedThisExpression EOF -> qualifiedThisExpression
    /*
    | thisExpr ',' qualifiedThisExpression EOF -> ^(Expressions thisExpr qualifiedThisExpression)
    | qualifiedThisExpression ',' thisExpr EOF -> ^(Expressions thisExpr qualifiedThisExpression)    
    */
    | borrowedFuncExprList EOF -> borrowedFuncExprList
    ;

borrowedFuncExprList
    : borrowedFuncExpr (',' borrowedFuncExpr)+ -> ^(Expressions borrowedFuncExpr+)
    ;
    
borrowedFuncExpr
    : thisExpr | qualifiedThisExpression
    ;

borrowedList
    : borrowedExpressionList EOF -> borrowedExpressionList
   	| borrowedExpression EOF -> borrowedExpression
    ;

borrowedExpressionList
	  : borrowedExpression (',' borrowedExpression)+ -> ^(Expressions borrowedExpression+)
    ;

borrowedExpression
    : varUse | thisExpr | qualifiedThisExpression
    ;    

borrowedNestedType
    : qualifiedThisExpression
    ;

// TODO
borrowedAllowReturn
    : 'allowReturn' '=' TRUE -> 'allowReturn'
    | 'allowReturn' '=' FALSE -> FALSE // Same as default
    ;

uniqueAllowRead
    : 'allowRead' '=' TRUE -> 'allowRead'
    | 'allowRead' '=' FALSE -> FALSE // Same as default
    ;
    
uniqueJava5Constructor
    : returnValue EOF -> returnValue
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
    : varUse | returnValue
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
	:identifier 'is' lockExpression PROTECTS regionName EOF -> ^(LockDeclaration identifier lockExpression regionName)
	;

requiresLock
	: lockSpecifications -> ^(RequiresLock lockSpecifications)
	| -> ^(RequiresLock)
	;
	
prohibitsLock
	: lockSpecifications -> ^(ProhibitsLock lockSpecifications)
	| -> ^(ProhibitsLock)
	;
	
isLock
	: simpleLockSpecification EOF -> ^(IsLock simpleLockSpecification)
	;

policyLock
	: policyLockDeclaration EOF -> policyLockDeclaration
	;

returnsLock
    	: rawLockSpecification EOF -> ^(ReturnsLock rawLockSpecification)
    	;
    	
/************* Locking supporting rules **************************/

policyLockDeclaration
	: identifier 'is' lockExpression EOF -> ^(PolicyLockDeclaration identifier lockExpression)
	;

lockSpecifications
	: lockSpecification (',' lockSpecification)* -> lockSpecification+
	;

rawLockSpecification
  : qualifiedLockName
  | simpleLockName
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
  : simpleExpression ':' identifier -> ^(QualifiedLockName simpleExpression identifier)  
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

    	
region
	: fullRegionDecl -> fullRegionDecl
	| regionDecl -> regionDecl
	;
	
regionDecl
  : accessModifiers identifier -> ^(NewRegionDeclaration accessModifiers identifier)
  ;
	
fullRegionDecl
  : accessModifiers identifier EXTENDS regionSpecification -> ^(NewRegionDeclaration accessModifiers identifier regionSpecification)
  ;
	
inRegion
	: regionSpecification EOF -> ^(InRegion regionSpecification)
	;

uniqueInRegion
  : mappedRegionSpecification EOF -> ^(UniqueMapping mappedRegionSpecification)
	| regionSpecification EOF -> ^(UniqueInRegion regionSpecification)
	;
	
borrowedInRegion
  : mappedRegionSpecification EOF -> ^(ExplicitBorrowedInRegion mappedRegionSpecification)
  | regionSpecification EOF -> ^(SimpleBorrowedInRegion regionSpecification)
;
	
mapFields
    : fieldMappings EOF -> fieldMappings
   	;

mapRegion
   	: regionMapping EOF -> regionMapping
   	;

regionEffects
	: 'none' EOF -> ^(RegionEffects)
	| readsEffect (';' writesEffect)? EOF -> ^(RegionEffects readsEffect writesEffect?)
	| writesEffect (';' readsEffect)? EOF -> ^(RegionEffects writesEffect readsEffect?)
	;


/************* Effects supporting rules **************************/

	
regionNames
	: regionName (',' regionName)* -> ^(RegionSpecifications regionName+)
	;

regionSpecification
	: qualifiedRegionName 
	| simpleRegionSpecification
	;
	
simpleRegionSpecification	
	: regionName
//	| (LBRACKET RBRACKET) -> ^(RegionName LBRACKET RBRACKET)
	;

innerClassLockName
	: qualifiedThisExpression ':' identifier -> ^(QualifiedLockName qualifiedThisExpression identifier)
	;

typeQualifiedLockName
	: typeExpression ':' identifier -> ^(QualifiedLockName typeExpression identifier)
	;

simpleLockName
	: identifier -> ^(SimpleLockName identifier)
	;
	
regionName
	: identifier -> ^(RegionName identifier)
	;
	
mappedRegionSpecification
	: regionMapping (',' regionMapping)* -> ^(MappedRegionSpecification regionMapping+)
	;
	
fieldMappings
	: regionNames 'into' regionSpecification -> ^(FieldMappings regionNames regionSpecification)
	;

regionMapping
	: simpleRegionSpecification 'into' regionSpecification -> ^(RegionMapping simpleRegionSpecification regionSpecification)
	;
	
qualifiedRegionName
	: qualifiedNamedType ':' identifier -> ^(QualifiedRegionName qualifiedNamedType identifier)
	| simpleNamedType ':' identifier -> ^(QualifiedRegionName simpleNamedType identifier)
	;
	
effectsSpecification
	: NOTHING
	| effectSpecification (',' effectSpecification)* ->  effectSpecification+
	;

effectSpecification
	: simpleEffectExpression ':' simpleRegionSpecification -> ^(EffectSpecification simpleEffectExpression simpleRegionSpecification)
	| implicitQualifierEffectSpecification
	;

implicitQualifierEffectSpecification
  : regionName -> ^(EffectSpecification ^(ImplicitQualifier) regionName)
  ;
  
simpleEffectExpression
	: anyInstanceExpression
	| qualifiedThisExpression
	| qualifiedNamedType -> ^(TypeExpression qualifiedNamedType)
	| simpleExpression
	;
	
anyInstanceExpression
  : ANY '(' ')' -> ^(AnyInstanceExpression ^(NamedType))
	| ANY '(' namedType ')' -> ^(AnyInstanceExpression namedType)
	;
 
readsEffect
	:'reads' effectsSpecification -> ^(Reads effectsSpecification)
	;
writesEffect
	:'writes' effectsSpecification -> ^(Writes effectsSpecification)
	;

/*************************************************************************************
 * @GuardedBy
 *************************************************************************************/ 

guardedBy
  : guardedBySpec EOF -> ^(GuardedBy guardedBySpec)
  ;
  
guardedBySpec
  : thisExpr
  | qualifiedThisExpression
  | ITSELF -> ^(Itself ITSELF)
  | simpleFieldRef
  | staticFieldRef  
  | qualifiedClassExpression
  | noArgsMethod
  ;

/*************************************************************************************
 * All Rules Supporting rules
 *************************************************************************************/	

noArgsMethod
  : identifier '(' ')' -> ^(MethodCall ^(ThisExpression THIS) identifier ^(Parameters))
  ;

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
	: staticFieldRef
	| qualifiedThisExpression '.' identifier -> ^(FieldRef qualifiedThisExpression identifier)
	;

staticFieldRef
  : typeExpression '.' identifier -> ^(FieldRef typeExpression identifier)  
  ;

namedType
  	: simpleNamedType
  	| qualifiedNamedType  	 	
  	;
  	
qualifiedNamedType
	: qualifiedName -> ^(NamedType qualifiedName)
	;

typeName
  	: {isType("", SLAnnotationsParser.this.input.LT(1))}? identifier
  	| qualifiedTypeName  	 	
  	;

qualifiedTypeName
	: identifier ({isType($qualifiedTypeName.text, SLAnnotationsParser.this.input.LT(2))}? 
	              '.' identifier)*	              
	;

simpleNamedType	
	: identifier -> ^(NamedType identifier)
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
	: (PUBLIC | PROTECTED | PRIVATE | STATIC)*
	;

/*************************************************************************************
 * Typical syntax to be parsed 
 *************************************************************************************/	

qualifiedName
	: identifier ( options {greedy=false;} : ('.' identifier))+ 
	;
	
simpleName
     	: identifier	
     	;	

identifier
      : IDENTIFIER | 'is' | 'protects' | 
        'none' | 'reads' | 'writes' | 'any' | 'readLock' | 'writeLock' |
        'into' | 'nothing' | 'itself' |
        'implementationOnly' | 'verify' | 'allowReturn' | 'allowRead'
      ;

IMPLEMENTATION_ONLY : 'implementationOnly';
VERIFY : 'verify';

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
