package com.surelogic.javac.adapter;

import java.io.File;

import com.surelogic.javac.JavacProject;

public class ClassResource {
	final String pkg;
	final String cuName;
	final JavacProject project;
	/**
	 * jar or .class
	 */
	final File fileOrigin;
	final String jarPath;

	ClassResource(JavacProject proj, String qname, File f, String jarPath) {
		project = proj;
		int lastDot = qname.lastIndexOf('.');
		if (lastDot >= 0) {
			pkg = qname.substring(0, lastDot);
			cuName = qname.substring(lastDot+1)+".class";
		} else {
			pkg = "";
			cuName = qname+".class";
		}
		fileOrigin = f;
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
}
