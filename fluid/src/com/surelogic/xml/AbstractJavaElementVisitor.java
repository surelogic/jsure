package com.surelogic.xml;

public abstract class AbstractJavaElementVisitor<T> implements IJavaElementVisitor<T> {
	protected final T defaultValue;
	
	protected AbstractJavaElementVisitor(T dv) {
		defaultValue = dv;
	}
	
	protected abstract T combine(T old, T result);
	
	protected <E extends IJavaElement> T visitAll(Iterable<E> elts) {
		T result = defaultValue;
		for(E e : elts) {
			if (e != null) {
				result = combine(result, e.visit(this));
			}
		}
		return result;
	}
	
	protected T visitAnnotated(AnnotatedJavaElement elt) {
		T result = defaultValue;//visitCommented(elt);
		return combine(result, visitAll(elt.getPromises(true)));
	}
	
	public T visit(PackageElement packageElt) {
		T result = visit(packageElt.getClassElement());
		result = combine(result, visitAnnotated(packageElt));
		return result;
	}

	public T visit(ClassElement classElt) {
		T result = defaultValue;
		if (classElt == null) {
			return defaultValue;
		}
		if (classElt.getClassInit() != null) {
			result = combine(result, classElt.getClassInit().visit(this));
		}
		result = combine(result, visitAll(classElt.getFields()));
		result = combine(result, visitAll(classElt.getMethods()));
		result = combine(result, visitAll(classElt.getConstructors()));
		result = combine(result, visitAll(classElt.getNestedClasses()));
		return combine(result, visitAnnotated(classElt));
	}

	public T visit(NestedClassElement e) {
		return visit((ClassElement) e);
	}
	
	public T visit(MethodElement methodElt) {
		return visitFunc(methodElt);	
	}
	
	public T visit(ConstructorElement constructorElt) {
		return visitFunc(constructorElt);		
	}
	
	protected T visitFunc(AbstractFunctionElement elt) {
		T result = visitAll(elt.getParameters());
		return combine(result, visitAnnotated(elt));
	}

	public T visit(FunctionParameterElement functionParameterElt) {
		return visitAnnotated(functionParameterElt);
	}

	public T visit(FieldElement fieldElt) {
		return visitAnnotated(fieldElt);
	}

	public T visit(ClassInitElement classInitElt) {
		return visitAnnotated(classInitElt);
	}

	public T visit(AnnotationElement annotationElt) {
		return defaultValue;//visitCommented(annotationElt);
	}
}
