package edu.cmu.cs.fluid.java.bind;

import com.surelogic.ThreadSafe;

import edu.cmu.cs.fluid.util.*;
import edu.cmu.cs.fluid.ir.*;

/**
 * Get information about Java AST nodes from binding information.
 * @see ITypeEnvironment
 * @author boyland
 */
@ThreadSafe
public interface IBinder {  
  void disableWarnings();
  
  void enableWarnings();
	
  /** 
   * Return the type environment that is used by this IBinder
   * to compute its results.
   */
  ITypeEnvironment getTypeEnvironment();

  IRNode ELIDED = new MarkedIRNode("Elided code");
  
  /** 
   * Get the IRNode representing the declaration that this name binds to.
   * 
   * @return Could return ELIDED if the binding was elided from the code
   * @-deprecated use {@link #getIBinding(IRNode)}
   */
  //@Deprecated
  IRNode getBinding(IRNode name);

  /**
   * Return the binding associated with this node.  This will include information
   * about type formals.
   */
  IBinding getIBinding(IRNode node);

  /**
   * Like above, but uses context to help figure what to return
   */
  IBinding getIBinding(IRNode node, IRNode contextFlowUnit);
  
  /**
   * Return the type of an expression or of a declaration (field or local or 
   * receiver).
   * @param n
   * @return type of this expression or declaration.
   * @throws SlotUndefinedException if not an expression or variable declaration.
   */
  IJavaType getJavaType(IRNode n);

  /*
   * Return the superclass of the given class (or null if not a class)
   * @deprecated This will do the wrong thing in JDK 1.5.
   * In some cases, ITypeEnvironment.getSuperTypes(IJavaType) will do what one needs.
   * (You may need to ignore all but the first result.)
   */
  //IRNode getSuperclass(IRNode type);
  
  /**
   * Return the superclass of the given class (or null if not a class).
   *@deprecated use type.getSuperClass(binder.getTypeEnvironment())
   */
  @Deprecated
  IJavaDeclaredType getSuperclass(IJavaDeclaredType type);

  /************************************************************************
   * Methods that search up the class hierarchy
   ************************************************************************/

  /** Static method resolution.
   * <p>
   * This method is inefficient and is currently used only internal
   * to the binder.  We may eventually add functionality such as this, but
   * it would have to be indexed by IJavaTypes instead of IRNodes.
   * @deprecated should be internal to Binder
   */
  //IRNode findMethod(IRNode type, String method, IRNode[] sig);

  // TODO what about other kinds of names that need binding?

  /************************************************************************
   * Methods that search up/down the class hierarchy
   * 
   * Note that those that search down into subclasses may return incomplete
   * results if we are not analyzing all of the code, e.g., due to dynamic
   * loading.
   ************************************************************************/

  /**
   * Look inside a type and superclasses and interfaces for something.
   * The method is poorly named, since it doesn't do anything special with class members.
   * @param type
   * @param tvs
   * @return
   */
  <T> T findClassBodyMembers(IRNode type, ISuperTypeSearchStrategy<T> tvs,
      boolean throwIfNotFound);

  /**
   * Return an iteration over all methods that this method immediately overrides.
   * There may be more than one if a class has more than one supertype
   * as is possible with interfaces.
   * @param mth
   * @return
   */
  Iteratable<IBinding> findOverriddenParentMethods(IRNode mth);

  /**
   * Return an iteration over all methods that this method declaration
   * overrides, both directly or indirectly.
   * @param methodDeclaration
   * @return
   */
  Iteratable<IBinding> findOverriddenMethods(IRNode methodDeclaration);

  /**
   * Find some of the overriding methods that are possibly called given the
   * receiver type decl.  The results of this method depend strongly on
   * the current state of the type environment / binder.  There are no guarantees
   * even that all currently loaded classes will be checked for overriding methods,
   * let alone that this method handles classes that are part of the project but
   * not (yet) loaded, not to speak of classes that may be loaded dynamically.
   * Any analysis that uses this information is technically unsound.
   * @param callee a method declaration node.
   * @param receiverType a type declaration node.
   * @return iterator of method declarations from the given type or some of
   * its subtypes that override directly or indirectly the given method declaration.
   */

  Iteratable<IRNode> findOverridingMethodsFromType(IRNode callee, IRNode receiverType);
}