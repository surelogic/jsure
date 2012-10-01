package edu.cmu.cs.fluid.java;

import java.net.URI;

import edu.cmu.cs.fluid.java.comment.IJavadocElement;

public abstract class AbstractSrcRef implements ISrcRef {

  public String getEnclosingFile() {
    return null;
  }

  public URI getEnclosingURI() {
    return null;
  }

  public String getRelativePath() {
    return null;
  }

  public IJavadocElement getJavadoc() {
    return null;
  }

  public int getLength() {
    return 0;
  }

  public int getLineNumber() {
    return 0;
  }

  public int getOffset() {
    return 0;
  }

  public void clearJavadoc() {
  }

  public ISrcRef createSrcRef(int offset) {
    return null;
  }

  public String getJavaId() {
    return null;
  }
  
  public String getEnclosingJavaId() {
	  return null;
  }
}
