package com.surelogic.javac.adapter;

import java.net.URI;

import edu.cmu.cs.fluid.java.ISrcRef;

public class ClassRef extends AbstractRef {
	final ClassResource clazz;
	
	ClassRef(ClassResource c, int line) {
		super(line);
		clazz = c;
	}
	
	public ISrcRef createSrcRef(int offset) {
		return this;
	}

	public String getCUName() {
		return clazz.getCUName();
	}

	public Object getEnclosingFile() {
		return clazz.getRelativePath();
	}

	public URI getEnclosingURI() {
		return null;
	}

	public Long getHash() {
		return clazz.getHash()+line;
	}

	public int getLength() {
		return 0;
	}

	public int getOffset() {
		return 0;
	}

	public String getPackage() {
		return clazz.getPackage();
	}

	public String getRelativePath() {
		return clazz.getRelativePath();
	}
}
