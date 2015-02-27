/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis;

public interface IAnalysisMonitor {
	boolean isCanceled();
	void subTask(String name, boolean log);	
	void subTaskDone(int work);
	void worked(int i);
}
