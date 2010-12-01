/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.persistence;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.common.xml.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.sea.*;

public abstract class AbstractAnalysisResult implements IAnalysisResult, PersistenceConstants {
	private final PromiseRef about;
	private final IRNode location; // TODO how to specify within a CU 
	
	public <T extends IAASTRootNode> AbstractAnalysisResult(PromiseDrop<T> d, IRNode loc) {
		about = new PromiseRef(d);
		location = loc;
	}
	
	public String outputToXML(JSureResultsXMLCreator creator, int indent, StringBuilder sb) {
		Entities.start(RESULT, sb, indent);
		attributesToXML(indent+1, sb);
		Entities.closeStart(sb, false);
		about.toXML(indent+1, sb, ABOUT_REF);

		ISrcRef s = JavaNode.getSrcRef(location);
		if (location == null) {
			throw new IllegalArgumentException("No src ref for "+DebugUnparser.toString(location));
		}		
		Entities.indent(sb, indent+1);
		creator.addSrcRef(location, s, "", null);
		
		subEntitiesToXML(indent+1, sb);
		Entities.end(RESULT, sb, indent);
		return sb.toString();
	}
	
	protected void attributesToXML(int indent, StringBuilder sb) {
		// Nothing right now
	}
	protected void subEntitiesToXML(int indent, StringBuilder sb) {
		// Nothing right now
	}
}
