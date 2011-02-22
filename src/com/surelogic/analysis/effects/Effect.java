package com.surelogic.analysis.effects;

import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetRelationship;
import com.surelogic.analysis.effects.targets.TargetRelationships;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Represents an effect on a region. Effects are either read or write, where
 * write includes read. Effect instances are immutable.
 * 
 * <p>
 * Effects are tagged with their source expression. The type of this expression
 * is one of:
 * <dl>
 * <dt>UseExpression
 * <dd>For reading or writing a local variable.
 * 
 * <dt>ThisExpression
 * <dd>For reading the receiver (<code>this</code>).
 * 
 * <dt>VariableDeclarator
 * <dd>For writing to a local variable or field in an variable initializer.
 * 
 * <dt>ArrayRefExpression
 * <dd>For reading or writing to an array element (region <code>[]</code>).
 * 
 * <dt>FieldRef
 * <dd>For reading or writing to a field of an object/class.
 * 
 * <dt>NewExpression, MethodCall, ConstructorCall
 * <dd>Latent effect from invoking a method or constructor.
 * 
 * <dt>MethodDeclaration, ConstructorDeclaration
 * <dd>Latent effect from invoking a method or constructor; effects with this
 * source are not instantiated for a particular invocation, but are the raw
 * annotated effects of the method/constructor.
 * </dl>
 * 
 * <p>An Effect refers to the effect it is <em>elaborated</em> from.  Elaboration
 * is a process used with Instance targets to process aggregation from 
 * unique references and to trace uses of local variables to more concrete 
 * sources such as method parameters or method return values.
 * 
 * @see Target
 * @author Aaron Greenhouse
 */
public abstract class Effect {
  public enum Kind {
    READ("reads"), WRITE("writes");
    
    final String unparseAs;
    
    private Kind(final String label) {
      unparseAs = label;
    }
    
    public String unparse() {
      return unparseAs;
    }
  }
  
  
  
  private static final class EmptyEffect extends Effect {
    private EmptyEffect(final IRNode src) {
      super(src);
    }

  
  
    @Override
    public boolean isMaskable(final IBinder binder) {
      /* Empty effects are not maskable because we want to process them so
       * that we can create links to the annotation that declared them.
       */
      return false;
    }
    
    @Override
    public boolean affectsReceiver(final IRNode rcvrNode) {
      // Empty effects affect nothing
      return false;
    }

    @Override
    public Effect setSource(final IRNode src) {
      return new EmptyEffect(src);
    }

    @Override
    public Target getTarget() {
      return null;
    }
    
    @Override
    public ElaborationEvidence getTargetElaborationEvidence() {
      return null;
    }

    @Override
    public boolean isTargetAggregated() {
      return false;
    }
    
    @Override
    public boolean isRead() {
      return false;
    }
    
    @Override
    public boolean isWrite() {
      return false;
    }
    
    @Override
    public boolean isEmpty() {
      return true;
    }
    
    @Override
    public EffectRelationship conflictsWith(
        final IMayAlias mayAlias, final IBinder binder, final Effect e) {
      // Empty effects never conflict with any other effect
      return EffectRelationship.newNoConflict();
    }
    
    @Override 
    public boolean isCheckedBy(final IBinder binder, final Effect declEffect) {
      // Empty effects are checked by any other effect
      return true;
    }
    
    @Override
    boolean checksRead(final IBinder binder, final Effect implEffect) {
      // Empty effects do not check read/write effects
      return false;
    }
    
    @Override
    boolean checksWrite(final IBinder binder, final Effect implEffect) {
      // Empty effects do not check read/write effects
      return false;
    }
    
    @Override
    public String unparseForPromise() {
      return "";
    }
    
    @Override
    public String toString() {
      return "nothing";
    }
    
    @Override
    public boolean equals(final Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof EmptyEffect) {
        final EmptyEffect other = (EmptyEffect) o;
        return source == null ? other.source == null : source.equals(other.source);
      } else {
        return false;
      }
    }
    
    @Override
    public int hashCode() {
      int result = 17;
      result = 31 * result + (source == null ? 0 : source.hashCode());
      return result;
    }
  }
  
  
  
  private abstract static class RealEffect extends Effect {
    /**
     * The target of the effect.
     */
    protected final Target target;

   
    
    /**
     * Create a new effect instance
     * 
     * @param src
     *          The source of the effect.
     * @param t
     *          Target of the effect
     */
    protected RealEffect(final IRNode src, final Target t) {
      super(src);
      target = t;
    }

    
    
    @Override
    public final boolean isMaskable(final IBinder binder) {
      return target.isMaskable(binder);
    }

    @Override
    public final boolean affectsReceiver(final IRNode rcvrNode) {
      return target.overlapsReceiver(rcvrNode);
    }

    @Override
    public final Target getTarget() {
      return target;
    }

    @Override
    public final ElaborationEvidence getTargetElaborationEvidence() {
      return target.getElaborationEvidence();
    }

    @Override
    public final boolean isTargetAggregated() {
      return target.isAggregated();
    }
    
    @Override
    public final boolean isEmpty() {
      return false;
    }
    
    @Override 
    public final String unparseForPromise() {
      return target.toString();
    }
  }
  
  
  
  private static final class ReadEffect extends RealEffect {
    private ReadEffect(final IRNode src, final Target t) {
      super(src, t);
    }

    @Override
    public Effect setSource(final IRNode src) {
      return new ReadEffect(src, target);
    }
    
    @Override
    public boolean isRead() {
      return true;
    }
    
    @Override
    public boolean isWrite() {
      return false;
    }

    @Override
    public EffectRelationship conflictsWith(
        final IMayAlias mayAlias, final IBinder binder, final Effect e) {
      // Conflict only if the other effect is a write effect
      if (e.isWrite()) {
        final TargetRelationship overlap =
          getTarget().overlapsWith(mayAlias, binder, e.getTarget());
        if (overlap.getTargetRelationship() != TargetRelationships.UNRELATED) {
          return EffectRelationship.newReadAWriteB(overlap);
        }
      }
      return EffectRelationship.newNoConflict();
    }
    
    @Override 
    public boolean isCheckedBy(final IBinder binder, final Effect declEffect) {
      return declEffect.checksRead(binder, this);
    }
    
    @Override
    boolean checksRead(final IBinder binder, final Effect implEffect) {
      return implEffect.getTarget().checkTarget(binder, this.getTarget());
    }
    
    @Override
    boolean checksWrite(final IBinder binder, final Effect implEffect) {
      // Read effects do not check write effects      
      return false;
    }
    
    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("reads ");
      target.toString(sb);
      return sb.toString();
    }
    
    @Override
    public boolean equals(final Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof ReadEffect) {
        final ReadEffect other = (ReadEffect) o;
        return (source == null ? other.source == null : source.equals(other.source))
            && target.equals(other.target);
      } else {
        return false;
      }
    }
    
    @Override
    public final int hashCode() {
      int result = 17;
      result = 31 * result + (source == null ? 0 : source.hashCode());
      return result;
    }
  }
  
  
  
  private static final class WriteEffect extends RealEffect {
    private WriteEffect(final IRNode src, final Target t) {
      super(src, t);
    }
    
    @Override
    public Effect setSource(final IRNode src) {
      return new WriteEffect(src, target);
    }
    
    @Override
    public boolean isRead() {
      return false;
    }
    
    @Override
    public boolean isWrite() {
      return true;
    }
    
    @Override
    public EffectRelationship conflictsWith(
        final IMayAlias mayAlias, final IBinder binder, final Effect e) {
      if (!e.isEmpty()) {
        final TargetRelationship overlap =
          getTarget().overlapsWith(mayAlias, binder, e.getTarget());
        if (overlap.getTargetRelationship() != TargetRelationships.UNRELATED) {
          if (e.isRead()) {
            return EffectRelationship.newWriteAReadB(overlap);
          } else { // both write effects
            return EffectRelationship.newWritesConflict(overlap);
          }
        }
      }
      return EffectRelationship.newNoConflict();
    }
    
    @Override 
    public boolean isCheckedBy(final IBinder binder, final Effect declEffect) {
      return declEffect.checksWrite(binder, this);
    }
    
    @Override
    boolean checksRead(final IBinder binder, final Effect implEffect) {
      return implEffect.getTarget().checkTarget(binder, this.getTarget());
    }
    
    @Override
    boolean checksWrite(final IBinder binder, final Effect implEffect) {
      return implEffect.getTarget().checkTarget(binder, this.getTarget());
    }
    
    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("writes ");
      target.toString(sb);
      return sb.toString();
    }
    
    @Override
    public boolean equals(final Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof WriteEffect) {
        final WriteEffect other = (WriteEffect) o;
        return (source == null ? other.source == null : source.equals(other.source))
            && target.equals(other.target);
      } else {
        return false;
      }
    }
    
    @Override
    public final int hashCode() {
      int result = 19;  // don't clash with read effects
      result = 31 * result + (source == null ? 0 : source.hashCode());
      return result;
    }
  }
  
  
  
  /**
   * The expression that directly caused the effect or <code>null</code> if
   * unknown.
   */
  protected final IRNode source;

  
  
  private Effect(final IRNode src) {
    source = src;
  }
  
  /**
   * Create a new effect.
   * 
   * @param src
   *          The source of the effect.
   * @param read
   *          <tt>true</tt> to create a read effect, <tt>false</tt> to
   *          create a write effect
   * @param t
   *          Target of the effect
   * @return An effect of the appropriate kind on target <tt>t</tt>
   */
  public static Effect newEffect(
      final IRNode src, final boolean read, final Target t) {
    return read ? newRead(src, t) : newWrite(src, t);
  }
  
  /**
   * Create a new read effect.
   * 
   * @param src
   *          The source of the effect.
   * @param t
   *          Target of the effect
   * @return An read affect on <tt>t</tt>
   */
  public static Effect newRead(final IRNode src, final Target t) {
    return new ReadEffect(src, t);
  }

  /**
   * Create a new write effect.
   * 
   * @param src
   *          The source of the effect.
   * @param t
   *          Target of the effect
   * @return An write affect on <tt>t</tt>
   */
  public static Effect newWrite(final IRNode src, final Target t) {
    return new WriteEffect(src, t);
  }
  
  /**
   * Create a new empty effect.
   * 
   * @param src
   *          The source of the effect.
   */
  public static Effect newEmpty(final IRNode src) {
    return new EmptyEffect(src);
  }

  
  
  /**
   * Does this effect refer to state that is not accessible outside of the
   * method in which it originates. 
   */
  public abstract boolean isMaskable(IBinder binder);
  
  /**
   * Does this effect affect an instance region of the receiver only?  That is,
   * the effect cannot possibly conflict with an effect on any other object, and 
   * cannot affect a static field.
   */
  public abstract boolean affectsReceiver(IRNode rcvrNode);
  
  /**
   * Get the source of the effect.
   * 
   * @return The IRNode of the expression that directly caused the effect.
   */
  public final IRNode getSource() {
    return source;
  }

  /**
   * Get a copy of this effect except change the source of the new effect.
   * 
   * @param src
   *          The new source of the effect
   * @return A copy of this effect with the source changed to <code>src</code>.
   */
  public abstract Effect setSource(IRNode src);

  /**
   * Query whether the effect is indirect, that is originates from invoking a
   * method/constructor.
   */
  public final boolean isIndirect() {
    if (source == null) {
      return false;
    } else {
      final Operator op = JJNode.tree.getOperator(source);
      return MethodDeclaration.prototype.includes(op)
          || ConstructorDeclaration.prototype.includes(op)
          || op instanceof CallInterface;
    }
  }

  /**
   * Query whether the effect is direct, that is does not originate from a
   * latent method/constructor effect.
   */
  public boolean isDirect() {
    return !isIndirect();
  }

  /**
   * Get the target of the effect.
   * 
   * @return The target of the effect or <code>null</code> if the effect
   * is the empty effect.
   */
  public abstract Target getTarget();

  /**
   * Get the rationale for the elaboration of the target, if it was
   * elaborated.  Returns {@value null} if the target was not generated
   * from elaboration.
   */
  public abstract ElaborationEvidence getTargetElaborationEvidence();
  
  /** 
   * Did the target of this effect result from aggregation? 
   */
  public abstract boolean isTargetAggregated();
  
  /**
   * Query if the effect is a read effect.
   */
  public abstract boolean isRead();
  
  /**
   * Query if the effect is a write effect.
   */
  public abstract boolean isWrite();
  
  /**
   * Query if the effect is an empty effect.
   */
  public abstract boolean isEmpty();

  
  
  /**
   * Determine if the effect might conflict with another effect. Two effects
   * conflict if at least one of them is a write, and the targets overlap.
   * 
   * @param am
   *          The particular alias analysis to use
   * @param e
   *          The effect to check against
   * @return An {@link EffectRelationship} object describing the reason for the
   *         conflict, which includes describing a non-conflict.
   */
  public abstract EffectRelationship conflictsWith(
     IMayAlias ma, IBinder binder, Effect e);

  /**
   * Assuming that the receiver is an implementation effect left over after
   * masking, see if the given declaration effect checks, that is accounts for,
   * the effect.
   */
  public abstract boolean isCheckedBy(IBinder binder, Effect declEffect);

  /**
   * Assuming that the receiver is a declared effect, does it account for
   * the given read effect.
   */
  abstract boolean checksRead(IBinder binder, Effect implEffect);

  /**
   * Assuming that the receiver is a declared effect, does it account for
   * the given write effect.
   */
  abstract boolean checksWrite(IBinder binder, Effect implEffect);
  
  

  /**
   * Convert the effect to a string that can be used in a RegionEffect 
   * promise.   
   */
  public abstract String unparseForPromise();
  
  /**
   * Convert the effect to a String. The string is of the form "<tt>read( </tt><I>target</I><tt> )</tt>"
   * or "<tt>write( </tt><I>target</I><tt> )</tt>", as appropriate, where
   * <i>target</i> is the String representation of the effect's target.
   * 
   * @return The String representation of the effect
   */
  @Override
  public abstract String toString();
  
  /**
   * Compare two effects. Two effects are equal if the have the same target and
   * are either both read or both write effects and they have the same source.
   * 
   * @param obj
   *          The object to test.
   * @see #reallyEquals(Effect)
   * @return <code>true</code> if the two effects are equal;
   *         <code>false</code> otherwise.
   */
  @Override
  public abstract boolean equals(Object o);

  /**
   * Get the hash value of an effect. The hash value is equal to the
   * <code>getTarget().hashCode()</code> if the effect is a read effect,
   * otherwise the hash value is the bitwise complement of the target's hash
   * value.
   * 
   * @return The hash value of the effect.
   */
  @Override
  public abstract int hashCode();
}
