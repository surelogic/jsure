/*$Header: /cvs/fluid/fluid/src/com/surelogic/parse/IASTFactory.java,v 1.3 2007/07/10 22:16:32 aarong Exp $*/
package com.surelogic.parse;

import java.util.List;

public interface IASTFactory<T> {
  String NIL = "null";
  
  /**
   * @param start The absolute offset into the text
   * @param stop
   */
  T create(String token, int start, int stop, 
                  int mods, String id, int dims, List<T> kids);
  
  IASTFactory<T> registerFactory(String token, IASTFactory<T> f);
  
  boolean handles(String token);
}
