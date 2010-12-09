/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.persistence;

import java.util.*;

import com.surelogic.analysis.*;
import com.surelogic.common.xml.*;
import com.surelogic.jsure.xml.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.*;
import edu.cmu.cs.fluid.sea.xml.SeaSummary;

public class JSureResultsXMLReader extends AbstractJSureResultsXMLReader<ResultDrop> {
	private final Map<String,SourceCUDrop> cuds = new HashMap<String, SourceCUDrop>();

	public JSureResultsXMLReader(IIRProjects p) {
		super(p);
		for(SourceCUDrop d : Sea.getDefault().getDropsOfExactType(SourceCUDrop.class)) {
			cuds.put(d.javaOSFileName, d);
		}
	}
	
	@Override
	protected ResultDrop createResult() {
		ResultDrop d = new ResultDrop("unknown");
		d.setConsistent();
		return d;
	}

	@Override
	protected void handleSourceRef(ResultDrop d, Entity srcRef) {
		IRNode n = findIRNode(srcRef);	
		if (n != null) {
			d.setNodeAndCompilationUnitDependency(n);
		}
	}
	
	@Override
	protected void handleAboutRef(ResultDrop d, Entity pe, PromiseDrop<?> pd) {
		if (pd == null) {
			handlePromiseRef(pe);
			throw new IllegalStateException("Unmatched about-ref: "+pe.getAttribute(PROMISE_LOCATION));
		}
		d.addCheckedPromise(pd);
	}

	@Override
	protected void handleAndRef(ResultDrop d, Entity nested, PromiseDrop<?> pd) {
		if (pd == null) {
			IRNode location = findPromiseLocation(nested);
			d.setInconsistent();					
			d.addProposal(new ProposedPromiseDrop(nested.getAttribute(PROMISE), 
					                              nested.getAttribute(PROMISE_CONTENTS), 
					                              location, location));// TODO d.getNode()));
		} else {
			d.addTrustedPromise(pd);
		}
	}

	@Override
	protected void finishResult(ResultDrop d, Entity e, boolean checkedPromises) {
		if (checkedPromises) {
			d.setMessage(d.isConsistent() ? "Consistent" : "Inconsistent");
		} else {
			int number = Integer.parseInt(e.getAttribute(MESSAGE_CODE));
			d.setResultMessage(number, (Object[]) e.getAttribute(MESSAGE_ARGS).split(", "));
		}
	}

	private IRNode findIRNode(Entity sr) {
		final String file = sr.getAttribute(AbstractXMLReader.FILE_ATTR);
		final long hash   = Long.valueOf(sr.getAttribute(AbstractXMLReader.HASH_ATTR));
		final int offset  = Integer.valueOf(sr.getAttribute(AbstractXMLReader.OFFSET_ATTR));
		// final int line    = Integer.valueOf(sr.getAttribute(XMLConstants.LINE_ATTR));
		// TODO
		// sr.getAttribute(AbstractXMLReader.PATH_ATTR);
		
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
	
	@Override
	protected PromiseDrop<?> handlePromiseRef(Entity pr) {
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
}
