/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStaticBlockish.java,v 1.3 2007/07/09 14:00:11 chance Exp $*/
package com.surelogic.analysis.threadroles;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;


public abstract class TRoleStaticBlockish extends TRoleStaticWithChildren {
  final List<TRoleStaticRef> allRefs;
  final List<TRoleStaticRef> interestingRefs;

  public TRoleStaticBlockish(final IRNode node, 
                             final TRoleStaticWithChildren parent) {
    super(node, parent);
    allRefs = new ArrayList<TRoleStaticRef>(4);
    interestingRefs = new ArrayList<TRoleStaticRef>(0);
  }
}
