/*
 * Created on Jan 8, 2005
 *
 * $header$
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.UniquenessAnnotation;
import edu.cmu.cs.fluid.util.Lattice;

/**
 * @author boyland
 *
 * A coarse lattice expressing the important parts of the permissions
 * system.  This lattice is <em>not</em> suitable for conservative positive
 * analysis.  It implements the following simple permission/capability
 * lattice:
 * <pre>
 *                   unique
 *               unique-write
 *  immutable           shared/owned
 *            readonly                   borrowed
 *                          useless
 * </pre>
 * We may extend/modify the lattice as theory indicates.
 */
@Deprecated
public class CoarsePermissionLattice implements Lattice {

  // implemented with some private hacks
  private final int level; // 0 = UNIQUE, 1 = U-W, 2=SHARED, 3 = BORROWED, 4 = USELESS
  private final boolean readonly;
  private final IRNode owner;
  private static final int OWNER_RELEVANT = 2;
  
  private CoarsePermissionLattice(int lev, boolean ro) {
    level = lev;
    readonly = ro;
    owner = null;
  }
  
  private CoarsePermissionLattice(boolean ro, IRNode own) {
    level = OWNER_RELEVANT;
    readonly = ro;
    owner = own;
  }
  
  public static final CoarsePermissionLattice UNIQUE = new CoarsePermissionLattice(0,false);
  public static final CoarsePermissionLattice UNIQUEWRITE = new CoarsePermissionLattice(1,false);
  public static final CoarsePermissionLattice IMMUTABLE = new CoarsePermissionLattice(1,true);
  public static final CoarsePermissionLattice SHARED = new CoarsePermissionLattice(2,false);
  public static final CoarsePermissionLattice READONLY= new CoarsePermissionLattice(2,true);
  public static final CoarsePermissionLattice BORROWED = new CoarsePermissionLattice(3,false);
  public static final CoarsePermissionLattice USELESS = new CoarsePermissionLattice(4,true);

  public static final boolean DEFAULT_IS_BORROWED = true;
  
  /**
   * Return the coarse lattice value for a declared parameter, receiver,
   * or field.
   * @param decl IR node for field, parameter or receiver.
   * @return lattice value representing its annotation
   */
  public static CoarsePermissionLattice fromAnnotation(IRNode decl) {
     if (UniquenessAnnotation.isUnique(decl)) {
      return UNIQUE;
    }
    if (UniquenessAnnotation.isImmutable(decl)) {
      return IMMUTABLE;
    }
    // TODO: fix this up to handle new annotations
    /*
    if (UniquenessAnnotation.isShared(decl)) {
      return SHARED;
    }
    if (UniquenessAnnotation.isReadonly(decl)) {
      return READONLY;
    }
    if (UniquenessAnnotation.isUniqueWrite(decl)) {
      return UNIQUEWRITE;
    }
    */
    if (DEFAULT_IS_BORROWED || UniquenessAnnotation.isBorrowed(decl)) {
      // TODO: should check method effects to see if
      // we should actually return "borrowed readonly".
      // This is optional since doing this will only mask errors, not cause them.
      return BORROWED;
    }
   return SHARED;
  }
  
  public static CoarsePermissionLattice getOwner(IRNode owner) {
    return new CoarsePermissionLattice(false,owner);
  }
  
  public IRNode getOwner() {
    return owner;
  }
  
  // compare two owners:
  // we haven't nailed down how to compare owners.  This is
  // probably too strict.
  public static boolean ownersEqual(IRNode o1, IRNode o2) {
    return o1.equals(o2);
  }
 
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.util.Lattice#top()
   */
  public Lattice top() {
    return UNIQUE;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.util.Lattice#bottom()
   */
  public Lattice bottom() {
    return USELESS;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.util.Lattice#meet(edu.cmu.cs.fluid.util.Lattice)
   */
  public Lattice meet(Lattice other) {
    if (this == UNIQUE || other == USELESS)
      return other;
    if (other == UNIQUE || this == USELESS)
      return this;
    if (includes(other))
      return this;
    CoarsePermissionLattice o = (CoarsePermissionLattice) other;
    if (o.includes(this))
      return o;
    if (level == OWNER_RELEVANT) {
      if (o.level < OWNER_RELEVANT) {
        return new CoarsePermissionLattice(readonly || o.readonly, owner);
      } else if (level == OWNER_RELEVANT) {
        // owners not equal because of earlier check for includes
        return new CoarsePermissionLattice(level + 1, readonly || o.readonly);
      }
    } else if (o.level == OWNER_RELEVANT) {
      if (level < OWNER_RELEVANT) {
        return new CoarsePermissionLattice(readonly || o.readonly, o.owner);
      }
    }
    int new_level = level;
    if (level < o.level)
      new_level = o.level;
    return new CoarsePermissionLattice(new_level, readonly || o.readonly);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof CoarsePermissionLattice)) return false;
    CoarsePermissionLattice o = (CoarsePermissionLattice)other;
    if (o.level != level || o.readonly != readonly) return false;
    if (level == OWNER_RELEVANT) return ownersEqual(owner,o.owner);
    return true;
  }
  
  @Override
  public int hashCode() {
    return level + (readonly ? 0x1776 : 0) +
      (level == OWNER_RELEVANT ? owner.hashCode() : 0);
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.util.Lattice#includes(edu.cmu.cs.fluid.util.Lattice)
   */
  public boolean includes(Lattice other) {
    if (other.equals(this)) return true;
    CoarsePermissionLattice o = (CoarsePermissionLattice)other;
    if (readonly && !o.readonly) return false;
    if (level > o.level) return false;
    if (level == OWNER_RELEVANT && o.level == level) return ownersEqual(owner,o.owner);
    return true;
  }

}
