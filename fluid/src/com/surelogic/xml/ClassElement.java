package com.surelogic.xml;

import java.util.*;

import com.surelogic.annotation.parse.AnnotationVisitor;
import com.surelogic.common.CommonImages;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
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
	
	public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}
	
	@Override
	public Operator getOperator() {
		return ClassDeclaration.prototype;
	}
	
	public final String getImageKey() {
		return CommonImages.IMG_CLASS;
	}
	
	public IClassMember addMember(IClassMember m) {
		if (m == null) {
			return null;
		}
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
	
	public MethodElement findMethod(String name, String params) {
		return methods.get(name, params);
	}
	
	public ConstructorElement findConstructor(String params) {
		return constructors.get(params);
	}
	
	ClassInitElement getClassInit() {
		return clinit;
	}
	
	Iteratable<NestedClassElement> getNestedClasses() {
		return PromisesXMLWriter.getSortedValues(classes);
	}

	public Iteratable<ConstructorElement> getConstructors() {
		return PromisesXMLWriter.getSortedValues(constructors);
	}
	
	Iteratable<FieldElement> getFields() {
		return PromisesXMLWriter.getSortedValues(fields);
	}
	
	public Collection<MethodElement> getMethods() {
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
	
	@Override
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
	
	@Override
	public boolean isModified() {
		if (super.isModified()) {
			return true;
		}
		if (clinit != null && clinit.isModified()) {
			return true;
		}
		for(FieldElement f : fields.values()) {
			if (f.isModified()) {
				return true;
			}
		}
		for(ConstructorElement c : constructors.values()) {
			if (c.isModified()) {
				return true;
			}
		}
		for(MethodElement m : methods.elements()) {
			if (m.isModified()) {
				return true;
			}
		}
		for(NestedClassElement n : classes.values()) {
			if (n.isModified()) {
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

	public MergeResult<ClassElement> merge(ClassElement changed, MergeType type) {
		if (getName().equals(changed.getName())) {
			boolean modified = false;
			
			if (clinit != null) {
				modified |= clinit.merge(changed.clinit, type);
			} else if (changed.clinit != null) {
				clinit = changed.clinit.cloneMe(this);
				modified = true;
			}
			for(FieldElement f2 : changed.fields.values()) {
				FieldElement f0 = fields.get(f2.getName());
				if (f0 != null) {
					modified |= f0.merge(f2, type);
				} else {
					addMember(f2.cloneMe(this));
					modified = true;
				}
			}
			for(ConstructorElement c2 : changed.constructors.values()) {
				ConstructorElement c0 = constructors.get(c2.getParams());
				if (c0 != null) {
					modified |= c0.merge(c2, type);
				} else {
					addMember(c2.cloneMe(this));
					modified = true;
				}
			}
			for(Pair<String,String> keys : changed.methods.keys()) {
				MethodElement m2 = changed.methods.get(keys.first(), keys.second());
				MethodElement m0 = methods.get(keys.first(), keys.second());
				if (m0 != null) {
					modified |= m0.merge(m2, type);
				} else {
					addMember(m2.cloneMe(this));
					modified = true;
				}
			}
			for(NestedClassElement n2 : changed.classes.values()) {
				NestedClassElement n0 = classes.get(n2.getName());
				if (n0 != null) {
					final MergeResult<?> r = n0.merge(n2, type);
					modified |= r.isModified;
				} else {
					addMember(n2.cloneMe(this));
					modified = true;
				}
			}
			modified |= mergeThis(changed, type);
			return new MergeResult<ClassElement>(this, modified);
		}
		return MergeResult.nullResult();
	}
	
	void copyToClone(ClassElement clone) {
		super.copyToClone(clone);
		if (clinit != null) {
			clone.addMember(clinit.cloneMe(clone));
		}
		for(FieldElement f : fields.values()) {
			clone.addMember(f.cloneMe(clone));
		}
		for(ConstructorElement c : constructors.values()) {
			clone.addMember(c.cloneMe(clone));
		}
		for(MethodElement m : methods.elements()) {
			clone.addMember(m.cloneMe(clone));
		}
		for(NestedClassElement n : classes.values()) {
			clone.addMember(n.cloneMe(clone));
		}
	}
	
	@Override
	ClassElement cloneMe(IJavaElement parent) {
		ClassElement e = new ClassElement(getName());
		copyToClone(e);
		return e;
	}

	ClassElement copyIfDirty() {
		if (isDirty()) {
			ClassElement e = new ClassElement(getName());
			copyIfDirty(e);
			return e;
		}
		return null;
	}
	
	void copyIfDirty(ClassElement clone) {
		super.copyIfDirty(clone);
		if (clinit != null) {
			clone.addMember(clinit.copyIfDirty());
		}
		for(FieldElement f : fields.values()) {
			clone.addMember(f.copyIfDirty());
		}
		for(ConstructorElement c : constructors.values()) {
			clone.addMember(c.copyIfDirty());
		}
		for(MethodElement m : methods.elements()) {
			clone.addMember(m.copyIfDirty());
		}
		for(NestedClassElement n : classes.values()) {
			clone.addMember(n.copyIfDirty());
		}
	}
	
	/**
	 * @return The number of annotations added
	 */
	@Override
	int applyPromises(final AnnotationVisitor v, final IRNode cuOrType) {
		if (cuOrType == null) {
			return 0;
		}
		final IRNode t = findType(cuOrType, getName());
		if (t == null) {			
			return 0;
		}
		int added = super.applyPromises(v, t);
		if (clinit != null) {
			added += clinit.applyPromises(v, JavaPromise.getClassInitOrNull(t));
		}
		for(FieldElement f : fields.values()) {
			added += f.applyPromises(v, TreeAccessor.findField(f.getName(), t));
		}
		for(ConstructorElement c : constructors.values()) {
			added += c.applyPromises(v, TreeAccessor.findConstructor(c.getParams(), t, v.getTypeEnv()));
		}
		for(MethodElement m : methods.elements()) {
			added += m.applyPromises(v, TreeAccessor.findMethod(t, m.getName(), m.getParams(), v.getTypeEnv()));
		}
		for(NestedClassElement n : classes.values()) {
			added += n.applyPromises(v, TreeAccessor.findNestedClass(n.getName(), t));
		}
		return added;
	}

	private static IRNode findType(final IRNode cuOrType, final String name) {
		final Operator op = JJNode.tree.getOperator(cuOrType);
		if (TypeDeclaration.prototype.includes(op)) {
			final String tName = JJNode.getInfo(cuOrType);
			if (!tName.equals(name)) {
				throw new IllegalStateException("Got a type with different name: "+tName+" vs "+name);
			}
			return cuOrType;
		} else {
			for(IRNode t : VisitUtil.getTypeDecls(cuOrType)) {
				if (JJNode.getInfo(t).equals(name)) {
					return t;
				}
			}
		}
		return null;
	}
}
