/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/IColorAcceptor.java,v 1.2 2007/07/09 13:39:26 chance Exp $*/
package com.surelogic.analysis.colors;



public interface IColorAcceptor {
  public void accept(
      IColorVisitor visitor);
}
