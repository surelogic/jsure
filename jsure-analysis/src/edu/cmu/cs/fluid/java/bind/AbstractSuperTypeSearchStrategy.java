/*
 * Created on Dec 17, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * @author chance
 *
 */
public abstract class AbstractSuperTypeSearchStrategy<T>
	extends AbstractTypeSearchStrategy<T> implements ISuperTypeSearchStrategy<T> 
{
  protected T result;
  
  /**
   * Set of visited types (esp. interfaces)
   * -- classes may not be needed, except if handling nested classes (implicitly)
   */
	Set<Object> visited = new HashSet<Object>();

	/*
	 * Whether or not to search the super/subclasses of a type
	 */
	protected boolean searchAfterLastType = false;	
	
	protected AbstractSuperTypeSearchStrategy(IBinder bind, String prefix, String name) {
		super(bind, prefix, name);
	}

  private boolean visitedBefore(IRNode type) {
    if (visited.contains(type.identity())) {
      // System.out.println("Trying to visit "+JavaNames.getTypeName(type)+" again");    
      // (new Throwable("trace")).printStackTrace(System.out);

      return true; // No need to visit this one again
    }
    // Done before to prevent cycles
    
    visited.add(type.identity());
    // System.out.println("Visiting "+JavaNames.getTypeName(type)+" for the first time");  
    // (new Throwable("trace")).printStackTrace(System.out);
    return false;
  }
  
	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.eclipse.bind.ISuperTypeSearchStrategy#visitInterface(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
  public final void visitInterface(IRNode type) {
    boolean visited = visitedBefore(type);
    if (visited) {
      return;
    }		
    visitClass_internal(type);
	}
  
  @Override
  public final void visitClass(IRNode type) {
    boolean visited = visitedBefore(type);
    if (visited) {
      return;
    }   
    visitClass_internal(type);
  }
  
  protected abstract void visitClass_internal(IRNode type);
  
	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.eclipse.bind.ISuperTypeSearchStrategy#visitSuperclass()
	 */
	@Override
  public boolean visitSuperclass() {
		return searchAfterLastType;
	}
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.eclipse.bind.ISuperTypeSearchStrategy#visitSuperifaces()
	 */
	@Override
  public boolean visitSuperifaces() {
		// Defaults to the same as visitSuperclass()
		return visitSuperclass();
	}
	
	@Override
  public void reset() {
	  visited.clear();
	  result = null;
	}
	
	@Override
  public T getResult() {
	  return result;
	}
}
