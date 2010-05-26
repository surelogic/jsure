package edu.cmu.cs.fluid.java.bind;

import com.surelogic.annotation.rules.AnnotationRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.promise.IPromiseParsedCallback;
import edu.cmu.cs.fluid.promise.IPromiseRule;
import edu.cmu.cs.fluid.sea.drops.promises.StartsPromiseDrop;

@Deprecated
public class StartsAnnotation extends AbstractPromiseAnnotation {

  private StartsAnnotation() {
    // private constructor for singleton creation
    AnnotationRules.initialize();
  }

  private static final StartsAnnotation INSTANCE = new StartsAnnotation();

  public static final StartsAnnotation getInstance() {
    return INSTANCE;
  }

  private static SlotInfo<IRNode> startsSI;

  private static SlotInfo<StartsPromiseDrop> startsSIDrop = makeDropSlotInfo("Starts");

  @Override
  protected IPromiseRule[] getRules() {
    return new IPromiseRule[] { new Starts_ParseRule(), };
  }

  public static IRNode getStartsSpec(IRNode declNode) {
    return getXorNull_filtered(startsSI, declNode);
  }

  public static boolean startsNothing(IRNode declNode) {
    return (null != getXorNull_filtered(startsSI, declNode));
  }

  public static void setStartsSpec(IRNode declNode, IRNode nothing) {
    setX_mapped(startsSI, declNode, nothing);
    getStartsDrop(declNode); // causes drop creation
  }

  public static StartsPromiseDrop getStartsDrop(IRNode declNode) {
    StartsPromiseDrop drop = declNode.getSlotValue(startsSIDrop);
    if (drop != null) {
      return drop;
    } else {
      // if it should exist, create it
      if (startsNothing(declNode)) {
        // Create a promise drop for this promise annotation
        drop = new StartsPromiseDrop(null);
        drop.setCategory(JavaGlobals.THREAD_EFFECTS_CAT);
        drop.setNode(getStartsSpec(declNode));
        drop.dependUponCompilationUnitOf(declNode);
        /*
        drop.setMessage(Messages.StartsAnnotation_startNothingDrop, JavaNames
            .genMethodConstructorName(declNode));
            */
        declNode.setSlotValue(startsSIDrop, drop);
        drop.setAttachedTo(declNode, startsSIDrop);
        return drop;
      } else
        return null;
    }
  }

  /*****************************************************************************
   * Parse rule for "starts"
   ****************************************************************************/
  static class Starts_ParseRule extends AbstractPromiseParserCheckRule<IRNode> {

    public Starts_ParseRule() {
      super("Starts", NODE, false, methodDeclOps, methodDeclOps); //$NON-NLS-1$
    }

    public TokenInfo<IRNode> set(SlotInfo<IRNode> si) {
      startsSI = si;
      return new TokenInfo<IRNode>("Starts", si, "starts"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected boolean processResult(final IRNode n, final IRNode result,
        IPromiseParsedCallback cb) {
      setStartsSpec(n, result);
      return true;
    }
  }
}