/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRNodeViewer.java,v 1.1 2006/08/23 08:54:51 boyland Exp $*/
package edu.cmu.cs.fluid.ir;

/**
 * Make an IRNode understandable.
 * For instance, unparse it.
 * @author boyland
 */
public interface IRNodeViewer {
  public String toString(IRNode n);
  
  public static final IRNodeViewer defaultViewer = new IRNodeViewer() {
    public String toString(IRNode n) {
      return n.toString();
    }
  };
}
