/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.persistence;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.irfree.XmlCreator;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class AbstractAnalysisResult implements IAnalysisResult, PersistenceConstants {
  private final PromiseRef about;
  private final IRNode location; // TODO how to specify within a CU

  public <T extends IAASTRootNode> AbstractAnalysisResult(PromiseDrop<T> d, IRNode loc) {
    about = new PromiseRef(d);
    location = loc;
  }

  public void outputToXML(JSureResultsXMLCreator creator, XmlCreator.Builder outer) {
    XmlCreator.Builder b = outer.nest(RESULT);
    attributesToXML(b);
    about.toXML(b.nest(ABOUT_REF));

    subEntitiesToXML(b);
    b.end();
    // return sb.toString();
  }

  protected void attributesToXML(XmlCreator.Builder sb) {
    // Nothing right now
  }

  protected void subEntitiesToXML(XmlCreator.Builder sb) {
    // Nothing right now
  }
}
