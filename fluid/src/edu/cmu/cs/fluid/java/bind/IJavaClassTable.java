/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/IJavaClassTable.java,v 1.2 2006/04/28 21:38:28 boyland Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.util.Set;

import com.surelogic.common.Pair;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * A representation of a class table / CLASSPATH.
 * It maps outer names to IRNodes for the class declaration.
 * It keeps back-pointers for incrementality.
 * @author boyland
 */
public interface IJavaClassTable {

  /**
   * Return a class declaration or package declaration for a given fully-qualified
   * outer-level class or package.  The name must be fully qualified and must refer to
   * an outer level class, not a nested class.  The use site is given so that if we
   * are implementing incrementality, and this class changes, then the use site can
   * be notified.
   * @param qName fully-qualified name
   * @param useSite point in AST where we looked for this name.
   * @return class declaration or package declaration node, or null if no such
   * package or class exists.
   */
  public abstract IRNode getOuterClass(String qName, IRNode useSite);

  /**
   * Enumerate the fully-qualified names in this class table.  This should be used
   * only for debugging and/or user-interface issues or other areas in which incrementality
   * is not an issue.
   * @return immutable set of all fully-qualified names in the table
   */
  public abstract Set<String> allNames();

  public abstract Iterable<Pair<String, IRNode>> allPackages();
  
  /**
   * Return a searchable scope for a package.  This scope can be used to
   * look for outer classes and for nested packages.
   * @param pdecl declaration of a package
   * @return searchable scope
   */
  public abstract IJavaScope packageScope(IRNode pdecl);

  /**
   * Return a searchable scope for a package.  This scope can be used to
   * look for outer classes and for nested packages.
   * @param qName fully-qualified name of the package
   * @return searchable scope
   */
  public abstract IJavaPackageScope packageScope(String qName);
}