/*
 * Created on Oct 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.sea.drops.colors;

import java.util.*;

import com.surelogic.aast.promise.ColorIncompatibleNode;
import com.surelogic.analysis.colors.*;

import SableJBDD.bdd.JBDD;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.Drop;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ColorIncompatibleDrop extends ColorNameListDrop<ColorIncompatibleNode> {
  private static final String myKind = "incompatibleColors";
//  private List tcList = null;
  
  private JBDD conflictExpr = null;
  
  
  public ColorIncompatibleDrop(ColorIncompatibleNode cin) {
    // note that super... takes care of the ColorAnnoMap for us...
    super(cin, myKind);
  }
  
   /**
   * @return Collection holding the String representation of the names that are
   * incompatible.
   */
  public Collection<String> getIncompatibleNames() {
    return getListedColors();
  }
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.Drop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    if (invalidDeponent instanceof ColorSummaryDrop) {
      return;
    }
    // Any IncompatibleDrop specific action would go here.
    // note that super... takes care of the ColorAnnoMap for us...
    super.deponentInvalidAction(invalidDeponent);
    
  }
  
  
  /**
   * @return Returns the conflictExpr.
   */
  public JBDD getConflictExpr() {
    if (conflictExpr == null) {
      final IRNode cu = getDeclContext();
      Collection<String> incNamesColl = getIncompatibleNames();
      String[] incNames = new String[incNamesColl.size()];
      incNames = incNamesColl.toArray(incNames);
      conflictExpr = ColorBDDPack.zero();
      
      for (int i=0; i<incNames.length; i++) {
        JBDD inner = ColorBDDPack.one();
        for (int j=0; j<incNames.length; j++) {
          
          final String aName = incNames[j];
          final ColorNameModel model = ColorNameModel.getInstance(aName, cu);
          final TColor aTC = model.getCanonicalTColor();
          
          if (i == j) {
            inner.andWith(aTC.getSelfExpr());
          } else {
            inner.andWith(aTC.getSelfExprNeg());
          }
        }
        conflictExpr.orWith(inner);
      }
      
      // don't forget the none-of-the-above case!
      JBDD inner = ColorBDDPack.one();
      for (int i=0; i<incNames.length; i++) {
        
        
        final String aName = incNames[i];
        final ColorNameModel model = ColorNameModel.getInstance(aName, cu);
        final TColor aTC = model.getCanonicalTColor();
        inner.andWith(aTC.getSelfExprNeg());
      }
      conflictExpr.orWith(inner);
    }
    return conflictExpr;
  }
}
