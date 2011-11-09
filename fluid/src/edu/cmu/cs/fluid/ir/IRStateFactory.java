/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRStateFactory.java,v 1.1 2007/11/05 15:03:56 chance Exp $*/
package edu.cmu.cs.fluid.ir;

public interface IRStateFactory {
  @SuppressWarnings("unchecked")
  IRState create();
}
