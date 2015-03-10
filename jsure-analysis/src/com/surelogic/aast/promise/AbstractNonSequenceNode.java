package com.surelogic.aast.promise;

import com.surelogic.aast.*;

/**
 * An AAST that can only appear once on a given decl
 */
public abstract class AbstractNonSequenceNode extends AASTRootNode {
	protected AbstractNonSequenceNode(int offset) {
		super(offset);
	}

	@Override
	public final boolean needsConflictResolution() {
		return true;
	}
}
