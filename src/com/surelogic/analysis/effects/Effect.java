package com.surelogic.analysis.effects;

import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetRelationship;
import com.surelogic.analysis.effects.targets.TargetRelationships;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.IAliasAnalysis;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/* 99-02-23
 * 
 * Effects now contain a field indicating the IRnode of the expression
 * that directly produced the effect.
 *
 * No longer uses the flyweight pattern.  Hopefully this will be fixed
 * in the future.
 *
 * Implements equals() and hashCode()
 */

/*
 * 98-06-01:
 * - Added newEffect( boolean, Target )
 * - Added javadoc
 * - Split conflictsWith() into mayConflict() and mustConflict()
 */

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
public final class Effect {
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
  
  
  
  /**
   * The target of the effect.
   */
  protected final Target target;

  /**
   * {@link Kind#READ} for a read effect, {@link Kind#WRITE} for a write effect.
   */
  protected final Kind kind;

  /**
   * The expression that directly caused the effect or <code>null</code> if
   * unknown.
   */
  protected final IRNode source;

//  /**
//   * Elaboration data, or <code>null</code> if the effect is not elaborated
//   * from any other effect.
//   */
//  protected final ElaborationEvidence elaborationEvidence;
//  
  
  
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
  public static Effect newEffect(final IRNode src, final boolean read,
      final Target t) {
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
    return new Effect(src, t, Kind.READ);
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
    return new Effect(src, t, Kind.WRITE);
  }

  /**
   * Create a new read effect with an unknown source.
   * 
   * @param t
   *          Target of the effect
   * @return An read affect on <tt>t</tt>
   */
  public static Effect newRead(final Target t) {
    return new Effect(null, t, Kind.READ);
  }

  /**
   * Create a new write effect with an unknown source.
   * 
   * @param t
   *          Target of the effect
   * @return An write affect on <tt>t</tt>
   */
  public static Effect newWrite(final Target t) {
    return new Effect(null, t, Kind.WRITE);
  }

  
  
  /**
   * Constructor. Private because we want to enforce the use of the state
   * factory methods.
   * 
   * @param src
   *          The source of the effect.
   * @param t
   *          Target of the effect
   * @param read
   *          <tt>true</tt> to create a read effect, <tt>false</tt> to
   *          create a write effect
   */
  private Effect(final IRNode src, final Target t, final Kind k) {
    source = src;
    target = t;
    kind = k;
  }

  
  
  /**
   * Does this effect refer to state that is not accessible outside of the
   * method in which it originates. 
   */
  public boolean isMaskable(final IBinder binder) {
    return target.isMaskable(binder);
  }
  
  /**
   * Is this an effect on the instance region of the given receiver declaration
   * node?
   */
  public boolean affectsReceiver(final IRNode rcvrNode) {
    return target.overlapsReceiver(rcvrNode);
  }
  
  /**
   * Get the source of the effect.
   * 
   * @return The IRNode of the expression that directly caused the effect.
   */
  public IRNode getSource() {
    return source;
  }

  /**
   * Get a copy of this effect except change the source of the new effect.
   * 
   * @param src
   *          The new source of the effect
   * @return A copy of this effect with the source changed to <code>src</code>.
   */
  public Effect setSource(final IRNode src) {
    return new Effect(src, target, kind);
  }

  /**
   * Query whether the effect is indirect, that is originates from invoking a
   * method/constructor.
   */
  public boolean isIndirect() {
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
   * @return The target of the effect
   */
  public Target getTarget() {
    return target;
  }

  /**
   * Query if the effect is a read effect.
   * 
   * @return <tt>true</tt> iff the effect is a read effect
   */
  public boolean isReadEffect() {
    return kind == Kind.READ;
  }

  /**
   * Query if the effect is a write effect.
   * 
   * @return <tt>true</tt> iff the write is a read effect
   */
  public boolean isWriteEffect() {
    return kind == Kind.WRITE;
  }

  
  
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
  public EffectRelationship conflictsWith(final IAliasAnalysis.Method am,
      final IBinder binder, final Effect e) {
    if (!isReadEffect() || !e.isReadEffect()) {
      final TargetRelationship overlap = target.overlapsWith(am, binder,
          e.target);
      if (overlap.getTargetRelationship() != TargetRelationships.UNRELATED) {
        if (isReadEffect() && !e.isReadEffect()) {
          return EffectRelationship.newReadAWriteB(overlap);
        } else if (!isReadEffect() && e.isReadEffect()) {
          return EffectRelationship.newWriteAReadB(overlap);
        } else if (!isReadEffect() && !e.isReadEffect()) {
          return EffectRelationship.newWritesConflict(overlap);
        }
      }
    }
    return EffectRelationship.newNoConflict();
  }

  public boolean checkEffect(final IBinder binder, final Effect e) {
    final boolean tgtChecks = target.checkTarget(binder, e.target);
    if (tgtChecks) {
      /*
       * only proceed if e is a write effect, in which case it must check us, or
       * if we are a read effect, which because of short circuit evaluation
       * means that e is a read effect, so we must be a read effect to be
       * checked by it.
       */
      return !e.isReadEffect() || this.isReadEffect();
    }
    return false;
  }

  
  
  /**
   * Convert the effect to a String. The string is of the form "<tt>read( </tt><I>target</I><tt> )</tt>"
   * or "<tt>write( </tt><I>target</I><tt> )</tt>", as appropriate, where
   * <i>target</i> is the String representation of the effect's target.
   * 
   * @return The String representation of the effect
   */
  @Override
  public String toString() {
    return toString(new StringBuilder()).toString();
  }

  public StringBuilder toString(final StringBuilder sb) {
    sb.append(kind.unparse());
    sb.append(' ');
    target.toString(sb);
    return sb;
  }
  
  
  /**
   * Compare two effects. Two effects are equal if the have the same target and
   * are either both read or both write effects. The source of the two effects
   * is not used.
   * 
   * @param obj
   *          The object to test.
   * @see #reallyEquals(Effect)
   * @return <code>true</code> if the two effects are equal;
   *         <code>false</code> otherwise.
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof Effect) {
      final Effect eff = (Effect) obj;
      return
        (eff.kind == this.kind) &&
        eff.target.equals(target) &&
        ((eff.source == null && source == null) || eff.source.equals(source));
    } else {
      return false;
    }
  }

  /**
   * Get the hash value of an effect. The hash value is equal to the
   * <code>getTarget().hashCode()</code> if the effect is a read effect,
   * otherwise the hash value is the bitwise complement of the target's hash
   * value.
   * 
   * @return The hash value of the effect.
   */
  @Override
  public int hashCode() {
    final int hc = target.hashCode();
    return (isReadEffect() ? hc : ~hc);
  }
}
