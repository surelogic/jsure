/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.persistence;

import java.util.*;

import org.xml.sax.Attributes;

import com.surelogic.analysis.*;
import com.surelogic.common.xml.*;
import com.surelogic.jsure.xml.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.*;
import edu.cmu.cs.fluid.sea.xml.SeaSummary;

public class JSureResultsXMLReader extends NestedXMLReader implements IXMLResultListener, PersistenceConstants {
	private final IIRProjects projects;
	private final Map<String,SourceCUDrop> cuds = new HashMap<String, SourceCUDrop>();

	public JSureResultsXMLReader(IIRProjects p) {
		projects = p;
		for(SourceCUDrop d : Sea.getDefault().getDropsOfExactType(SourceCUDrop.class)) {
			cuds.put(d.javaOSFileName, d);
		}
	}
	
	@Override
	protected String checkForRoot(String name, Attributes attributes) {
		if (COMP_UNIT.equals(name)) {
			if (attributes == null) {
				return "";
			}
			return attributes.getValue("path");
		}
		return null;
	}

	public Entity makeEntity(String name, Attributes a) {
		return new Entity(name, a);
	}

	public void start(String uid, String project) {
		System.out.println("uid = "+uid);
	}
	
	public void notify(Entity e) {
		if (!RESULT.equals(e.getName())) {
			throw new IllegalStateException("Unexpected top-level entity: "+e.getName());
		}
		if (e.numRefs() < 2) {
			throw new IllegalStateException("Missing about/source-ref: "+e.getName());
		}
		// I can build each result drop here
		boolean checkedPromises = false;
		ResultDrop d = new ResultDrop("unknown");
		d.setConsistent();
		
		for(Entity nested : e.getReferences()) {
			if (ABOUT_REF.equals(nested.getName())) {
				PromiseDrop<?> pd = handlePromiseRef(nested);
				if (pd == null) {
					throw new IllegalStateException("Unmatched about-ref: "+e.getAttribute(PROMISE_LOCATION));
				}
				d.addCheckedPromise(pd);
			}
			else if (JSureXMLReader.SOURCE_REF.equals(nested.getName())) {				
				IRNode n = handleSourceRef(nested);				
				if (n != null) {
					d.setNodeAndCompilationUnitDependency(n);
				}
			}
			else if (AND_REF.equals(nested.getName())) {			
				PromiseDrop<?> pd = handlePromiseRef(nested);
				if (pd == null) {
					IRNode location = findPromiseLocation(nested);
					d.setInconsistent();					
					d.addProposal(new ProposedPromiseDrop(nested.getAttribute(PROMISE), 
							                              nested.getAttribute(PROMISE_CONTENTS), 
							                              location, location));// TODO d.getNode()));
				} else {
					d.addTrustedPromise(pd);
				}
				checkedPromises = true;
			}
			
		}		
		if (checkedPromises) {
			d.setMessage(d.isConsistent() ? "Consistent" : "Inconsistent");
		} else {
			int number = Integer.parseInt(e.getAttribute(MESSAGE_CODE));
			d.setResultMessage(number, (Object[]) e.getAttribute(MESSAGE_ARGS).split(", "));
		}
		
	}
	
	private IRNode handleSourceRef(Entity sr) {
		final String file = sr.getAttribute(AbstractXMLReader.FILE_ATTR);
		final long hash   = Long.valueOf(sr.getAttribute(AbstractXMLReader.HASH_ATTR));
		final int offset  = Integer.valueOf(sr.getAttribute(AbstractXMLReader.OFFSET_ATTR));
		// final int line    = Integer.valueOf(sr.getAttribute(XMLConstants.LINE_ATTR));
		// TODO
		sr.getAttribute(AbstractXMLReader.PATH_ATTR);
		
		SourceCUDrop d = cuds.get(file);
		if (d == null) {
			return null; // Unknown CU
		}
		for(IRNode n : JJNode.tree.topDown(d.cu)) {
			ISrcRef ref = JavaNode.getSrcRef(n);
			if (ref != null && ref.getOffset() == offset) {
				final long nHash = SeaSummary.computeHash(n);
				if (hash == nHash) {
					return n;
				}
			}
		}
		return null;
	}
	
	private IRNode findPromiseLocation(Entity e) {
		return JavaIdentifier.findDecl(projects, e.getAttribute(PROMISE_LOCATION));
	}
	
	private PromiseDrop<?> handlePromiseRef(Entity pr) {
		IRNode location = findPromiseLocation(pr);
		String annoType = pr.getAttribute(PROMISE);
		String contents = pr.getAttribute(PROMISE_CONTENTS);
		if (contents.startsWith(annoType+' ')) {
			contents = contents.substring(annoType.length()+1);
		}
		if ("true".equals(pr.getAttribute(USE_IMPLICATION))) {
			return JavaIdentifier.isImpliedByPromise(location, annoType, contents);
		} else {
			return JavaIdentifier.findMatchingPromise(location, annoType, contents);
		}
	}

	public void done() {
		// Nothing to do here?
	}
}
