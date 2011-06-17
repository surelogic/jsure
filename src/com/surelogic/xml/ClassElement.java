package com.surelogic.xml;

import java.util.*;

import com.surelogic.common.CommonImages;

import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

public class ClassElement extends AnnotatedJavaElement {
	private final Map<String,NestedClassElement> classes = new HashMap<String,NestedClassElement>(0);
	private final Map<String,FieldElement> fields = new HashMap<String,FieldElement>(0);
	private final Hashtable2<String,String,MethodElement> methods = new Hashtable2<String, String, MethodElement>();
	private final Map<String,ConstructorElement> constructors = new HashMap<String, ConstructorElement>(0);
	private ClassInitElement clinit;
	
	public ClassElement(String id) {
		super(id);
	}
	
	@Override
	public Operator getOperator() {
		return ClassDeclaration.prototype;
	}
	
	public final String getImageKey() {
		return CommonImages.IMG_CLASS;
	}
	
	public IClassMember addMember(IClassMember m) {
		m.setParent(this);
		if (m instanceof MethodElement) {
			MethodElement method = (MethodElement) m;
			return methods.put(m.getName(), method.getParams(), method);
		}
		else if (m instanceof ConstructorElement) {
			ConstructorElement method = (ConstructorElement) m;
			return constructors.put(method.getParams(), method);
		}
		else if (m instanceof NestedClassElement) {		
			return classes.put(m.getName(), (NestedClassElement) m);
		}
		else if (m instanceof FieldElement) {
			return fields.put(m.getName(), (FieldElement) m);
		}
		else if (m instanceof ClassInitElement) {
			try {
				return clinit;
			} finally {			
				clinit = (ClassInitElement) m;
			}
		}
		else {
			throw new IllegalArgumentException("Unexpected IClassMember: "+m);
		}
	}
	
	NestedClassElement findClass(String id) {
		return classes.get(id);
	}
	
	FieldElement findField(String key) {
		return fields.get(key);
	}
	
	MethodElement findMethod(String name, String params) {
		return methods.get(name, params);
	}
	
	ClassInitElement getClassInit() {
		return clinit;
	}
	
	Iteratable<NestedClassElement> getNestedClasses() {
		return PromisesXMLWriter.getSortedValues(classes);
	}

	Iteratable<ConstructorElement> getConstructors() {
		return PromisesXMLWriter.getSortedValues(constructors);
	}
	
	Iteratable<FieldElement> getFields() {
		return PromisesXMLWriter.getSortedValues(fields);
	}
	
	Collection<MethodElement> getMethods() {
		final List<MethodElement> elements = new ArrayList<MethodElement>(methods.size());
		for(Pair<String,String> key : methods.keys()) {
			elements.add(methods.get(key.first(), key.second()));
		}
		Collections.sort(elements, new Comparator<MethodElement>() {
			public int compare(MethodElement o1, MethodElement o2) {				
				int rv = o1.getName().compareTo(o2.getName());
				if (rv == 0) {
					rv = o1.getParams().compareTo(o2.getParams());
				}
				return rv;
			}
		});
		return elements;
	}

	public String getLabel() {
		return "type "+getName();
	}
	
	@Override
	public boolean hasChildren() {
		return !methods.isEmpty() || !constructors.isEmpty() || super.hasChildren() ||
		       !classes.isEmpty() || !fields.isEmpty() || clinit != null;
	}
	
	@Override
	protected void collectOtherChildren(List<Object> children) {
		super.collectOtherChildren(children);
		if (clinit != null) {
			children.add(clinit);
		}
		for(FieldElement f : getFields()) {
			children.add(f);
		}
		for(ConstructorElement c : getConstructors()) {
			children.add(c);
		}
		children.addAll(getMethods());
		for(NestedClassElement n : getNestedClasses()) {
			children.add(n);
		}
	}
	
	public boolean isDirty() {
		if (super.isDirty()) {
			return true;
		}
		if (clinit != null && clinit.isDirty()) {
			return true;
		}
		for(FieldElement f : fields.values()) {
			if (f.isDirty()) {
				return true;
			}
		}
		for(ConstructorElement c : constructors.values()) {
			if (c.isDirty()) {
				return true;
			}
		}
		for(MethodElement m : methods.elements()) {
			if (m.isDirty()) {
				return true;
			}
		}
		for(NestedClassElement n : classes.values()) {
			if (n.isDirty()) {
				return true;
			}
		}
		return false;
	}
	
	public void markAsClean() {
		super.markAsClean();
		if (clinit != null) {
			clinit.markAsClean();
		}
		for(FieldElement f : fields.values()) {
			f.markAsClean();
		}
		for(ConstructorElement c : constructors.values()) {
			c.markAsClean();
		}
		for(MethodElement m : methods.elements()) {
			m.markAsClean();
		}
		for(NestedClassElement n : classes.values()) {
			n.markAsClean();
		}
	}
}
