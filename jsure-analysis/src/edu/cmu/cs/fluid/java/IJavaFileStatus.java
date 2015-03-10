/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/IJavaFileStatus.java,v 1.8 2008/08/25 15:32:49 chance Exp $*/
package edu.cmu.cs.fluid.java;

import edu.cmu.cs.fluid.ir.*;
import com.surelogic.common.java.Config.Type;

public interface IJavaFileStatus<T> {
  long NO_TIME = 0;
  
  /**
   * Returns the resource identifier
   */
  T id();
  
  /**
   * Returns the root of the AST tree
   */
  IRNode root();
  
  /**
   * Returns the label associated with this resource
   */
  String label();
  
  /**
   * @return The modification timestamp of this resource, or NO_TIME
   */
  long modTime();
  
  /**
   * Returns true if fully loaded as source
   * False if only as interface
   */
  boolean asSource();
  
  /**
   * Return what type of AST this is
   */
  Type getType();
  
  /**
   * Return if the corresponding AST has been canonicalized
   */
  boolean isCanonical();
  
  boolean isCanonicalizing();

  /**
   * Canonicalize the corresponding AST
   */
  void canonicalize();
  
  /**
   * Returns true if the resource is loaded in the system
   */
  boolean isLoaded();

  /**
   * Returns true if the resource has been saved to disk
   */
  boolean isPersistent();
}
