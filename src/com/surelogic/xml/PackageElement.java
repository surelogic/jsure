package com.surelogic.xml;

import java.util.List;

import com.surelogic.common.CommonImages;

import edu.cmu.cs.fluid.java.operator.PackageDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public class PackageElement extends AnnotatedJavaElement {
	private final ClassElement clazz;
	
	public PackageElement(String id, ClassElement c) {
		super(id);
		clazz = c;
		if (clazz != null) {
			c.setParent(this);
		}
	}	

	@Override
	public Operator getOperator() {
		return PackageDeclaration.prototype;
	}
	
	public final String getImageKey() {
		return CommonImages.IMG_PACKAGE;
	}
	
	public final ClassElement getClassElement() {
		return clazz;
	}

	public String getLabel() {
		return "package "+getName();
	}
	
	@Override
	public boolean hasChildren() {
		return super.hasChildren() || clazz != null;
	}
	
	@Override
	protected void collectOtherChildren(List<Object> children) {
		super.collectOtherChildren(children);
		if (clazz != null) {
			children.add(clazz);
		}
	}
	
	public boolean isDirty() {
		return clazz.isDirty() || super.isDirty();
	}
	
	public void markAsClean() {
		super.markAsClean();
		clazz.markAsClean();
	}

	/**
	 * This is effectively the root, so we start merging the whole tree here
	 */
	PackageElement merge(PackageElement changed) {
		if (changed.getName().equals(getName())) {
			ClassElement c;
			if (clazz != null) {
				if (changed.clazz == null) {
					// One's a class, the other's a package
					return null;
				}
				c = clazz.merge(changed.clazz);
				if (c != null) {
					// Class merged, so continue merging
					mergeThis(changed);				
					return this;
				}
			} else if (changed.clazz == null) {
				// neither has a class, so they're both package-info.java files
				mergeThis(changed);				
				return this;
			}
		}
		return null;
	}

	@Override
	PackageElement cloneMe() {
		PackageElement e = new PackageElement(getName(), clazz);
		copyToClone(e);
		return e;
	}
}
