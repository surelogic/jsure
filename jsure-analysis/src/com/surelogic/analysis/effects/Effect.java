package com.surelogic.analysis.effects;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.concurrency.model.instantiated.NeededLock;
import com.surelogic.analysis.effects.targets.EmptyTarget;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetRelationship;
import com.surelogic.analysis.effects.targets.TargetRelationships;
import com.surelogic.analysis.effects.targets.evidence.EmptyEvidence;
import com.surelogic.analysis.effects.targets.evidence.TargetEvidence;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;

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
 * <p>An Effect also refers to the set of locks that must be held by the thread
 * that produces this effect in order for the code that produces the effect
 * to comply with the programs locking policy.
 * 
 * @see Target
 * @author Aaron Greenhouse
 */
public abstract class Effect {
  /**
   * The target of the effect.
   */
  protected final Target target;
  
  /**
   * The expression that directly caused the effect or <code>null</code> if
   * unknown.
   */
  protected final IRNode source;

  /**
   * Any additional evidence for the existence of this effect.
   * Never null
   */
  protected final EffectEvidence evidence;
  
  /**
   * The set of locks that should be held before producing this effect.
   */
  protected final Set<NeededLock> neededLocks;
  
  
  
  private Effect(final IRNode src, final Target t,
      final EffectEvidence evidence,
      final Set<NeededLock> neededLocks) {
    target = t;
    source = src;
    this.evidence = evidence;
    this.neededLocks = neededLocks;
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
  public static Effect effect(
      final IRNode src, final boolean read, final Target t,
      final EffectEvidence evidence,
      final Set<NeededLock> neededLocks) {
    return read ? read(src, t, evidence, neededLocks) : write(src, t, evidence, neededLocks);
  }

  public static Effect effect(
      final IRNode src, final boolean read, final Target t,
      final EffectEvidence evidence,
      final NeededLock neededLock) {
    return read ? read(src, t, evidence, neededLock) : write(src, t, evidence, neededLock);
  }

  public static Effect effect(
      final IRNode src, final boolean read, final Target t,
      final EffectEvidence evidence) {
    return Effect.effect(src, read, t, evidence, ImmutableSet.<NeededLock>of());
  }
  
  

  /**
   * Convert the effect to a string that can be used in a RegionEffect 
   * promise.   
   */
  public abstract String unparseForPromise();
  
  /**
   * Convert the effect to a string that can be used in a DropSea message.
   */
  public abstract String unparseForMessage();

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

  protected boolean baseEquals(final Effect other) {
    return target.equals(other.target) &&
        (source == null ? other.source == null : source.equals(other.source)) &&
        evidence.equals(other.evidence) &&
        neededLocks.equals(other.neededLocks);
  }
  
  @Override
  public final int hashCode() {
    int result = 17;
    result = 31 * result + target.hashCode();
    result = 31 * result + (source == null ? 0 : source.hashCode());
    result = 31 * result + evidence.hashCode();
    result = 31 * result + neededLocks.hashCode();
    return result;
  }

  
  
  private static final class EmptyEffect extends Effect {
    /** Only for use by changeSource(). */
    private EmptyEffect(final IRNode src, final Target t, 
        final EffectEvidence evidence, final Set<NeededLock> neededLocks) {
      super(src, t, evidence, neededLocks);
    }

    private EmptyEffect(final IRNode src, final EmptyEvidence evidence,
        final EffectEvidence effectEvidence, final Set<NeededLock> neededLocks) {
      this(src, new EmptyTarget(evidence), effectEvidence, neededLocks);
    }

  
  
    @Override 
    public EmptyEffect mask(final IBinder binder) {
      /* Empty effects are not maskable because we want to process them so
       * that we can create links to the annotation that declared them.
       */
      return this;
    }
    
    @Override
    public boolean affectsReceiver(final IRNode rcvrNode) {
      // Empty effects affect nothing
      return false;
    }
    
    @Override
    public EmptyEffect changeSource(final IRNode src, final TargetEvidence e, final EffectEvidence newEvidence) {
      return new EmptyEffect(src, target.changeEvidence(e), newEvidence, neededLocks);
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
      return EffectRelationship.noConflict();
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
    public String unparseForMessage() {
      return "nothing";
    }
    
    @Override
    public String toString() {
      return "nothing " + neededLocks.toString();
    }
    
    @Override
    public boolean equals(final Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof EmptyEffect) {
        return baseEquals((EmptyEffect) o);
      } else {
        return false;
      }
    }
  }
  
  
  
  private abstract static class RealEffect extends Effect {
    /**
     * Create a new effect instance
     * 
     * @param src
     *          The source of the effect.
     * @param t
     *          Target of the effect
     */
    protected RealEffect(final IRNode src, final Target t,
        final EffectEvidence evidence, final Set<NeededLock> neededLocks) {
      super(src, t, evidence, neededLocks);
    }

    protected final Effect maskImpl(final IBinder binder, final boolean isRead) {
      final Target maskedTarget = target.mask(binder);
      if (maskedTarget == null) {
        return null;
      } else if (maskedTarget == target) {
        return this;        
      } else {
        return effect(source, isRead, maskedTarget, evidence, neededLocks);
      }
    }

    
    @Override
    public final boolean affectsReceiver(final IRNode rcvrNode) {
      return target.overlapsReceiver(rcvrNode);
    }

    @Override
    public final boolean isEmpty() {
      return (target instanceof EmptyTarget);
    }
    
    @Override 
    public final String unparseForPromise() {
      return target.toString();
    }
  }
  
  
  
  private static final class ReadEffect extends RealEffect {
    private ReadEffect(final IRNode src, final Target t,
        final EffectEvidence evidence, final Set<NeededLock> neededLocks) {
      super(src, t, evidence, neededLocks);
    }

    @Override
    public final Effect mask(final IBinder binder) {
      return maskImpl(binder, true);
    }
    
    @Override
    public ReadEffect changeSource(final IRNode src, final TargetEvidence e, final EffectEvidence newEvidence) {
      return new ReadEffect(src, target.changeEvidence(e), newEvidence, neededLocks);
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
          return EffectRelationship.readAWriteB(overlap);
        }
      }
      return EffectRelationship.noConflict();
    }
    
    @Override 
    public boolean isCheckedBy(final IBinder binder, final Effect declEffect) {
      return isEmpty() || declEffect.checksRead(binder, this);
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
    public String unparseForMessage() {
      final StringBuilder sb = new StringBuilder("reads ");
      target.toString(sb);
      return sb.toString();
    }
    
    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("reads ");
      target.toString(sb);
      sb.append(' ');
      sb.append(neededLocks.toString());
      return sb.toString();
    }
    
    @Override
    public boolean equals(final Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof ReadEffect) {
        return baseEquals((ReadEffect) o);
      } else {
        return false;
      }
    }
  }
  
  
  
  private static final class WriteEffect extends RealEffect {
    private WriteEffect(final IRNode src, final Target t,
        final EffectEvidence evidence, final Set<NeededLock> neededLocks) {
      super(src, t, evidence, neededLocks);
    }

    @Override
    public final Effect mask(final IBinder binder) {
      return maskImpl(binder, false);
    }
    
    @Override
    public WriteEffect changeSource(final IRNode src, final TargetEvidence e, final EffectEvidence newEvidence) {
      return new WriteEffect(src, target.changeEvidence(e), newEvidence, neededLocks);
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
            return EffectRelationship.writeAReadB(overlap);
          } else { // both write effects
            return EffectRelationship.writesConflict(overlap);
          }
        }
      }
      return EffectRelationship.noConflict();
    }
    
    @Override 
    public boolean isCheckedBy(final IBinder binder, final Effect declEffect) {
      return isEmpty() || declEffect.checksWrite(binder, this);
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
    public String unparseForMessage() {
      final StringBuilder sb = new StringBuilder("writes ");
      target.toString(sb);
      return sb.toString();
    }
    
    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("writes ");
      target.toString(sb);
      sb.append(' ');
      sb.append(neededLocks.toString());
      return sb.toString();
    }
    
    @Override
    public boolean equals(final Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof WriteEffect) {
        return baseEquals((WriteEffect) o);
      } else {
        return false;
      }
    }
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
  public static Effect read(final IRNode src, final Target t,
      final EffectEvidence evidence,
      final NeededLock neededLock) {
    return new ReadEffect(src, t, evidence, ImmutableSet.of(neededLock));
  }

  public static Effect read(final IRNode src, final Target t,
      final EffectEvidence evidence,
      final Set<NeededLock> neededLocks) {
    return new ReadEffect(src, t, evidence, neededLocks);
  }

  public static Effect read(final IRNode src, final Target t,
      final EffectEvidence evidence) {
    return Effect.read(src, t, evidence, ImmutableSet.<NeededLock>of());
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
  public static Effect write(final IRNode src, final Target t,
      final EffectEvidence evidence,
      final NeededLock neededLock) {
    return new WriteEffect(src, t, evidence, ImmutableSet.of(neededLock));
  }
  
  public static Effect write(final IRNode src, final Target t,
      final EffectEvidence evidence,
      final Set<NeededLock> neededLocks) {
    return new WriteEffect(src, t, evidence, neededLocks);
  }
  
  public static Effect write(final IRNode src, final Target t,
      final EffectEvidence evidence) {
    return Effect.write(src, t, evidence, ImmutableSet.<NeededLock>of());
  }
  
  /**
   * Create a new empty effect.
   * 
   * @param src
   *          The source of the effect.
   */
  public static Effect empty(final IRNode src, final EmptyEvidence evidence, final EffectEvidence effectEvidence, final Set<NeededLock> neededLocks) {
    return new EmptyEffect(src, evidence, effectEvidence, neededLocks);
  }

  public static Effect empty(final IRNode src, final EmptyEvidence evidence, final EffectEvidence effectEvidence, final NeededLock neededLock) {
    return new EmptyEffect(src, evidence, effectEvidence, ImmutableSet.of(neededLock));
  }

  public static Effect empty(final IRNode src, final EmptyEvidence evidence, final EffectEvidence effectEvidence) {
    return new EmptyEffect(src, evidence, effectEvidence, ImmutableSet.<NeededLock>of());
  }
  
  /**
   * Mask the effect, that is, convert it to an empty effect if the effect
   * affects state that is not visible outside of the flow unit that generated
   * the effect.
   * 
   * @return The masked effect, or <code>null</code> if the effect should be
   *         ignored completely.
   */
  public abstract Effect mask(IBinder binder);
  
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
   * Get a copy of this effect except change the source and evidence of the new
   * effect.
   * 
   * @param src
   *          The new source of the effect
   * @param e
   *          The new evidence for the target.
   * @return An effect of the same implementation class whose source node and
   *         target evidence are modifed with the given arguments.
   */
  public abstract Effect changeSource(IRNode src, TargetEvidence e, final EffectEvidence newEvidence);
  
  /**
   * Get the target of the effect.
   * 
   * @return The target of the effect or <code>null</code> if the effect
   * is the empty effect.
   */
  public final Target getTarget() {
    return target;
  }

  public final EffectEvidence getEvidence() {
    return evidence;
  }
  
  /**
   * Get the rationale for the elaboration of the target, if it was
   * elaborated.  Returns {@value null} if the target was not generated
   * from elaboration.
   */
  public final TargetEvidence getTargetEvidence() {
    return target.getEvidence();
  }

  /**
   * Get the locks that need to be held before producing this effect.
   */
  public final Set<NeededLock> getNeededLocks() {
    return neededLocks;
  }
  
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
}
