/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis;

public interface IIRProjects {
	String getLabel();
	Iterable<String> getProjectNames();
	Iterable<? extends IIRProject> getProjects();
	IIRProject get(String name);
}
