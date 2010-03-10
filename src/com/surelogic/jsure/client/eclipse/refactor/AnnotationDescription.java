package com.surelogic.jsure.client.eclipse.refactor;

import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;

/**
 * Describes an annotation that should be placed at a given target.
 * 
 * @author nathan
 * 
 */
public class AnnotationDescription implements Comparable<AnnotationDescription> {

	private final Object target;
	private final String annotation;
	private final String contents;

	AnnotationDescription(final ProposedPromiseDrop drop) {
		target = null;// TODO
		annotation = drop.getAnnotation();
		contents = drop.getContents();
	}

	public Object getTarget() {
		return target;
	}

	public String getAnnotation() {
		return annotation;
	}

	public String getContents() {
		return contents;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (annotation == null ? 0 : annotation.hashCode());
		result = prime * result + (contents == null ? 0 : contents.hashCode());
		result = prime * result + (target == null ? 0 : target.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final AnnotationDescription other = (AnnotationDescription) obj;
		if (annotation == null) {
			if (other.annotation != null) {
				return false;
			}
		} else if (!annotation.equals(other.annotation)) {
			return false;
		}
		if (contents == null) {
			if (other.contents != null) {
				return false;
			}
		} else if (!contents.equals(other.contents)) {
			return false;
		}
		if (target == null) {
			if (other.target != null) {
				return false;
			}
		} else if (!target.equals(other.target)) {
			return false;
		}
		return true;
	}

	public int compareTo(final AnnotationDescription o) {
		return annotation.compareTo(o.annotation);
	}

	public boolean hasContents() {
		return contents != null;
	}

}
