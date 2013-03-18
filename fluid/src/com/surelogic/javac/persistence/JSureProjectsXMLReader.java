package com.surelogic.javac.persistence;

import com.surelogic.common.java.*;
import com.surelogic.javac.Javac;
import com.surelogic.javac.JavacProject;
import com.surelogic.javac.Projects;

import edu.cmu.cs.fluid.ide.IDEPreferences;

public class JSureProjectsXMLReader extends JavaProjectsXMLReader<JavacProject> {
  public JSureProjectsXMLReader() {
    super(Projects.javaFactory);
  }

  @Override
  protected void setupDefaultJRE(String projectName) {
	  if (projectName.startsWith(Config.JRE_NAME)) {
		  // TODO what if this should be JavacEclipse?
		  Javac.getDefault().setPreference(IDEPreferences.DEFAULT_JRE, projectName);
	  }
  }
}
