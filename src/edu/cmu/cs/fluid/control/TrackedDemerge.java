/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/TrackedDemerge.java,v 1.4 2003/07/02 20:19:22 thallora Exp $ */
package edu.cmu.cs.fluid.control;

/** Class for splitting parallel control flows that share
 * edges into separate edges.  Each TrackedMerge must
 * be balanced by exactly one TrackedDemerge.
 * @author John Tang Boyland
 * @see TrackedMerge
 */

public class TrackedDemerge extends Split
{
  public TrackedDemerge() { }
}
