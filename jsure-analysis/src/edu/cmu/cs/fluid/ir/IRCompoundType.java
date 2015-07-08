/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRCompoundType.java,v 1.6 2007/05/25 02:12:41 boyland Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import com.surelogic.ThreadSafe;

/** These types have compound parts.
 * @see IRCompound
 */
@ThreadSafe
public interface IRCompoundType<T> extends IRType<T> {
  /** Return the type associated with compound element i */
  public IRType<?> getType(int i);

  /** Read in the value given current state of compound.
   * This differs from @{link IRCompound#readContents}
   * because it handles the (re)reading of the container value
   * as well.  This method is needed to handle reading
   * persistent files all of which save the value of the compound
   * as well the contents.
   * @see #readValue(IRInput)
   */
  public T readValue(IRInput in, T currentValue) throws IOException;
}
