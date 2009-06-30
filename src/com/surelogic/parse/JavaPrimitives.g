parser grammar JavaPrimitives; 
options{
  output=AST;
  tokenVocab=JavaToken;
  TokenLabelType=Token;
  ASTLabelType=Tree;
}
tokens {
  Tokens;
  VarDecl;
  NamedType;
  TypeRef;
  ArrayType;
  Annotations;
  MarkerAnnotation;
  ThisExpression;
  ReturnValueDeclaration;
  Nothing;
}
@header{
package com.surelogic.parse;
}  

borrowed
    : thisExpr
    ;
    
unique
    : thisExpr | returnValue | nothing
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

fieldDecl
    : annotations modifiers returnType IDENTIFIER -> ^(VarDecl annotations modifiers returnType IDENTIFIER)
    ;

annotations
    : annotation* -> ^(Annotations annotation*)
    ;

annotation
    : AT IDENTIFIER -> ^(MarkerAnnotation IDENTIFIER)
    ;

modifiers
    : (PUBLIC | PROTECTED | PRIVATE)? STATIC? FINAL?
    ;

returnType
    : VOID | type
    ;

type
    : arrayType | nonArrayType
    ;

nonArrayType
    : primType 
    | typeRef
    | namedType
    ;

arrayType
    : nonArrayType (LBRACKET RBRACKET)+ -> ^(ArrayType nonArrayType RBRACKET+)
    ;

primType
    : BOOLEAN | BYTE | CHAR | SHORT | INT | LONG | FLOAT | DOUBLE
    ;    

typeRef
    : namedType DOT IDENTIFIER typeRefExt -> ^(TypeRef namedType IDENTIFIER typeRefExt)
    ;

typeRefExt 
    : (DOT IDENTIFIER)*
    ;

namedType
    : ( packageName COLON )? IDENTIFIER -> ^(NamedType packageName DOT IDENTIFIER)
    ;
  
packageName
    : qualifiedName 
    ;  
    
qualifiedName
	  : IDENTIFIER ( DOT IDENTIFIER )*
	  ;

simpleName
    : IDENTIFIER
    ;