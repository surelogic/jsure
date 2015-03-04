/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.java;

public abstract class AbstractCodeFile implements ICodeFile {
	@Override
  public String getProjectName() {
		throw new UnsupportedOperationException();
	}
	@Override
  public String getRelativePath() {
		return null;
	}
}
