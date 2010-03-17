package com.surelogic.jsure.client.eclipse.refactor;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;

/**
 * Describes an annotation that should be placed at a given target.
 * 
 * @author nathan
 * 
 */
class AnnotationDescription implements Comparable<AnnotationDescription> {

	private final String annotation;
	private final String contents;
	private final IJavaDeclaration target;
	private final IJavaDeclaration assumptionTarget;
	private final CU cu;
	private final CU assumptionCU;

	AnnotationDescription(final String annotation, final String contents) {
		this(annotation, contents, null, null, null, null);
	}

	public AnnotationDescription(final String annotation,
			final String contents, final IJavaDeclaration target,
			final IJavaDeclaration assumptionTarget, final CU cu,
			final CU assumptionCU) {
		if (annotation == null) {
			throw new IllegalArgumentException(
					"The annotation must always be specified.");
		}
		this.target = target;
		this.assumptionTarget = assumptionTarget;
		this.annotation = annotation;
		this.contents = contents;
		this.cu = cu;
		this.assumptionCU = assumptionCU;
	}

	public AnnotationDescription(final ProposedPromiseDrop drop, final IBinder b) {
		target = IRNodeUtil.convert(b, drop.getNode());
		assumptionTarget = IRNodeUtil.convert(b, drop.getAssumptionNode());
		annotation = drop.getAnnotation();
		contents = drop.getContents();
		ISrcRef srcRef = drop.getSrcRef();
		cu = new CU(srcRef.getPackage(), srcRef.getCUName());
		srcRef = drop.getAssumptionRef();
		assumptionCU = new CU(srcRef.getPackage(), srcRef.getCUName());
	}

	public IJavaDeclaration getTarget() {
		return target;
	}

	public IJavaDeclaration getAssumptionTarget() {
		return assumptionTarget;
	}

	public String getAnnotation() {
		return annotation;
	}

	public String getContents() {
		return contents;
	}

	@Override
	public String toString() {
		return String.format("@%s(%s)", getAnnotation(), getContents());
	}

	public int compareTo(final AnnotationDescription o) {
		return getAnnotation().compareTo(o.getAnnotation());
	}

	public boolean hasContents() {
		return getContents() != null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (annotation == null ? 0 : annotation.hashCode());
		result = prime * result + (contents == null ? 0 : contents.hashCode());
		result = prime * result + (cu == null ? 0 : cu.hashCode());
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
		if (cu == null) {
			if (other.cu != null) {
				return false;
			}
		} else if (!cu.equals(other.cu)) {
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

	public CU getCU() {
		return cu;
	}

	public CU getAssumptionCU() {
		return assumptionCU;
	}

	public static class CU {
		final String cu;
		final String pakkage;

		CU(final String pakkage, final String cu) {
			this.cu = cu;
			this.pakkage = pakkage;
		}

		public String getCu() {
			return cu;
		}

		public String getPackage() {
			return pakkage;
		}

		@Override
		public String toString() {
			return pakkage + "." + cu;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (cu == null ? 0 : cu.hashCode());
			result = prime * result
					+ (pakkage == null ? 0 : pakkage.hashCode());
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
			final CU other = (CU) obj;
			if (cu == null) {
				if (other.cu != null) {
					return false;
				}
			} else if (!cu.equals(other.cu)) {
				return false;
			}
			if (pakkage == null) {
				if (other.pakkage != null) {
					return false;
				}
			} else if (!pakkage.equals(other.pakkage)) {
				return false;
			}
			return true;
		}
	}

}
