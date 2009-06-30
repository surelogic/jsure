/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/template/JavaNodeField.java,v 1.8 2005/06/30 20:36:48 chance Exp $ */
package edu.cmu.cs.fluid.java.template;

import edu.cmu.cs.fluid.template.*;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.version.VersionTracker;

/**
 * Field that holds a single IRNode.  The IRNode to be placed in the
 * field can be filtered by operator type.  The operators must
 * be specific to Java parse tree nodes.
 */
@Deprecated
@SuppressWarnings("all")
public class JavaNodeField
extends IRNodeField
{
  /**
   * Create a new field that accepts JavaNodes based on
   * the node's operator.
   * @param n The field's name.
   * @param t The Template the field belongs to.
   * @param vt The verion tracker to use to get the version that
   * the data should be from.
   * @param c The Field's consultant.
   * @param ops Array of operators to use as filters.  An IRNode
   * will only be accepted if it's operator type is a descendent
   * of an operator in the list.  If the list is <tt>null</tt>
   * then any IRNode will be accepted.
   * @exception IllegalArgumentException
   * Thrown if <code>c</code> is <code>null</code>.
   */
  public JavaNodeField( final String n, final Template t,
                        final VersionTracker vt,
                        final FieldConsultant c,
                        final Operator[] ops )
  {
    super( n, t, vt, c, new JavaNodeStrategy( ops ) );
  }

  /**
   * Create a new field that accepts JavaNodes based on
   * the node's operator.  Uses the templates version tracker
   * and the default consultant.
   * @param n The field's name.
   * @param t The Template the field belongs to.
   * @param ops Array of operators to use as filters.  An IRNode
   * will only be accepted if it's operator type is a descendent
   * of an operator in the list.  If the list is <tt>null</tt>
   * then any IRNode will be accepted.
   */
  public JavaNodeField( final String n, final Template t,
                        final Operator[] ops )
  {
    this( n, t, USE_TEMPLATES_TRACKER, DEFAULT_CONSULTANT, ops );
  }

  /**
   * Create a new field that accepts JavaNodes based on
   * the node's operator.  Uses the templates version tracker.
   * @param n The field's name.
   * @param t The Template the field belongs to.
   * @param c The Field's consultant.
   * @param ops Array of operators to use as filters.  An IRNode
   * will only be accepted if it's operator type is a descendent
   * of an operator in the list.  If the list is <tt>null</tt>
   * then any IRNode will be accepted.
   */
  public JavaNodeField( final String n, final Template t,
                        final FieldConsultant c,
                        final Operator[] ops )
  {
    this( n, t, USE_TEMPLATES_TRACKER, c, ops );
  }
}