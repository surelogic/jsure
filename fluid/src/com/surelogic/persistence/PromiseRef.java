/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.persistence;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.*;
import com.surelogic.common.xml.Entities;
import com.surelogic.common.xml.XMLCreator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public class PromiseRef {
	private final String location;
	private final String promise;
	private final String contents;
	private final boolean useImplication;
	
	public PromiseRef(String anno, String details, IRNode decl, boolean implies) {
		final IIRProject p = JavaProjects.getEnclosingProject(decl);
		location = JavaIdentifier.encodeDecl(p.getTypeEnv().getProject(), decl);
		promise = anno;
		contents = details;
		useImplication = implies;
	}
	
	public PromiseRef(String anno, String details, IRNode decl) {
		this(anno, details, decl, true);
	}
	
	public <T extends IAASTRootNode> PromiseRef(PromiseDrop<T> d) {
		this(d.getPromiseName(), d.getAST().toString(), d.getNode(), false);
	}

	public void toXML(XMLCreator.Builder b) {
		b.addAttribute(PersistenceConstants.PROMISE_LOCATION, location);
		b.addAttribute(PersistenceConstants.PROMISE, promise);
		b.addAttribute(PersistenceConstants.PROMISE_CONTENTS, contents);
		b.addAttribute(PersistenceConstants.USE_IMPLICATION, useImplication);
		b.end();
	}
}
