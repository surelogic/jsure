/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/AbstractSrcRef.java,v 1.6 2008/08/11 12:44:19 chance Exp $*/
package edu.cmu.cs.fluid.java;

import edu.cmu.cs.fluid.java.comment.IJavadocElement;

public abstract class AbstractSrcRef implements ISrcRef {
  public String getComment() {
    // TODO Auto-generated method stub
    return null;
  }
  public Object getEnclosingFile() {
    // TODO Auto-generated method stub
    return null;
  }
  public IJavadocElement getJavadoc() {
    // TODO Auto-generated method stub
    return null;
  }
  public int getLength() {
    // TODO Auto-generated method stub
    return 0;
  }
  public int getLineNumber() {
    // TODO Auto-generated method stub
    return 0;
  }
  public int getOffset() {
    // TODO Auto-generated method stub
    return 0;
  }
  
  public void clearJavadoc() {  
      // Nothing to do
  }
  
  public ISrcRef createSrcRef(int offset) {
	return null;
  }
}
