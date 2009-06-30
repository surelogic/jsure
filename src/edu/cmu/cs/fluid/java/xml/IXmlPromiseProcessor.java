/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/xml/IXmlPromiseProcessor.java,v 1.2 2007/09/05 19:44:47 chance Exp $*/
package edu.cmu.cs.fluid.java.xml;

import edu.cmu.cs.fluid.ir.IRNode;

public interface IXmlPromiseProcessor {
  void init();
  void registerXML(String name);
  void process(IRNode root);
  boolean processPackage(IRNode pkg, String name);
  void unregisterXML(String name);
}
