package com.surelogic.xml;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ref.IDecl;

//import edu.cmu.cs.fluid.tree.Operator;

/**
 * Mostly for use by a Content/LabelProvider
 * 
 * @author Edwin
 */
public interface IJavaElement {
  <T> T visit(IJavaElementVisitor<T> v);

  /**
   * @return the simple name for this element
   */
  String getName();
  
  IDecl.Kind getKind();
  
  /**
   * Sets the parent of this in the XML tree. An exception will be thrown if the
   * parent is already set.
   * 
   * @param p
   *          the parent of this.
   * @throws IllegalStateException
   *           if the parent is already set.
   */
  void setParent(IJavaElement p);

  /**
   * Gets the parent of this in the XML tree. This method returns <tt>null</tt>
   * if this is the root.
   * 
   * @return the parent of this, or <tt>null</tt> if this is the root.
   */
  IJavaElement getParent();

  /**
   * Gets the root parent of this XML tree. This method will not return a
   * <tt>null</tt> value. If it is not possible to get the package this is
   * enclosed within an exception is thrown.
   * 
   * @return the root parent of this XML tree, which must be a
   *         {@link PackageElement}.
   * @throws IllegalArgumentException
   *           if no root parent can be found.
   */
  PackageElement getRootParent();

  /**
   * Gets the image key from {@link CommonImages} for this. Suitable for UI use.
   * 
   * @return the image key from {@link CommonImages} for this.
   */
  String getImageKey();

  /**
   * Gets the label to display for this. Suitable for UI use.
   * 
   * @return the label to display for this.
   */
  String getLabel();

  /**
   * Checks if this has child XML elements.
   * 
   * @return <tt>true</tt> if this has child XML elements, <tt>false</tt>
   *         otherwise.
   */
  boolean hasChildren();

  /**
   * Gets the child XML elements for this element.
   * 
   * @return a list of the child XML elements.
   */
  Object[] getChildren();

  /**
   * Check for changes (via the editor) from the files on disk.
   * 
   * @return <tt>true</tt> if changed from the files on disk, <tt>false</tt> if
   *         unchanged.
   */
  boolean isDirty();

  /**
   * A best effort to determine that this is "bad", e.g. because the syntax is
   * wrong. Is not a complete check, JSure may find this to be bad even if this
   * call does not.
   * 
   * @return <tt>true</tt> if this annotation is "bad", <tt>false</tt> if it
   *         appears okay.
   */
  boolean isBad();

  /**
   * Check for changes compared to shipped XML from SureLogic.
   * 
   * @return <tt>true</tt> if changed from shipped version, <tt>false</tt> if
   *         unchanged.
   */
  boolean isModified();

  /**
   * Check if this can be edited by either adding annotations or modifying the
   * contents of this (if this is an annotation).
   * 
   * @return <tt>true</tt> if this can be edited, <tt>false</tt> otherwise.
   */
  boolean isEditable();

  /**
   * Gets the IR operator (like method declaration) for this.
   * 
   * @return the IR operator (like method declaration) for this.
   */
  //Operator getOperator();

  /**
   * Gets the release version of this Type/CU. This is stored on the
   * {@link PackageElement}.
   * 
   * @return the version of this Type/CU.
   */
  int getReleaseVersion();

  /**
   * Sets the release version of this Type/CU. This is stored on the
   * {@link PackageElement}.
   */
  void setReleaseVersion(int value);

  /**
   * Increments the release version of this Type/CU by one. This is stored on
   * the {@link PackageElement}.
   * <p>
   * This method has the same effect as the following code:
   * 
   * <pre>
   * setReleaseVersion(getReleaseVersion() + 1);
   * </pre>
   */
  void incrementReleaseVersion();

  /**
   * Checks is this element is a static declaration.
   * 
   * @return {@code true} if this is a static declaration, {@code false}
   *         otherwise.
   */
  boolean isStatic();
}
