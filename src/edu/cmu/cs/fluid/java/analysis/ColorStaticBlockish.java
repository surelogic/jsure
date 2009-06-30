/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/ColorStaticBlockish.java,v 1.3 2007/07/10 22:16:29 aarong Exp $*/
package edu.cmu.cs.fluid.java.analysis;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;

@Deprecated
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
