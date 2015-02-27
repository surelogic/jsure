package com.surelogic.xml;

import java.util.*;
import java.util.Map.Entry;

import com.surelogic.common.CommonImages;
import com.surelogic.common.Pair;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.util.*;


public class ClassElement extends AnnotatedJavaElement {
	private final Map<String,NestedClassElement> classes = new HashMap<String,NestedClassElement>(0);
	private final Map<String,FieldElement> fields = new HashMap<String,FieldElement>(0);
	private final HashMap<Pair<String,String>,MethodElement> methods = new HashMap<Pair<String,String>, MethodElement>();
	private final Map<String,ConstructorElement> constructors = new HashMap<String, ConstructorElement>(0);
	private ClassInitElement clinit;
	
	public ClassElement(boolean confirmed, String id, Access access) {
		super(confirmed, id, access);
	}
	
	public final IDecl.Kind getKind() {
		return IDecl.Kind.CLASS;
	}
	
	@Override
  public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}
	
//	@Override
//	public Operator getOperator() {
//		return ClassDeclaration.prototype;
//	}
	
	@Override
  public final String getImageKey() {
		switch (getAccessibility()) {
		case PROTECTED:
			return CommonImages.IMG_CLASS_PROTECTED;
		case DEFAULT:
			return CommonImages.IMG_CLASS_DEFAULT;
		case PUBLIC:
		default:
			return CommonImages.IMG_CLASS;
		}
	}
	
	public IClassMember addMember(IClassMember m) {
		if (m == null) {
			return null;
		}
		m.setParent(this);
		if (m instanceof MethodElement) {
			MethodElement method = (MethodElement) m;
			return methods.put(Pair.getInstance(m.getName(), method.getParams()), method);
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
		return methods.get(Pair.getInstance(name, params));
	}
	
	public ConstructorElement findConstructor(String params) {
		return constructors.get(params);
	}
	
	public void removeMethod(MethodElement m) {
		methods.remove(Pair.getInstance(m.getName(), m.getParams()));
	}
	
	public void removeConstructor(ConstructorElement c) {
		constructors.remove(c.getParams());
	}
	
	public void removeClass(NestedClassElement n) {
		classes.remove(n.getName());
	}
	
	public ClassInitElement getClassInit() {
		return clinit;
	}
	
	public Iteratable<NestedClassElement> getNestedClasses() {
		return PromisesXMLWriter.getSortedValues(classes);
	}

	public Iteratable<ConstructorElement> getConstructors() {
		return PromisesXMLWriter.getSortedValues(constructors);
	}
	
	public Iteratable<FieldElement> getFields() {
		return PromisesXMLWriter.getSortedValues(fields);
	}
	
	public Collection<MethodElement> getMethods() {
		final List<MethodElement> elements = new ArrayList<MethodElement>(methods.values());
		Collections.sort(elements, new Comparator<MethodElement>() {
			@Override
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

	@Override
  public String getLabel() {
		return getName();
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
		for(MethodElement m : methods.values()) {
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
		for(MethodElement m : methods.values()) {
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
	
	@Override
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
		for(MethodElement m : methods.values()) {
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
      for (Entry<Pair<String,String>,MethodElement> entry : changed.methods.entrySet()) {
        MethodElement m2 = entry.getValue();
        MethodElement m0 = methods.get(entry.getKey());
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
		for(MethodElement m : methods.values()) {
			clone.addMember(m.cloneMe(clone));
		}
		for(NestedClassElement n : classes.values()) {
			clone.addMember(n.cloneMe(clone));
		}
	}
	
	@Override
	ClassElement cloneMe(IJavaElement parent) {
		ClassElement e = new ClassElement(isConfirmed(), getName(), getAccessibility());
		copyToClone(e);
		return e;
	}

	ClassElement copyIfDirty() {
		if (isDirty()) {
			ClassElement e = new ClassElement(isConfirmed(), getName(), getAccessibility());
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
		for(MethodElement m : methods.values()) {
			clone.addMember(m.copyIfDirty());
		}
		for(NestedClassElement n : classes.values()) {
			clone.addMember(n.copyIfDirty());
		}
	}
}
