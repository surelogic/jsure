package com.surelogic.javac.adapter;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.comment.IJavadocElement;

public abstract class AbstractRef implements ISrcRef {
	final int line;
	
	AbstractRef(long l) {
		line = (int) l;
	}
	
	public void clearJavadoc() {
		// Nothing to do
	}

	public IJavadocElement getJavadoc() {
		return null;
	}

	public String getComment() {
		return null;
	}
	
	public int getLineNumber() {
		return line;
	}
	
	public String getJavaId() {
		return null;
	}
}
