/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AttributeInheritancePolicy.java,v 1.7 2003/07/15 21:47:18 aarong Exp $
 * AttributeInheritancePolicy.java
 *
 * Created on December 12, 2001, 9:40 AM
 */

package edu.cmu.cs.fluid.mvc;


/**
 * Describes strategies that may be used by
 * implementations of {@link AttributeInheritanceManager}s to determine
 * which attributes should be inherited.
 *
 * Implementations are expected to be thread-safe, and they should
 * probably <em>not</em> be part of any particular model's monitor.
 *
 * @author Aaron Greenhouse
 */
public interface AttributeInheritancePolicy
{
  /**
   * Singleton reference to a policy that never inherits any attributes.
   */
  public static final AttributeInheritancePolicy nullPolicy =
    new NullAttributeInheritancePolicy();
  
  /**
   * Singleton reference to an empty HowToInherit array.
   */
  public static final HowToInherit[] emptyArray = new HowToInherit[0];
  
  
  
  /**
   * Record that describes how an attribute should be inherited.
   */
  public static class HowToInherit
  {
    /** The name of the attribute to inherit (interned). */
    public final String attr;
    
    /**
     * The name to give the attribute when it is inherited (interned).
     */
    public final String inheritAs;
    
    /**
     * The mode in which the attribute should be inherited.
     * Inheritance modes are defined in {@link AttributeInheritanceManager}s.
     */
    public final Object mode;
    
    /** 
     * The kind with which the attribute should be inherited.
     */
    public final int kind;
    
    /** Create a record for an inheritable attribute. */
    public HowToInherit( final String srcAttr, final String newAttr,
      final Object mode, final int kind )
    {
      this.attr = srcAttr;
      this.inheritAs = newAttr;
      this.mode = mode;
      this.kind = kind;
    }
  }

  /**
   * Query which model-level attributes from the given model should be
   * inherited.
   * @param from The model from which the attribute would be inherited.
   */
  public HowToInherit[] compAttrsToInherit( Model from );

  /**
   * Query which node-level attributes from a give model should be 
   * inherited.
   * @param from The model from which the attribute would be inherited.
   */
  public HowToInherit[] nodeAttrsToInherit( Model from );
}  



/**
 * An attribute inheritance policy that never inherits any attributes.
 */
final class NullAttributeInheritancePolicy
implements AttributeInheritancePolicy
{
  @Override
  public HowToInherit[] compAttrsToInherit( final Model from ) 
  {
    return AttributeInheritancePolicy.emptyArray;
  }
  
  @Override
  public HowToInherit[] nodeAttrsToInherit( final Model from ) 
  {
    return AttributeInheritancePolicy.emptyArray;
  }
}
