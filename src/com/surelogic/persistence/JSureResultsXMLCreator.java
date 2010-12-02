/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.persistence;

import java.io.*;
import java.util.List;

import com.surelogic.common.xml.Entities;

import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.xml.AbstractSeaXmlCreator;

public class JSureResultsXMLCreator extends AbstractSeaXmlCreator {
	public JSureResultsXMLCreator(OutputStream out) throws IOException {
		super(out);
	}

	public void reportResults(CUDrop cud, List<IAnalysisResult> results) {
		Entities.start(PersistenceConstants.COMP_UNIT, b, 0);
		Entities.addAttribute("path", cud.javaOSFileName, b);
		Entities.closeStart(b, false);
		flush();
		for(IAnalysisResult r : results) {
			r.outputToXML(this, 1, b);
			flush();
		}
		Entities.end(PersistenceConstants.COMP_UNIT, b, 0);
		flush();
	}

	private void flush() {
		flushBuffer(pw);
		/*
		System.out.print(b.toString());
		reset();
		*/
	}
}
