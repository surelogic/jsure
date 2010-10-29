package edu.cmu.cs.fluid.dcf.views.coe;


import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.sea.*;

/**
 * Class used to represent derived viewer nodes.
 */
public final class Content extends AbstractContent<Drop,Content> {
	/**
	 * The fAST node that this item references, or null, if the associated drop
	 * defines the reference location.
	 */
	private ISrcRef f_sourceRef = null;
	
	Content(String msg, Collection<Content> content, Drop drop) {
		super(msg, content, drop);
		if (drop instanceof IRReferenceDrop) {
			f_sourceRef = ((IRReferenceDrop) drop).getSrcRef();
		}
	}

	Content(String msg, Collection<Content> content) {
		this(msg, content, null);
	}

	Content(String msg) {
		this(msg, new HashSet<Content>(), null);
	}

	Content(String msg, Drop drop) {
		this(msg, new HashSet<Content>(), drop);
	}

	Content(String msg, IRNode location, Drop drop) {
		this(msg, new HashSet<Content>(), drop);
		if (location != null) {
			f_sourceRef = JavaNode.getSrcRef(location);
		}
	}

	Content(String msg, IRNode location) {
		this(msg, location, null);
	}

	public ISrcRef getSrcRef() {
		/*
		 * if (referencedDrop instanceof IRReferenceDrop) { return
		 * ((IRReferenceDrop) referencedDrop).getSrcRef(); } return
		 * (referencedLocation != null ? JavaNode .getSrcRef(referencedLocation)
		 * : null);
		 */
		return f_sourceRef;
	}
	
	public static Object[] filterNonInfo(Object[] items) {
		return AbstractContent.<Drop,Content>filterNonInfo(items);
	}
}