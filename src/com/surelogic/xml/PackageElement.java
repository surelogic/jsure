package com.surelogic.xml;

import java.util.List;

import com.surelogic.common.CommonImages;

import edu.cmu.cs.fluid.java.operator.PackageDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public class PackageElement extends AnnotatedJavaElement {
	private final ClassElement clazz;
	private int revision;
	
	public PackageElement(String id, int rev, ClassElement c) {
		super(id);
		revision = rev;
		clazz = c;
		if (clazz != null) {
			c.setParent(this);
		}
	}	

	int getRevision() {
		return revision;
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
	 * 
	 * @param updateToClient if true; merge to fluid otherwise
	 */
	PackageElement merge(PackageElement changed, boolean updateToClient) { 
		final boolean needsToSync = !updateToClient || this.revision > changed.revision;
		if (!needsToSync) {
			// Nothing to do, since I'm updating the client, and the revisions are the same
			return this;
		}
		final MergeType type = MergeType.get(updateToClient);
		
		if (changed.getName().equals(getName())) {
			ClassElement c;
			if (clazz != null) {
				if (changed.clazz == null) {
					// One's a class, the other's a package
					return null;
				}			
				c = clazz.merge(changed.clazz, type);
				if (c != null) {
					// Class merged, so continue merging
					mergeThis(changed, type);				
					return this;
				}
			} else if (changed.clazz == null) {
				// neither has a class, so they're both package-info.java files
				mergeThis(changed, type);								
				return this;
			}
		}
		return null;
	}	
	
	@Override
	PackageElement cloneMe() {
		PackageElement e = new PackageElement(getName(), revision, clazz);
		copyToClone(e);
		return e;
	}
}
