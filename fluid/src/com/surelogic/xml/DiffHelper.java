package com.surelogic.xml;

/**
 * Helps to stash away state needed to check if we really changed things
 * 
 * @author Edwin
 */
class DiffHelper extends AbstractJavaElementVisitor<Void> {
	DiffHelper() {
		super(null);
	}

	@Override
	protected Void combine(Void old, Void result) {
		return null;
	}

	@Override
	public Void visit(AnnotationElement a) {
		a.stashDiffState(a);
		return null;
	}
}
