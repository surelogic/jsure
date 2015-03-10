/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/parse/ParseHelper.java,v 1.3 2007/08/28 19:49:17 chance Exp $*/
package com.surelogic.annotation.parse;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaClassTable;

/**
 * Encapsulates info needed to compute semantic predicates for
 * the new promise parser.  
 * 
 * Note that it can be heuristic, because the resulting AASTs are checked later, 
 * so it is ok to combine information from different classpaths.
 * 
 * @author Edwin.Chan
 */
public class ParseHelper {
  private static final ParseHelper prototype = new ParseHelper();
  
  private ParseHelper() { 
    // nothing to do 
  }
  
  public static ParseHelper getInstance() {
    return prototype;
  }
  
  private final Map<String,ParseStatus> cache = new HashMap<String,ParseStatus>();
  private boolean uninitialized = true;;
  
  public void initialize(IJavaClassTable classes) {
    uninitialized = false;
    
    for(String name : classes.allNames()) {
      if (cache.containsKey(name)) {
        continue;
      }      
      int i = name.indexOf('.');
      while (i >= 0) {
        String prefix = name.substring(0, i);
        cache.put(prefix, ParseStatus.MAYBE_PKG);
        //System.out.println(prefix);
        
        i = name.indexOf('.', i+1);
      }
      //System.out.println(name);
      int lastDot = name.lastIndexOf('.');
      if (lastDot > 0) {
    	  cache.put(name.substring(lastDot+1), ParseStatus.TYPE);
      }
      cache.put(name, ParseStatus.TYPE);
    }
  }
  
  ParseStatus getStatus(IRNode context, String prefix, String id) {
    if (uninitialized ) {
      return ParseStatus.TYPE;
    }
    String key    = prefix.length() == 0 ? id : prefix+'.'+id;
    ParseStatus s = cache.get(key);   
    return s;
  }
  
  public void clearCache() {
	  cache.clear();
  }
}
