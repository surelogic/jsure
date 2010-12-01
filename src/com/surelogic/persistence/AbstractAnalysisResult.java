/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.persistence;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.common.xml.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.*;

public abstract class AbstractAnalysisResult implements IAnalysisResult, PersistenceConstants {
	private final PromiseRef about;
	private final IRNode location; // TODO how to specify within a CU 
	
	public <T extends IAASTRootNode> AbstractAnalysisResult(PromiseDrop<T> d, IRNode loc) {
		about = new PromiseRef(d);
		location = loc;
	}
	
	public String toXML(final int indent) {
		StringBuilder sb = new StringBuilder(indent);
		Entities.start("result", sb);
		attributesToXML(indent+1, sb);
		sb.append(">\n");
		about.toXML(indent+1, sb);
		//TODO location
		subEntitiesToXML(indent+1, sb);
		sb.append(indent).append("</result>\n");
		return sb.toString();
	}
	
	protected void attributesToXML(int indent, StringBuilder sb) {}
	protected void subEntitiesToXML(int indent, StringBuilder sb) {}
}
