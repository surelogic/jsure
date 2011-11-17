package com.surelogic.xml;

public interface IJavaElementVisitor<T> {
	T visit(PackageElement packageElement);
	T visit(ClassElement classElement);
	T visit(NestedClassElement classElement);
	T visit(MethodElement methodElement);
	T visit(ConstructorElement constructorElement);
	T visit(FunctionParameterElement functionParameterElement);
	T visit(FieldElement fieldElement);
	T visit(ClassInitElement classInitElement);
	T visit(AnnotationElement annotationElement);
}
