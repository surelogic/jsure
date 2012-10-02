/*
 * Created on Jul 8, 2004
 *
 */
package edu.cmu.cs.fluid.java.comment;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRObjectType;
import edu.cmu.cs.fluid.ir.SlotInfo;

public interface IJavadocElement extends Iterable<Object> {

  /**
   * Fluid IR node that defines the type used by the {@link SlotInfo} for
   * {@link IJavadocElement}.
   */
  public static final IRObjectType<IJavadocElement> FLUID_JAVADOC_REF_SLOT_TYPE = new IRObjectType<IJavadocElement>();

  /**
   * Fluid IR name used the {@link SlotInfo} for {@link IJavadocElement} below.
   */
  public static final String JAVADOC_REF_SLOT_NAME = "JavaNode.IJavadocElement";

  /**
   * The text before any tags could contain inline tags
   * 
   * Any JavadocTags in order
   */
  Iterator<Object> elements();

  /**
   * Offset in characters for this in the Java source file.
   * 
   * @return an offset in characters for this in the Java source file.
   */
  int getOffset();

  /**
   * Length in characters for this in the Java source file.
   * 
   * @return the length in characters for this in the Java source file.
   */
  int getLength();
}
