/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.persistence;

import com.surelogic.aast.IAASTRootNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public class SimpleAnalysisResult extends AbstractAnalysisResult {
	private final int messageCode;
	private final Object[] args;
	
	public <T extends IAASTRootNode> SimpleAnalysisResult(PromiseDrop<T> about, IRNode location, int code, Object... args) {
		super(about, location);
		messageCode = code;
		this.args = args;
	}
}
