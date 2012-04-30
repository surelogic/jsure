package com.surelogic.xml;

import java.util.List;

import com.surelogic.annotation.parse.AnnotationVisitor;
import com.surelogic.common.CommonImages;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.PackageDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

public class PackageElement extends AnnotatedJavaElement {
	private final ClassElement clazz;
	private int f_releaseVersion;

	public PackageElement(boolean confirmed, String id, int releaseVersion,
			ClassElement c) {
		super(confirmed, id, Access.PUBLIC);
		f_releaseVersion = releaseVersion;
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
		return this.f_releaseVersion > other.f_releaseVersion;
	}

	@Override
	public PackageElement getRootParent() {
		return this;
	}

	@Override
	public int getReleaseVersion() {
		return f_releaseVersion;
	}

	@Override
	public void setReleaseVersion(int value) {
		f_releaseVersion = value;
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
		return "package " + getName();
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

	@Override
	public boolean isDirty() {
		if (clazz != null && clazz.isDirty()) {
			return true;
		}
		return super.isDirty();
	}

	@Override
	public boolean isModified() {
		if (clazz != null && clazz.isModified()) {
			return true;
		}
		return super.isModified();
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
	 * @param updateToClient
	 *            if true; merge to fluid otherwise
	 */
	PackageElement merge(PackageElement changed, boolean updateToClient) {
		// Needs to sync if the versions are the same due to storing local diffs
		/*
		 * final boolean needsToSync = !updateToClient || this.revision <
		 * changed.revision; if (!needsToSync) { // Nothing to do, since I'm
		 * updating the client, and the revisions are the same return this; }
		 */
		final MergeType type = updateToClient ? MergeType.JSURE_TO_LOCAL
				: MergeType.LOCAL_TO_JSURE;

		if (changed.getName().equals(getName())) {
			if (clazz != null) {
				if (changed.clazz == null) {
					// One's a class, the other's a package
					return null;
				}
				final MergeResult<ClassElement> r = clazz.merge(changed.clazz,
						type);
				if (r.element != null) {
					// Class merged, so continue merging
					boolean modified = r.isModified;
					modified |= mergeThis(changed, type);
					return this;
				}
			} else if (changed.clazz == null) {
				// neither has a class, so they're both package-info.java files
				boolean modified = mergeThis(changed, type);
				return this;
			}
		}
		return null;
	}

	@Override
	public PackageElement cloneMe(IJavaElement parent) {
		ClassElement cl = null;
		if (clazz != null) {
			cl = clazz.cloneMe(null);
		}
		PackageElement e = new PackageElement(isConfirmed(), getName(),
				f_releaseVersion, cl);
		copyToClone(e);
		return e;
	}

	PackageElement copyIfDirty() {
		if (isDirty()) {
			ClassElement c = null;
			if (clazz != null) {
				c = clazz.copyIfDirty();
			}
			PackageElement p = new PackageElement(isConfirmed(), getName(),
					f_releaseVersion, c);
			copyIfDirty(p);
			return p;
		}
		return null;
	}

	/**
	 * @return The number of annotations added
	 */
	@Override
	int applyPromises(AnnotationVisitor v, final IRNode cu) {
		final IRNode pkg = CompilationUnit.getPkg(cu);
		int added = super.applyPromises(v, pkg);
		if (clazz != null) {
			added += clazz.applyPromises(v, cu);
		}
		return added;
	}
}
