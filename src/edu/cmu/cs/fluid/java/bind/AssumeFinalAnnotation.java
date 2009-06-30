package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.promise.parse.*;
import edu.cmu.cs.fluid.sea.drops.promises.AssumeFinalPromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;

public class AssumeFinalAnnotation extends AbstractPromiseAnnotation {

  static SlotInfo<Boolean> assumeFinalSI;

  private static DefaultDropFactory<AssumeFinalPromiseDrop> assumeFinalSIFactory = 
    new DefaultDropFactory<AssumeFinalPromiseDrop>("AssumeFinal") {
    @Override
    public AssumeFinalPromiseDrop newDrop(IRNode node, Object val) {
      if (isAssumeFinal(node)) {
        AssumeFinalPromiseDrop drop = new AssumeFinalPromiseDrop(null);
        drop.setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
        drop.setMessage(Messages.AssumeFinalAnnotation_finalFieldDrop, JavaNames.getFieldDecl(node));
        return drop;
      }
      return null;
    }
  };

  private AssumeFinalAnnotation() {
  }

  private static final AssumeFinalAnnotation instance = new AssumeFinalAnnotation();

  public static final AssumeFinalAnnotation getInstance() {
    return instance;
  }

  public static boolean isAssumeFinal(IRNode node) {
    return isX_filtered(assumeFinalSI, node);
  }

  public static AssumeFinalPromiseDrop getAssumeFinalDrop(IRNode node) {
    return getDrop(assumeFinalSIFactory, node);
  }

  public static void setAssumeFinal(IRNode node, boolean assumeFinal) {
    setX_mapped(assumeFinalSI, node, assumeFinal);
    getAssumeFinalDrop(node);
  }

  /**
   * @see edu.cmu.cs.fluid.java.bind.AbstractPromiseAnnotation#getRules()
   */
  @Override
  protected IPromiseRule[] getRules() {
    return new IPromiseRule[] {
        new AbstractPromiseStorageAndCheckRule<Boolean>("AssumeFinal",
            IPromiseStorage.BOOL, varDeclaratorOps, varDeclaratorOps) {

          public TokenInfo<Boolean> set(SlotInfo<Boolean> si) {
            assumeFinalSI = si;
            return new TokenInfo<Boolean>("AssumeFinal", si, name);
          }

          @Override
          public boolean checkSanity(Operator op, IRNode promisedFor,
              IPromiseCheckReport report) {
            // invoke lazy creation of drops (if they don't exist
            // already...these
            // calls will create them)
            getAssumeFinalDrop(promisedFor);
            return true;
          }
        }, new BooleanFieldRule("AssumeFinal") {

          @Override
          protected SlotInfo<Boolean> getSI() {
            return assumeFinalSI;
          }

          @Override
          protected void parsedSuccessfully(IRNode decl) {
            getAssumeFinalDrop(decl);
          }
        }, };
  }
}
