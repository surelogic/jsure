/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/ICompUnitListener.java,v 1.2 2007/10/18 21:17:55 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.*;

public interface ICompUnitListener {
  // void astDeclared(IRNode cu);
  void astChanged(IRNode cu);
  void astsChanged();
}
