/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/IColorAcceptor.java,v 1.2 2007/07/09 14:08:29 chance Exp $*/
package edu.cmu.cs.fluid.java.analysis;


@Deprecated
public interface IColorAcceptor {
  public void accept(
      IColorVisitor visitor);
}
