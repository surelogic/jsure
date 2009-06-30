/*
 * Created on Dec 4, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.java.analysis;

import java.util.Collection;
import java.util.Iterator;

import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.DropEvent;
import edu.cmu.cs.fluid.sea.DropObserver;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.drops.promises.*;

/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
@Deprecated
public class ColorStats implements DropObserver {
  
  private static final ColorStats INSTANCE = new ColorStats();
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
  
  
  public StringBuilder beforeCfp = null;
  public StringBuilder afterCfp = null;
  public StringBuilder afterCsp = null;
  
  private ColorStats() {
    final Sea theSea = Sea.getDefault();
     theSea.register(ColorContextDrop.class, this);
     theSea.register(ColorRequireDrop.class,this);
     theSea.register(ColorCtxSummaryDrop.class,this);
     theSea.register(ColorReqSummaryDrop.class,this);
     theSea.register(ColorDeclareDrop.class,this);
     theSea.register(ColorIncompatibleDrop.class,this);
     theSea.register(ColorRevokeDrop.class,this);
     theSea.register(ColorGrantDrop.class,this);
  }
  
  public static final ColorStats getInstance() {
    return INSTANCE;
  }
  
  
  public static final void resetForAFullBuild() {
//    INSTANCE.numContext = 0;
//    INSTANCE.numContextFromSrc = 0;
//    INSTANCE.numRequire = 0;
//    INSTANCE.numRequireFromSrc = 0;
//    INSTANCE.numCtxSummary = 0;
//    INSTANCE.numReqSummary = 0;
//    INSTANCE.numDeclare = 0;
//    INSTANCE.numDeclareFromSrc = 0;
//    INSTANCE.numIncompatible = 0;
//    INSTANCE.numIncompatibleFromSrc = 0;
//    INSTANCE.numRevoke = 0;
//    INSTANCE.numRevokeFromSrc = 0;
//    INSTANCE.numGrant = 0;
//    INSTANCE.numGrantFromSrc = 0;
//    INSTANCE.numNameModel = 0;
//    INSTANCE.numNameModelFromSrc = 0;
  }
  
  public void dropChanged(Drop drop, DropEvent event) {
	  
	if (event == DropEvent.Created) {
      if (drop instanceof ColorContextDrop) {
        numContext += 1;
        if (((ColorContextDrop) drop).isFromSrc()) numContextFromSrc += 1;
      } else if (drop instanceof ColorRequireDrop) {
        numRequire += 1;
        if (((ColorRequireDrop) drop).isFromSrc()) numRequireFromSrc += 1;
      } else if (drop instanceof ColorCtxSummaryDrop) {
        numCtxSummary += 1;
      } else if (drop instanceof ColorReqSummaryDrop) {
        numReqSummary += 1;
      } else if (drop instanceof ColorDeclareDrop) {
        numDeclare += 1;
        if (((ColorDeclareDrop) drop).isFromSrc()) numDeclareFromSrc += 1;
      } else if (drop instanceof ColorIncompatibleDrop) {
        numIncompatible += 1;
        if (((ColorIncompatibleDrop) drop).isFromSrc()) numIncompatibleFromSrc += 1;
      } else if (drop instanceof ColorGrantDrop) {
        numGrant += 1;
        if (((ColorGrantDrop) drop).isFromSrc()) numGrantFromSrc += 1;
      } else if (drop instanceof ColorRevokeDrop) {
        numRevoke += 1;
        if (((ColorRevokeDrop) drop).isFromSrc()) numRevokeFromSrc += 1;
      }
	} else if (event == DropEvent.Invalidated) {
      if (drop instanceof ColorContextDrop) {
        numContext -= 1;
        if (((ColorContextDrop) drop).isFromSrc()) numContextFromSrc -= 1;
      } else if (drop instanceof ColorRequireDrop) {
        numRequire -= 1;
        if (((ColorRequireDrop) drop).isFromSrc()) numRequireFromSrc -= 1;
      } else if (drop instanceof ColorCtxSummaryDrop) {
        numCtxSummary -= 1;
      } else if (drop instanceof ColorReqSummaryDrop) {
        numReqSummary -= 1;
      } else if (drop instanceof ColorDeclareDrop) {
        numDeclare -= 1;
        if (((ColorDeclareDrop) drop).isFromSrc()) numDeclareFromSrc -= 1;
      } else if (drop instanceof ColorIncompatibleDrop) {
        numIncompatible -= 1;
        if (((ColorIncompatibleDrop) drop).isFromSrc()) numIncompatibleFromSrc -= 1;
      } else if (drop instanceof ColorGrantDrop) {
        numGrant -= 1;
        if (((ColorGrantDrop) drop).isFromSrc()) numGrantFromSrc -= 1;
      } else if (drop instanceof ColorRevokeDrop) {
        numRevoke -= 1;
        if (((ColorRevokeDrop) drop).isFromSrc()) numRevokeFromSrc -= 1;
      }
    }
    
  }
  public StringBuilder getColorStats(String when) {
    StringBuilder res = new StringBuilder(when + " ");
    
    res.append("Color Statistics:\n");
    res.append("Dropkind, Total Number, FromSrc Number\n");
    // --------------------------------------------------
    // compute and report the number of color Name models
    // --------------------------------------------------
    Collection<ColorNameModel> nModels = ColorNameModel.getAllValidColorNameModels();
    numNameModel = 0;
    numNameModelFromSrc = 0;
    for (Iterator<ColorNameModel> nmIter = nModels.iterator(); nmIter.hasNext();) {
      ColorNameModel aName = nmIter.next();
      
      numNameModel += 1;
      if (aName.isFromSrc()) {
        numNameModelFromSrc += 1;
      }
    }
    res.append("ColorNameModel, " + Integer.toString(numNameModel) + ", " + Integer.toString(numNameModelFromSrc) + "\n");
    res.append("ColorContext, " + Integer.toString(numContext) + ", " + Integer.toString(numContextFromSrc) + "\n");
    res.append("ColorDeclare, " + Integer.toString(numDeclare) + ", " + Integer.toString(numDeclareFromSrc) + "\n");
    res.append("ColorGrant, " + Integer.toString(numGrant) + ", " + Integer.toString(numGrantFromSrc) + "\n");
    res.append("ColorIncompatible, " + Integer.toString(numIncompatible) + ", " + Integer.toString(numIncompatibleFromSrc) + "\n");
    res.append("ColorRequire, " + Integer.toString(numRequire) + ", " + Integer.toString(numRequireFromSrc) + "\n");
    res.append("ColorRevoke, " + Integer.toString(numRevoke) + ", " + Integer.toString(numRevokeFromSrc) + "\n");
    res.append("ColorCtxSummary, " + Integer.toString(numCtxSummary) + ", -1\n");
    res.append("ColorReqSummary, " + Integer.toString(numReqSummary) + ", -1\n");

    SimpleCallGraphDrop.CGStats cgStats = SimpleCallGraphDrop.getStats();
    res.append("\n\n");
    res.append("numCallGraphDrops, numWithCallers, numWithCallees, numAPI, numAPInoCallers, numAPInoCallees\n");
    res.append(cgStats.numDrops + ", " + cgStats.numWithCallers + ", ");
    res.append(cgStats.numWithCallees + ", " + cgStats.numAPI +", ");
    res.append(cgStats.numAPInoCallers + ", " + cgStats.numAPInoCallees + "\n");
    
    
    return res;
  }
  
}


