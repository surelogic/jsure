/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.persistence;

import java.io.*;
import java.util.List;

import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.xml.AbstractSeaXmlCreator;

public class JSureResultsXMLCreator extends AbstractSeaXmlCreator {
	public JSureResultsXMLCreator(OutputStream out) throws IOException {
		super(out);
	}

	public void reportResults(CUDrop cud, List<IAnalysisResult> results) {
		try {
			b.start(PersistenceConstants.COMP_UNIT);
			b.addAttribute("path", cud.getJavaOSFileName());
			for(IAnalysisResult r : results) {
				r.outputToXML(this, b);
			}
			b.end();
		} finally {
			flushBuffer();
		}
	}
}
