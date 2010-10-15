package edu.cmu.cs.fluid.sea;

import java.net.URI;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.comment.IJavadocElement;

/**
 * Drop to represent promise scrubber warnings.
 */
public final class PromiseWarningDrop extends IRReferenceDrop 
implements ISrcRef, IResultDrop
{
  private int offset = -1;
  
  public PromiseWarningDrop() {
    this(-1);
  }
  
  public PromiseWarningDrop(int off) {
    offset = off;
  }
  
  @Override
  public ISrcRef getSrcRef() {
    final ISrcRef ref = super.getSrcRef();
    if (ref != null) {
      if (offset >= 0) {
        //System.out.println("Getting ref for "+this.getMessage());
        return this;
      } else {
        return ref;
      }
    }
    return null;
  }
  
  public void setOffset(int off) {
    if (off < 0) {
      throw new IllegalArgumentException("negative offset");
    }
    offset = off;
  }
  
  /*******************
   * Stuff to implement ISrcRef
   *******************/
  
  public void clearJavadoc() {
    throw new UnsupportedOperationException();
  }

  public String getComment() {
    throw new UnsupportedOperationException();
  }

  public Object getEnclosingFile() {
    return super.getSrcRef().getEnclosingFile();
  }
  
  public URI getEnclosingURI() {
	  return super.getSrcRef().getEnclosingURI();
  }

  public String getRelativePath() {
	  return super.getSrcRef().getRelativePath();
  }
  
  public IJavadocElement getJavadoc() {
    throw new UnsupportedOperationException();
  }

  public int getLength() {
    return 0;
  }

  public int getLineNumber() {
	  return super.getSrcRef().getLineNumber();
  }

  public int getOffset() {
    return offset;
  }
  
  public Long getHash() {
	  return super.getSrcRef().getHash();
  }

  public String getCUName() {
	  return super.getSrcRef().getCUName();
  }

  public String getPackage() {
	  return super.getSrcRef().getPackage();
  }
  
  public ISrcRef createSrcRef(int offset) {
	throw new UnsupportedOperationException();
  }
}
