/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.persistence;

public interface IAnalysisResult {
	String outputToXML(JSureResultsXMLCreator creator, int indent, StringBuilder sb);
}
