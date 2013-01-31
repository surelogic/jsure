package com.surelogic.javac;

import edu.cmu.cs.fluid.java.ICodeFile;

public class JarResource implements ICodeFile {
	private final String jarName;
	private final String qname;
	private final String project;

	public JarResource(String jar, String ref, String p) {
		jarName = jar;
		qname   = ref;
		project = p;
	}

	@Override
  public String getProjectName() {
		return project;
	}
	
	@Override
  public Object getHostEnvResource() {
		return null;
	}
	
	@Override
  public String getRelativePath() {
		return null;
	}

	@Override
  public String getPackage() {
		int lastDot = qname.lastIndexOf('.');		
		return lastDot < 0 ? qname : qname.substring(0, lastDot);
	}
	
	@Override
	public int hashCode() {
		return jarName.hashCode() + qname.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof JarResource) {
			JarResource f = (JarResource) o;
			return jarName.equals(f.jarName) && qname.equals(f.qname);
		}
		return false;
	}
	@Override
	public String toString() {
		return jarName+':'+qname;
	}
}
