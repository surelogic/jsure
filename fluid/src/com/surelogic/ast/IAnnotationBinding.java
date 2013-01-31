// $Header$
package com.surelogic.ast;

import com.surelogic.ast.java.operator.*;

public interface IAnnotationBinding extends IBinding {
  public String getName();

  /**   
   * @return null if no corresponding source declaration   
   */  
  @Override
  public IAnnotationDeclarationNode getNode();
}
