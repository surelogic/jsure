/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/DigraphMixin.java,v 1.8 2005/05/25 16:29:21 chance Exp $ */
package edu.cmu.cs.fluid.tree;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.surelogic.ThreadSafe;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRObservable;
import edu.cmu.cs.fluid.ir.InsertionPoint;

/** A class that provides some common functions
 * for directed graphs.
 * It implements the listerner protocol and
 * also provides some default definitions for functions
 */
@ThreadSafe
abstract public class DigraphMixin extends IRObservable
     implements DigraphInterface 
{
  private final List<DigraphListener> listeners = new CopyOnWriteArrayList<DigraphListener>();

  public void addDigraphListener(DigraphListener dl) {
    listeners.add(dl);
  }
  public void removeDigraphListener(DigraphListener dl) {
    listeners.remove(dl);
  }

  protected boolean hasListeners() {
    return listeners.size() > 0;
  }
  
  protected void informDigraphListeners(DigraphEvent de) {
    for (int i=listeners.size(); i > 0; --i) {
      DigraphListener dl = listeners.get(i-1);
      if (dl != null) dl.handleDigraphEvent(de);
    }
  }

  /** Add new child as a new child of node at the given insertion point.
   *  @exception StructureException if newChild is not suitable
   *            or the parent cannot accept new children.
   * @return location of new child
   */
  public abstract IRLocation
    insertChild(IRNode node, IRNode newChild, InsertionPoint ip)
    throws StructureException;

  /** Add newChild as a new first child of node.
   * @exception IllegalChildException if newChild is not suitable
   *            or the parent cannot accept new children.
   */
  public void insertChild(IRNode node, IRNode newChild)
       throws IllegalChildException
  {
    insertChild(node,newChild,InsertionPoint.first);
  }

  /** Add newChild as a new last child of node.
   * @exception StructureException if newChild is not suitable
   *            or the parent cannot accept new children.
   */
  public void appendChild(IRNode node, IRNode newChild)
       throws StructureException
  {
    insertChild(node,newChild,InsertionPoint.last);
  }

}
