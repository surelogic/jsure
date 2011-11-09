/*
 * Created on Jul 8, 2004
 *
 */
package edu.cmu.cs.fluid.java.comment;

import java.util.Iterator;


/**
 * 
 * @author Edwin
 *
 */
public class JavadocElement extends java.util.ArrayList<Object> implements IJavadocElement {
  final int offset, length;
  
  // May need to size the array to 0 since
  // usually there aren't that many tags
  public JavadocElement(int pos, int len) {
    super(0);
    offset = pos;
    length = len;
  }
  
  /**
   * The text before any tags could contain inline tags
   * 
   * Any JavadocTags in order
   */
  public Iterator<Object> elements() {
    return iterator();
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.comment.IJavadocElement#getOffset()
   */
  public int getOffset() {
    return offset;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.comment.IJavadocElement#getLength()
   */
  public int getLength() {
    return length;
  }
  
  public void addElt(IJavadocElement elt) {
    add(elt);
  }
}
