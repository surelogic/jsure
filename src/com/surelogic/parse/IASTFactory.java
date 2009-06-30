/*$Header: /cvs/fluid/fluid/src/com/surelogic/parse/IASTFactory.java,v 1.3 2007/07/10 22:16:32 aarong Exp $*/
package com.surelogic.parse;

import java.util.List;

import com.surelogic.aast.AASTNode;

public interface IASTFactory {
  String NIL = "null";
  
  AASTNode create(String token, int start, int stop, 
                  int mods, String id, int dims, List<AASTNode> kids);
  
  IASTFactory registerFactory(String token, IASTFactory f);
  
  boolean handles(String token);
}
