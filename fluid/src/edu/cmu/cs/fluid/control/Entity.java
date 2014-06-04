/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/Entity.java,v 1.2 2003/07/02 20:19:22 thallora Exp $ */
package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.AbstractIRNode;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;

/** Control-flow graph nodes (not persistable). */
class Entity extends AbstractIRNode {
  public Entity() { 
	super();
  }
  
  @Override
  public String toString() {
	  String cs = classToString();
	  String is = infoToString(); 
	  if (is == null) return cs;
	  return cs + " " + is;
  }
  
  protected String classToString() {
	  String cn = getClass().toString();
	  return cn.substring(cn.lastIndexOf('.')+1);
  }
  
  protected String infoToString() {
	  if (this instanceof ComponentNode) {
		  IRNode syntax;
		  if (this instanceof SubcomponentNode) {
			  syntax = ((SubcomponentNode)this).getSubcomponent().getSyntax();
		  } else {
			  syntax = ((ComponentNode)this).getComponent().getSyntax();
		  }
		  return "for " + DebugUnparser.toString(syntax);
	  }
	  return null;
  }
  
  /** Default implementation of {@link Port#which()}
   * @return kind of port using interface implementation.
   */
  public WhichPort which() {
	  if (this instanceof EntryPort) return WhichPort.ENTRY;
	  if (this instanceof AbruptExitPort) return WhichPort.ABRUPT_EXIT;
	  if (this instanceof ExitPort) return WhichPort.NORMAL_EXIT;
	  return null;
  }

}
