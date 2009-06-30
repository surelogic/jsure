/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStaticBlockish.java,v 1.3 2007/07/09 14:00:11 chance Exp $*/
package com.surelogic.analysis.colors;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;


public abstract class ColorStaticBlockish extends ColorStaticWithChildren {
  final List<ColorStaticRef> allRefs;
  final List<ColorStaticRef> interestingRefs;

  public ColorStaticBlockish(final IRNode node, 
                             final ColorStaticWithChildren parent) {
    super(node, parent);
    allRefs = new ArrayList<ColorStaticRef>(4);
    interestingRefs = new ArrayList<ColorStaticRef>(0);
  }
}
