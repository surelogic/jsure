package com.surelogic.javac.adapter;

import java.io.File;

import com.surelogic.javac.JavacProject;

public class ClassResource {
	final String pkg;
	final String cuName;
	final JavacProject project;
	final String pathToJarOrClass;
	final String jarPath;

	ClassResource(JavacProject proj, String qname, File f) {
		this(proj, qname, f.getAbsolutePath(), null);
	}
	
	ClassResource(JavacProject proj, String qname, String relPath, String jarPath) {
		project = proj;
		int lastDot = qname.lastIndexOf('.');
		if (lastDot >= 0) {
			pkg = qname.substring(0, lastDot);
			cuName = qname.substring(lastDot+1)+".class";
		} else {
			pkg = "";
			cuName = qname+".class";
		}
		pathToJarOrClass = relPath;
		this.jarPath = jarPath;
	}
	
	public String getPackage() {
		return pkg;
	}

	public String getCUName() {
		return cuName;
	}
	
	public String getRelativePath() {
		return getPackage()+'/'+getCUName();
	}

	public long getHash() {
		return getRelativePath().hashCode();
	}

	public JavacProject getProject() {
		return project;
	}
	
	public String getProjectName() {
		return project.getName();
	}
	
	public String getWorkspaceRelativePath() {
		return pathToJarOrClass;
	}
	
	public String getJarRelativePath() {
		return jarPath;
	}
}
