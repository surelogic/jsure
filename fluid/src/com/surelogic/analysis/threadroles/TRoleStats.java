/*
 * Created on Dec 4, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.analysis.threadroles;

import java.util.Collection;
import java.util.Iterator;

import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.DropEvent;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleCtxSummaryDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleDeclareDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleGrantDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleIncompatibleDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleNameModel;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleReqSummaryDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleRequireDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleRevokeDrop;

/**
 * @author dfsuther
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Generation - Code and Comments
 *         
 *         THIS IS BUSTED -- NO MORE DROP OBSERVERS ALLOWED
 */
public class TRoleStats {

  private static final TRoleStats INSTANCE = new TRoleStats();
  private boolean registered = false;

  private int numContext = 0;
  private int numContextFromSrc = 0;
  private int numRequire = 0;
  private int numRequireFromSrc = 0;
  private int numCtxSummary = 0;
  private int numReqSummary = 0;
  private int numDeclare = 0;
  private int numDeclareFromSrc = 0;
  private int numIncompatible = 0;
  private int numIncompatibleFromSrc = 0;
  private int numRevoke = 0;
  private int numRevokeFromSrc = 0;
  private int numGrant = 0;
  private int numGrantFromSrc = 0;
  private int numNameModel = 0;
  private int numNameModelFromSrc = 0;

  public StringBuilder beforeTRfp = null;
  public StringBuilder afterTRfp = null;
  public StringBuilder afterTRsp = null;

  private TRoleStats() {
    final Sea theSea = Sea.getDefault();
    // theSea.register(ColorContextDrop.class, this);
    // theSea.register(TRoleRequireDrop.class,this);
    // theSea.register(ColorCtxSummaryDrop.class,this);
    // theSea.register(TRoleReqSummaryDrop.class,this);
    // theSea.register(TRoleDeclareDrop.class,this);
    // theSea.register(TRoleIncompatibleDrop.class,this);
    // theSea.register(TRoleRevokeDrop.class,this);
    // theSea.register(TRoleGrantDrop.class,this);
  }

  public static final TRoleStats getInstance() {
    return INSTANCE;
  }

  public static final void resetForAFullBuild() {
    // INSTANCE.numContext = 0;
    // INSTANCE.numContextFromSrc = 0;
    // INSTANCE.numRequire = 0;
    // INSTANCE.numRequireFromSrc = 0;
    // INSTANCE.numCtxSummary = 0;
    // INSTANCE.numReqSummary = 0;
    // INSTANCE.numDeclare = 0;
    // INSTANCE.numDeclareFromSrc = 0;
    // INSTANCE.numIncompatible = 0;
    // INSTANCE.numIncompatibleFromSrc = 0;
    // INSTANCE.numRevoke = 0;
    // INSTANCE.numRevokeFromSrc = 0;
    // INSTANCE.numGrant = 0;
    // INSTANCE.numGrantFromSrc = 0;
    // INSTANCE.numNameModel = 0;
    // INSTANCE.numNameModelFromSrc = 0;
  }

  public void dropChanged(Drop drop, DropEvent event) {

    if (event == DropEvent.Created) {
      // if (drop instanceof ColorContextDrop) {
      // numContext += 1;
      // if (((ColorContextDrop) drop).isFromSrc()) numContextFromSrc += 1;
      // } else
      if (drop instanceof TRoleRequireDrop) {
        numRequire += 1;
        if (((TRoleRequireDrop) drop).isFromSrc())
          numRequireFromSrc += 1;
      } else if (drop instanceof TRoleCtxSummaryDrop) {
        numCtxSummary += 1;
      } else if (drop instanceof TRoleReqSummaryDrop) {
        numReqSummary += 1;
      } else if (drop instanceof TRoleDeclareDrop) {
        numDeclare += 1;
        if (((TRoleDeclareDrop) drop).isFromSrc())
          numDeclareFromSrc += 1;
      } else if (drop instanceof TRoleIncompatibleDrop) {
        numIncompatible += 1;
        if (((TRoleIncompatibleDrop) drop).isFromSrc())
          numIncompatibleFromSrc += 1;
      } else if (drop instanceof TRoleGrantDrop) {
        numGrant += 1;
        if (((TRoleGrantDrop) drop).isFromSrc())
          numGrantFromSrc += 1;
      } else if (drop instanceof TRoleRevokeDrop) {
        numRevoke += 1;
        if (((TRoleRevokeDrop) drop).isFromSrc())
          numRevokeFromSrc += 1;
      }
    } else if (event == DropEvent.Invalidated) {
      // if (drop instanceof ColorContextDrop) {
      // numContext -= 1;
      // if (((ColorContextDrop) drop).isFromSrc()) numContextFromSrc -= 1;
      // } else
      if (drop instanceof TRoleRequireDrop) {
        numRequire -= 1;
        if (((TRoleRequireDrop) drop).isFromSrc())
          numRequireFromSrc -= 1;
      } else if (drop instanceof TRoleCtxSummaryDrop) {
        numCtxSummary -= 1;
      } else if (drop instanceof TRoleReqSummaryDrop) {
        numReqSummary -= 1;
      } else if (drop instanceof TRoleDeclareDrop) {
        numDeclare -= 1;
        if (((TRoleDeclareDrop) drop).isFromSrc())
          numDeclareFromSrc -= 1;
      } else if (drop instanceof TRoleIncompatibleDrop) {
        numIncompatible -= 1;
        if (((TRoleIncompatibleDrop) drop).isFromSrc())
          numIncompatibleFromSrc -= 1;
      } else if (drop instanceof TRoleGrantDrop) {
        numGrant -= 1;
        if (((TRoleGrantDrop) drop).isFromSrc())
          numGrantFromSrc -= 1;
      } else if (drop instanceof TRoleRevokeDrop) {
        numRevoke -= 1;
        if (((TRoleRevokeDrop) drop).isFromSrc())
          numRevokeFromSrc -= 1;
      }
    }

  }

  public StringBuilder getTRoleStats(String when) {
    StringBuilder res = new StringBuilder(when + " ");

    res.append("TRole Statistics:\n");
    res.append("Dropkind, Total Number, FromSrc Number\n");
    // --------------------------------------------------
    // compute and report the number of color Name models
    // --------------------------------------------------
    Collection<TRoleNameModel> nModels = TRoleNameModel.getAllValidTRoleNameModels();
    numNameModel = 0;
    numNameModelFromSrc = 0;
    for (Iterator<TRoleNameModel> nmIter = nModels.iterator(); nmIter.hasNext();) {
      TRoleNameModel aName = nmIter.next();

      numNameModel += 1;
      if (aName.isFromSrc()) {
        numNameModelFromSrc += 1;
      }
    }
    res.append("TRoleNameModel, " + Integer.toString(numNameModel) + ", " + Integer.toString(numNameModelFromSrc) + "\n");
    res.append("TRoleContext, " + Integer.toString(numContext) + ", " + Integer.toString(numContextFromSrc) + "\n");
    res.append("TRoleDeclare, " + Integer.toString(numDeclare) + ", " + Integer.toString(numDeclareFromSrc) + "\n");
    res.append("TRoleGrant, " + Integer.toString(numGrant) + ", " + Integer.toString(numGrantFromSrc) + "\n");
    res.append("TRoleIncompatible, " + Integer.toString(numIncompatible) + ", " + Integer.toString(numIncompatibleFromSrc) + "\n");
    res.append("TRoleRequire, " + Integer.toString(numRequire) + ", " + Integer.toString(numRequireFromSrc) + "\n");
    res.append("TRoleRevoke, " + Integer.toString(numRevoke) + ", " + Integer.toString(numRevokeFromSrc) + "\n");
    res.append("TRoleCtxSummary, " + Integer.toString(numCtxSummary) + ", -1\n");
    res.append("TRoleReqSummary, " + Integer.toString(numReqSummary) + ", -1\n");

    /*
     * SimpleCallGraphDrop.CGStats cgStats = SimpleCallGraphDrop.getStats();
     * res.append("\n\n"); res.append(
     * "numCallGraphDrops, numWithCallers, numWithCallees, numAPI, numAPInoCallers, numAPInoCallees\n"
     * ); res.append(cgStats.numDrops + ", " + cgStats.numWithCallers + ", ");
     * res.append(cgStats.numWithCallees + ", " + cgStats.numAPI +", ");
     * res.append(cgStats.numAPInoCallers + ", " + cgStats.numAPInoCallees +
     * "\n");
     */

    return res;
  }

}
