/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/parse/ParseUtil.java,v 1.1 2007/08/14 15:15:24 chance Exp $*/
package com.surelogic.annotation.parse;

import com.surelogic.parse.ASTFactory;

import edu.cmu.cs.fluid.ir.IRNode;

public class ParseUtil {
  static boolean isType(IRNode context, String prefix, String id) {
    System.out.println(prefix+'.'+id);    
    return ParseHelper.getInstance().getStatus(context, prefix, id) != null;
  }
  
  private static boolean initialized = false;
  
  public static void init() {
	  if (initialized) {
		  return;
	  }
	  ASTFactory.getInstance();
	  ScopedPromisesLexer.init();
	  SLAnnotationsLexer.init();
	  SLThreadRoleAnnotationsLexer.init();
	  LayerPromisesLexer.init();
	  initialized = true;
  }
  
  public static void clear() {
	  // TODO Disabled for now, due to use by XML editor
	  initialized = false;
	  ScopedPromisesLexer.clear();
	  SLAnnotationsLexer.clear();
	  SLThreadRoleAnnotationsLexer.clear();
	  LayerPromisesLexer.clear();
  }
}
