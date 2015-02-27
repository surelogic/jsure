package com.surelogic.xml;

import java.util.List;

import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.IDecl;


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

	public final IDecl.Kind getKind() {
		return IDecl.Kind.PACKAGE;
	}
	
	@Override
  public <T> T visit(IJavaElementVisitor<T> v) {
		return v.visit(this);
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

//	@Override
//	public Operator getOperator() {
//		return PackageDeclaration.prototype;
//	}

	@Override
  public final String getImageKey() {
		return CommonImages.IMG_PACKAGE;
	}

	public final ClassElement getClassElement() {
		return clazz;
	}

	@Override
  public String getLabel() {
		return getName();
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

	@Override
  public void markAsClean() {
		super.markAsClean();
		if (clazz != null) {
			clazz.markAsClean();
		}
	}

	/**
	 * Performs a deep merge of this with the passed tree, this tree is mutated
	 * if the merge is successful. The trees must be of the same package or type
	 * or the merge is not performed and an exception is thrown.
	 * 
	 * @param other
	 *            the tree to merge with.
	 * @param typeOfMerge
	 *            the type of merge to perform.
	 * @throws IllegalArgumentException
	 *             if the passed tree is not of the same package or type.
	 */
	void mergeDeep(PackageElement other, MergeType typeOfMerge) {
		if (!other.getName().equals(getName()))
			throw new IllegalArgumentException(I18N.err(240, getName(),
					other.getName()));

		if (clazz == null && other.clazz == null) {
			// neither has a class, so they're both 'package-info.java' files
			mergeThis(other, typeOfMerge);
			return; // done
		} else if (clazz != null && other.clazz != null) {
			final MergeResult<ClassElement> r = clazz.merge(other.clazz,
					typeOfMerge);
			if (r.element != null) {
				// Class merged, so continue merging
				mergeThis(other, typeOfMerge);
				return; // done
			} else
				throw new IllegalArgumentException(I18N.err(242, getName(),
						other.getName()));
		} else
			throw new IllegalArgumentException(I18N.err(241, getName(),
					other.getName()));
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
}
