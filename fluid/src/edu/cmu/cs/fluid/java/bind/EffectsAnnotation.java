/*
 * Created on Oct 30, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.Effects;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.util.CogenUtil;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.sea.drops.promises.MethodEffectsPromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

/**
 * @author chance
 * 
 */
@Deprecated
public class EffectsAnnotation extends AbstractPromiseAnnotation {
  private static final String READS_TAG = "Reads";
  
  static SlotInfo<IRSequence<IRNode>> effectsSI;

  private static SlotInfo<MethodEffectsPromiseDrop> methodEffectsSIDrop = SimpleSlotFactory.prototype
      .newAttribute(null);

  public static MethodEffectsPromiseDrop getMethodEffectsDrop(IRNode declNode) {
    // LOG.info("Looking for method drop on "+DebugUnparser.toString(declNode));
    MethodEffectsPromiseDrop drop = declNode.getSlotValue(methodEffectsSIDrop);

    if (drop != null) {
      return drop;
    } else {
      if (methodEffects(declNode) != null) {
        drop = new MethodEffectsPromiseDrop(null);
        drop.setCategory(JavaGlobals.EFFECTS_CAT);
        drop.setMessage(Messages.EffectsAnnotation_prescrubbedEffects,
            JavaNames.genMethodConstructorName(declNode)); //$NON-NLS-1$
        // if (result.isFromSrc()) {
        // System.out.print("Drop from source: " + result.getMessage());
        // System.out.println();
        // }
        declNode.setSlotValue(methodEffectsSIDrop, drop);
        drop.setAttachedTo(declNode, methodEffectsSIDrop);
        return drop;
      } else
        return null;
    }
  }

  private EffectsAnnotation() {
    super();
  }

  private static final EffectsAnnotation instance = new EffectsAnnotation();

  public static final IPromiseAnnotation getInstance() {
    return instance;
  }

  private static Operator getOperator(final IRNode n) {
    return JJNode.tree.getOperator(n);
  }

  /**
   * Generates a model name from the promise.
   * 
   * @param node
   *          a constructor or method declaration
   * @return a created model name for the thread effects declaration
   */
  private static String genModelName(final IRNode node,
      final List<Effect> effects) {
    final Operator op = getOperator(node);
    // add the type we found the method within (could be the promised type)
    IRNode enclosingType = VisitUtil.getEnclosingType(node);
    String typeName = JavaNames.getTypeName(enclosingType);
    String targetName = "(none)";
    if (MethodDeclaration.prototype.includes(op)) {
      targetName = MethodDeclaration.getId(node);
      IRNode args = MethodDeclaration.getParams(node);
      targetName += JavaNames.genArgList(args);
    } else if (ConstructorDeclaration.prototype.includes(op)) {
      targetName = ConstructorDeclaration.getId(node);
      IRNode args = ConstructorDeclaration.getParams(node);
      targetName += JavaNames.genArgList(args);
    }
    String reads = "reads ";
    int readCt = 0;
    String writes = "writes ";
    int writeCt = 0;
    for (Iterator<Effect> i = effects.iterator(); i.hasNext();) {
      Effect eff = i.next();
      if (eff.isRead()) {
        reads += (readCt++ > 0 ? ", " : "") + eff.getTarget().toString();
      }
      if (eff.isWrite()) {
        writes += (writeCt++ > 0 ? ", " : "") + eff.getTarget().toString();
      }
    }
    if (readCt == 0) {
      reads += "nothing ";
    }
    if (writeCt == 0) {
      writes += "nothing";
    }
    return reads + " " + writes + " on " + typeName + "." + targetName;
  }

  /**
   * Enumerate the effects of a method.
   * 
   * @return null if no effects promised for method.
   */
  public static Iterator<IRNode> methodEffects(IRNode methodNode) {
    Iterator<IRNode> e = getEnum_filtered(effectsSI, methodNode);
    if (e.hasNext()) {
      e.next();
      return e;
    } else {
      return null;
    }
  }

  /**
   * Query the presence or absence of declared effects on the given method.
   * 
   * @param methodOrCtorDecl
   *          The IRNode declaration of a method or constructor.
   * @return true iff the given decl has declared effects.
   */
  public static boolean hasDeclaredEffects(IRNode methodOrCtorDecl) {
    final Iterator<IRNode> i = methodEffects(methodOrCtorDecl);
    final boolean res = i == null;
    return res;
  }

  private static IRSequence<IRNode> createEmptyEffectsSequence() {
    IRSequence<IRNode> s = sf.newSequence(~0);
    s.appendElement(null);
    return s;
  }

  /**
   * Set the effects of a method.
   * 
   * @param effects
   *          an enumeration of EffectSpecification IR nodes. if null then no
   *          promises are made concerning the method.
   */
  public static void setMethodEffects(final IRNode methodNode,
      final Iterator<IRNode> effects) {
    final IRNode n = frame.mapToProxyNode(methodNode);

    /*
     * First we remove the old effects. (We do this first since some of these
     * effects may be in the enumeration we're about to add.)
     */
    final Iterator<IRNode> old = methodEffects(n);
    if (old != null) {
      while (old.hasNext()) {
        IRNode effectNode = old.next();
        JavaPromise.detachPromiseNode(n, effectNode);
      }
    }
    IRSequence<IRNode> s;
    if (effects == null) {
      s = null;
    } else {
      s = createEmptyEffectsSequence();

      while (effects.hasNext()) {
        IRNode effect = effects.next();
        JavaPromise.attachPromiseNode(n, effect);
        s.appendElement(effect);
      }
    }
    n.setSlotValue(effectsSI, s);
    // treeChanged.noteChange(methodNode);
  }

  /**
   * Add a effect declaration node to the list of effects for this method
   * declaration node. It does not check to see this effect declaration node is
   * already in the list.
   */
  public static void addMethodEffect(final IRNode methodNode,
      final IRNode effectNode) {
    if (effectNode == null) {
      return;
    }
    // final String mName = JavaNames.genMethodConstructorName(methodNode);
    final IRNode n = frame.mapToProxyNode(methodNode);

    Object o = n.getSlotValue(effectsSI);
    if (o == null) {
      n.setSlotValue(effectsSI, createEmptyEffectsSequence());
    }
    addToSeq_mapped(effectsSI, methodNode, effectNode);
  }

  /**
   * Remove a effect declaration node from the list of effects for this method
   * declaration node. It returns true if the effect node was found (and
   * removed).
   */
  public static boolean removeMethodEffect(final IRNode methodNode,
      final IRNode effectNode) {
    if (effectNode == null) {
      return false;
    }
    return removeFromEnum_mapped(effectsSI, frame.getProxyNode(methodNode),
        effectNode);
  }

  /**
   * @see edu.cmu.cs.fluid.java.bind.AbstractPromiseAnnotation#getRules()
   */
  @Override
  protected IPromiseRule[] getRules() {
    return new IPromiseRule[] {
        // otherwise, these get checked twice
        new Effects_ParseRule(READS_TAG, true),
        new Effects_ParseRule("Writes", false), };
  }

  class Effects_ParseRule extends AbstractPromiseParserCheckRule<IRSequence<IRNode>> {
    final boolean check;

    Effects_ParseRule(String tag, boolean check) {
      super(tag, SEQ, true, functionDeclOps, functionDeclOps);
      this.check = check;
    }

    public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) {
      effectsSI = si;
      return new TokenInfo<IRSequence<IRNode>>("Declared effects", si, "effects");
    }

    @Override
    protected boolean processResult(final IRNode n, final IRNode result,
        IPromiseParsedCallback cb, Collection<IRNode> newResults) {
      final boolean isWrite = !name.equals(READS_TAG);
      final Iterator<IRNode> e = EffectsSpecification.getEffectIterator(result);

      if (e.hasNext()) {
        // There are effects to add, so just add them in
        do {
          IRNode effect = e.next();

          // Fix up if implicit this and static region
          IRNode context = EffectSpecification.getContext(effect);
          Operator cop = tree.getOperator(context);
          if (ThisExpression.prototype.includes(cop)) {
            IRNode r = EffectSpecification.getRegion(effect);
            if (RegionName.prototype.includes(r)) {
              // check if exists
              String name = RegionName.getId(r);
              IRNode type = VisitUtil.getEnclosingType(n);
              IRNode region = RegionAnnotation.getInstance().findRegion(type,
                  name);
              if (region != null
                  && JavaNode.getModifier(region, JavaNode.STATIC)) {
                // Fix up
                String typeName = JavaNames.getFullTypeName(type);
                IRNode newContext = CogenUtil.createTypeE(typeName);
                EffectSpecification.setContext(effect, newContext);
              }
            }
          }

          JavaPromise.setIsWrite(effect, isWrite);
          addMethodEffect(n, effect);
          newResults.add(effect);
        } while (e.hasNext());
      } else {
        // No effects ("nothing"), so need to clear old ones
        final Iterator<IRNode> effects = methodEffects(n);

        if (effects == null) {
          // LOG.debug("No effects yet, setting them to empty");
          setMethodEffects(n, new EmptyIterator<IRNode>());
        } else {
          // LOG.debug("Some effects already, clearing out the " + keyword + "
          // effects");
          int total = 0;
          int removed = 0;

          while (effects.hasNext()) {
            IRNode effect = effects.next();
            total++;

            if (JavaPromise.isWrite(effect) == isWrite) {
              removeMethodEffect(n, effect);
              removed++;
            }
          }
          // LOG.debug("Cleared out "+removed+" '"+keyword+"' from "+total+"
          // effects");
        }
      }
      getMethodEffectsDrop(n); // create the drop

      return true;
    }

    /**
     * @see edu.cmu.cs.fluid.java.bind.IPromiseCheckRule#checkSanity(edu.cmu.cs.fluid.tree.Operator,
     *      edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public boolean checkSanity(Operator op, IRNode promisedFor,
        IPromiseCheckReport report) {
      /* Read and write effects are separate promises, but get stored in the
       * same structure, so we only check for read effects.
       */ 
      if (!check) {
        return true;
      }
      final boolean isConstructor =
        ConstructorDeclaration.prototype.includes(op);
      final boolean isStatic = TypeUtil.isStatic(promisedFor);
      final Iterator<IRNode> promisedEffects = methodEffects(promisedFor);
      boolean good = true;
      if (promisedEffects != null) {
        while (promisedEffects.hasNext()) {
          final IRNode peff = promisedEffects.next();
          final IRNode preg = EffectSpecification.getRegion(peff);

          /*
           * Check that the regions exist.
           */
          if (binder.getBinding(preg) == null) {
            report.reportError("Could not find region for @"
                + DebugUnparser.toString(peff), peff);
            good = false;
            continue;
          }

          /*
           * Check that constructors don't declare instance effects, They are
           * meaningless, and cause problems later on.
           */
          final IRNode pcon = EffectSpecification.getContext(peff);
          if (isConstructor) {
            if (ThisExpression.prototype.includes(getOperator(pcon))) {
              report.reportError("Constructor "
                  + JavaNames.genMethodConstructorName(promisedFor)
                  + " should not be reporting any effects on region "
                  + RegionName.getId(preg) + " of the newly created object",
                  peff);
              good = false;
              continue;
            }
          } else if (isStatic) {
            /*
             * Make sure "this." regions only appear on instance methods
             */
            if (ThisExpression.prototype.includes(getOperator(pcon))) {
              report.reportError("Static method "
                  + JavaNames.genMethodConstructorName(promisedFor)
                  + " does not have an associated 'this' object", peff);
              good = false;
              continue;
            }
          }
        }
      }

      /* This is redundnat, and I should find a better way of generating
       * the label.
       */
      final List<Effect> declFx = 
        Effects.getDeclaredMethodEffects(promisedFor, promisedFor);
      
      MethodEffectsPromiseDrop med = getMethodEffectsDrop(promisedFor);
      if (med != null) {
        if (good && declFx != null) {
          String modelName = genModelName(promisedFor, declFx);
          med.setMessage(modelName);
          med.setNodeAndCompilationUnitDependency(promisedFor);
          return true;
        } else {
          med.invalidate();
          return false;
        }
      } else {
        return false;
      }
    }
  }
}