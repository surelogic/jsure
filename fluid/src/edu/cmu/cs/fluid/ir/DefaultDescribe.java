// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/DefaultDescribe.java,v 1.2 2003/07/02 20:19:14 thallora Exp $
package edu.cmu.cs.fluid.ir;

import java.io.PrintStream;

/** A mixim class providing a default
 * implementation of the describe method.
 */
public class DefaultDescribe {
  public void describe(PrintStream out) {
    out.println(this.toString());
  }
}
