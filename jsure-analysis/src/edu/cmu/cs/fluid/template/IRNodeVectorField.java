/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/template/IRNodeVectorField.java,v 1.9 2005/06/13 19:04:11 chance Exp $ */
package edu.cmu.cs.fluid.template;

import edu.cmu.cs.fluid.tree.GraphLabel;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.version.VersionTracker;

/**
 * A field that can hold a list of IRNodes.  Each position in the
 * field may have different constraints on the IRNode it can
 * can accept. 
 */
@Deprecated
@SuppressWarnings("all")
public class IRNodeVectorField
extends StrategizedField
{
  /**
   * Create new field with a given name and parent template
   * @param n The name of the field.
   * @param t The Template to which the field belongs.
   * @param vt The verion tracker to use to get the version that
   * the data should be from.
   * @param c The Field's consultant.
   * @param max The maximum length of the vector.
   * @param fN An array specifying what operators are legal
   * in the first <code>fN.length</code> positions.
   * @param r The legal operators for the rest of the positions.
   * @param gl The GraphLabel instance to use to get the operator of
   * a node.  Must be non-<code>null</code> if <code>ops</code> is
   * non-<code>null</code>.
   * @exception IllegalArgumentException
   * Thrown if <code>c</code> is <code>null</code>.
   */
  public IRNodeVectorField( final String n, final Template t,
                            final VersionTracker vt,
                            final FieldConsultant c,
                            final int max,
                            final Operator[][] fN,
                            final Operator[] r,
                            final GraphLabel gl )
  {
    super( n, t, true, vt, c, 
           new FancyVectorStrategy( new IRNodeStrategy.IRNodeVectorDescriptor( 0, max, fN, r, gl ) ) );
  }

  /**
   * Create a new field with no maximum length, and with the 
   * same operator restrictions on all positions.
   * @param n The name of the field.
   * @param t The Template to which the field belongs.
   * @param vt The verion tracker to use to get the version that
   * the data should be from.
   * @param c The Field's consultant.
   * @param ops The legal operators.
   * @param gl The GraphLabel instance to use to get the operator of
   * a node.  Must be non-<code>null</code> if <code>ops</code> is
   * non-<code>null</code>.
   * @exception IllegalArgumentException
   * Thrown if <code>c</code> is <code>null</code>.
   */
  public IRNodeVectorField( final String n, final Template t,
                            final VersionTracker vt,
                            final FieldConsultant c,
                            final Operator[] ops, final GraphLabel gl )
  {
    this( n, t, vt, c, NO_LIMIT, null, ops, gl );
  }

  /**
   * Create a new field with no maximum length, and with the 
   * same operator restrictions on all positions, that uses
   * the default field consultant.
   * @param n The name of the field.
   * @param t The Template to which the field belongs.
   * @param vt The verion tracker to use to get the version that
   * the data should be from.
   * @param ops The legal operators.
   * @param gl The GraphLabel instance to use to get the operator of
   * a node.  Must be non-<code>null</code> if <code>ops</code> is
   * non-<code>null</code>.
   * @exception IllegalArgumentException
   * Thrown if <code>c</code> is <code>null</code>.
   */
  public IRNodeVectorField( final String n, final Template t,
                            final VersionTracker vt,
                            final Operator[] ops, final GraphLabel gl )
  {
    this( n, t, vt, DEFAULT_CONSULTANT, NO_LIMIT, null, ops, gl );
  }
}
