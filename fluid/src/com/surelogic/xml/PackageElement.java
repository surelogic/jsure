package com.surelogic.xml;

import java.util.List;

import com.surelogic.annotation.parse.AnnotationVisitor;
import com.surelogic.common.CommonImages;

import edu.cmu.cs.fluid.ir.IRNode;
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

	public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
	}
	
	public boolean needsToUpdate(PackageElement other) {
		if (other == null) {
			return false;
		}
		return this.revision > other.revision;
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
		if (clazz != null && clazz.isDirty()) {
			return true;
		}
		return super.isDirty();
	}
	
	public void markAsClean() {
		super.markAsClean();
		if (clazz != null) {
			clazz.markAsClean();
		}
	}

	/**
	 * This is effectively the root, so we start merging the whole tree here
	 * 
	 * @param updateToClient if true; merge to fluid otherwise
	 */
	PackageElement merge(PackageElement changed, boolean updateToClient) { 
		// Needs to sync if the versions are the same due to storing local diffs
		/*
		final boolean needsToSync = !updateToClient || this.revision < changed.revision;
		if (!needsToSync) {
			// Nothing to do, since I'm updating the client, and the revisions are the same
			return this;
		}
		*/
		final MergeType type = MergeType.get(updateToClient);
		
		if (changed.getName().equals(getName())) {
			if (clazz != null) {
				if (changed.clazz == null) {
					// One's a class, the other's a package
					return null;
				}			
				final MergeResult<ClassElement> r = clazz.merge(changed.clazz, type);
				if (r.element != null) {
					// Class merged, so continue merging
					boolean modified = r.isModified;
					modified |= mergeThis(changed, type);		
					if (modified) {
						revision++;
					}
					return this;
				}
			} else if (changed.clazz == null) {
				// neither has a class, so they're both package-info.java files
				boolean modified = mergeThis(changed, type);								
				if (modified) {
					revision++;
				}
				return this;
			}
		}
		return null;
	}	
	
	@Override
	PackageElement cloneMe() {
		PackageElement e = new PackageElement(getName(), revision, clazz.cloneMe());
		copyToClone(e);
		return e;
	}

	PackageElement copyIfDirty() {
		if (isDirty()) {
			ClassElement c = null;
			if (clazz != null) {
				c = clazz.copyIfDirty();
			}
			PackageElement p = new PackageElement(getName(), revision, c);
			copyIfDirty(p);
			return p;
		}
		return null;
	}

	/**
	 * @return The number of annotations added
	 */
	@Override
	int applyPromises(AnnotationVisitor v, IRNode cu) {
		int added = super.applyPromises(v, cu);
		if (clazz != null) {
			added += clazz.applyPromises(v, cu);
		}
		return added;
	}
}
