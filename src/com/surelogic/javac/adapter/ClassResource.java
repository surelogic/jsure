package com.surelogic.javac.adapter;

import java.io.File;

public class ClassResource {
	final String pkg;
	final String cuName;
	
	ClassResource(String qname) {
		int lastDot = qname.lastIndexOf('.');
		if (lastDot >= 0) {
			pkg = qname.substring(0, lastDot);
			cuName = qname.substring(lastDot+1)+".class";
		} else {
			pkg = "";
			cuName = qname+".class";
		}
	}
	
	ClassResource(String qname, File f) {
		this(qname);
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
}
