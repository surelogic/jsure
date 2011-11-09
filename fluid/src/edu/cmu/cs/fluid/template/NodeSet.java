/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/template/NodeSet.java,v 1.4 2003/07/02 20:19:39 thallora Exp $ */
package edu.cmu.cs.fluid.template;

import java.util.Enumeration;
import java.util.Iterator;

import edu.cmu.cs.fluid.util.EmptyEnumeration;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.SetException;

/**
 * Stores a set of related nodes and a message describing why the nodes
 * are interesting.  This is used by Templates to return information 
 * in {@link TemplateEvent} messages.  For example, they can be 
 * used to indicate that a set of nodes representing Java expressions
 * have some property that prevents the Template from executing.
 */
public class NodeSet
{
  /** A message that describes why the nodes are interesting. */
  private final String message;
  /** The set of nodes */
  private final ImmutableHashOrderSet nodes;

  /**
   * Create a new NodeSet.
   * @param msg Message describing why the nodes are interesting.
   * @param nodes The set of nodes.
   */
  public NodeSet( final String msg, final ImmutableHashOrderSet nodes )
  {
    message = msg;
    this.nodes = nodes;
  }

  /**
   * Get the description of the nodes.
   */
  public String getMessage()
  {
    return message;
  }

  /**
   * Get the nodes.
   */
  public ImmutableHashOrderSet getNodes()
  {
    return nodes;
  }

  /**
   * Get an enumeration of the nodes.
   */
  public Enumeration elements()
  {
    try {      
      return nodes.elements();
    } catch( SetException e ) {
      return EmptyEnumeration.prototype;
    }
  }

  /**
   * Get an iterator over the nodes.
   */
  public Iterator iterator()
  {
    return nodes.iterator();
  }
}
