/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/parse/ParseUtil.java,v 1.1 2007/08/14 15:15:24 chance Exp $*/
package com.surelogic.annotation.parse;

import edu.cmu.cs.fluid.ir.IRNode;

public class ParseUtil {
  static boolean isType(IRNode context, String prefix, String id) {
    System.out.println(prefix+'.'+id);    
    return ParseHelper.getInstance().getStatus(context, prefix, id) != null;
  }
}
