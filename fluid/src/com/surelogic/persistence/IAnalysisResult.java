/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.persistence;

import com.surelogic.common.xml.XMLCreator;

public interface IAnalysisResult {
	void outputToXML(JSureResultsXMLCreator creator, XMLCreator.Builder sb);
}
