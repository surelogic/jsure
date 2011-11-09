/*
 * Created on Jul 8, 2004
 *
 */
package edu.cmu.cs.fluid.java.comment;

import java.util.Iterator;


/**
 * @author Edwin
 *
 */
public interface IJavadocElement extends Iterable<Object> {
  /**
   * The text before any tags could contain inline tags
   * 
   * Any JavadocTags in order
   */
  Iterator<Object> elements();
  int getOffset();
  int getLength();
}
