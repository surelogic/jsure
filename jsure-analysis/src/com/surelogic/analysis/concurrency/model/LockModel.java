package com.surelogic.analysis.concurrency.model;

import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;

/**
 * Lock model for the entire set of classes being analyzed.  At its
 * core, it maps regions to locks.  This is subdivided by taking the class
 * hierarchy into consideration.  Also provides the means for looking up
 * locks by name or by implementing object reference.
 */
public final class LockModel {
  /**
   * We map these to model locks.  Allows us to abstract away the difference
   * between using <code>this</code>, <code>.class</code>, or a real field
   * as a lock.
   */
  private static interface Member {
    // ANything to go here?
  }
  
  private abstract static class IRNodeMember implements Member {
    protected final IRNode decl;
    
    public IRNodeMember(final IRNode d) {
      decl = d;
    }
  }

  private final static class Field extends IRNodeMember {
    // fieldDecl must be a VariableDeclarator from a FieldDeclaration
    public Field(final IRNode fieldDecl) {
      super(fieldDecl);
    }
    
    @Override
    public String toString() {
      return VariableDeclarator.getId(decl);
    }
    
    @Override
    public int hashCode() {
      return 31 * 17 + decl.hashCode();
    }
    
    @Override
    public boolean equals(final Object other) {
      if (other == this) { 
        return true;
      } else if (other instanceof Field) {
        return decl.equals(((Field) other).decl);
      } else {
        return false;
      }
    }
  }
  
  private final static class ClassObject extends IRNodeMember {
    // typeDecl must be a TypeDeclaration
    public ClassObject(final IRNode typeDecl) {
      super(typeDecl);
    }
    
    @Override
    public String toString() {
      return TypeDeclaration.getId(decl) + ".class";
    }
    
    @Override
    public int hashCode() {
      return 31 * 17 + decl.hashCode();
    }
    
    @Override
    public boolean equals(final Object other) {
      if (other == this) { 
        return true;
      } else if (other instanceof ClassObject) {
        return decl.equals(((ClassObject) other).decl);
      } else {
        return false;
      }
    }
  }
  
  private final static class Self implements Member {
    public static final Self prototype = new Self();
    
    private Self() {
      super();
    }
    
    @Override
    public String toString() {
      return "this";
    }
    
    // use default hashcode and identity-based equality
  }
  
  // ======================================================================
  
  private static final class Clazz {
//    /**
//     * Class record of the super class, or <code>null</code> if this
//     * class is the root of the hierarchy.
//     */
//    private final Clazz parent;
//
//    /** The type representation of the class. */
//    private final IJavaType classDecl;    
//
//    /**
//     * Map from VariableDeclarators (fields) to the locks they implement.  
//     * The implementations do not have to be from this class.
//     */
//    private final SetMultimap<IRNode, ModelLock<?, ?>> fieldAsLock = HashMultimap.create();
//    
//    /**
//     * The locks declared in this class.
//     */
//    private final Set<ModelLock<?, ?>> declaredLocks;
    
    /* XXX: How to represent .class and this? */
    
    
    
    public Clazz() {
      super();
    }
  }
  
  // ======================================================================
  
  private final IBinder binder;
  
  public LockModel(final IBinder binder) {
    this.binder = binder;
  }

  
}
