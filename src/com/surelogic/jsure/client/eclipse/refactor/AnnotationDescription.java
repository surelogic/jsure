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
public class AnnotationDescription implements Comparable<AnnotationDescription> {

	private final IJavaDeclaration target;
	private final IJavaDeclaration assumptionTarget;
	private final ProposedPromiseDrop drop;

	AnnotationDescription(final ProposedPromiseDrop drop, final IBinder b) {
		this.drop = drop;
		target = IRNodeUtil.convert(b, drop.getNode());
		assumptionTarget = IRNodeUtil.convert(b, drop.getAssumptionNode());
	}

	public IJavaDeclaration getTarget() {
		return target;
	}

	public IJavaDeclaration getAssumptionTarget() {
		return assumptionTarget;
	}

	public String getAnnotation() {
		return drop.getAnnotation();
	}

	public String getContents() {
		return drop.getContents();
	}

	@Override
	public String toString() {
		return String.format("@%s(%s)", getAnnotation(), getContents());
	}

	public int compareTo(final AnnotationDescription o) {
		return drop.getAnnotation().compareTo(o.drop.getAnnotation());
	}

	public boolean hasContents() {
		return drop.getContents() != null;
	}

	public CU getCU() {
		final ISrcRef srcRef = drop.getSrcRef();
		return new CU(srcRef.getPackage(), srcRef.getCUName());
	}

	public CU getAssumptionCU() {
		final ISrcRef srcRef = drop.getAssumptionRef();
		return new CU(srcRef.getPackage(), srcRef.getCUName());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (assumptionTarget == null ? 0 : assumptionTarget.hashCode());
		result = prime * result + (drop == null ? 0 : drop.hashCode());
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
		if (assumptionTarget == null) {
			if (other.assumptionTarget != null) {
				return false;
			}
		} else if (!assumptionTarget.equals(other.assumptionTarget)) {
			return false;
		}
		if (drop == null) {
			if (other.drop != null) {
				return false;
			}
		} else if (!annotationMatches(other)) {
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

	private boolean annotationMatches(final AnnotationDescription other) {
		if (!getAnnotation().equals(other.getAnnotation())) {
			return false;
		}
		if (getContents() == null) {
			return other.getContents() == null;
		} else {
			return getContents().equals(other.getContents());
		}
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
