/*$Header$*/
package com.surelogic.ast;

import com.surelogic.ast.java.operator.*;

/**
 * Not for use with AASTs
 * @author chance
 */
public interface IBinding {
  /**   
   * @return null if no corresponding source declaration   
   */  
  IDeclarationNode getNode();
}
