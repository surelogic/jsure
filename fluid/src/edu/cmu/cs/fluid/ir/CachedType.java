/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/CachedType.java,v 1.4 2006/03/28 20:58:45 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import com.surelogic.ThreadSafe;

/** A mixin for IR types that use caching for storage.
 * The methods are implemented 
 * @see IRInput#readCachedObject
 * @see IROutput#writeCachedObject
 */
@ThreadSafe
public abstract class CachedType<T> implements IRType<T> {
  public void writeValue(T value, IROutput out)
       throws IOException
  {
    if (out.writeCachedObject(value)) return;
    writeValueInternal(value,out);
  }

  /** Write the contents of a cached object to the output stream. */
  protected void writeValueInternal(Object value, IROutput out)
       throws IOException
  {
    // override to write internal state.
  }

  @SuppressWarnings("unchecked")
  public T readValue(IRInput in)
       throws IOException
  {
    T cached = (T) in.readCachedObject();
    if (cached != null) return cached;
    cached = createValue(in);
    in.cacheReadObject(cached);
    initValue(cached,in);
    return cached;
  }

  /** Create the cached object.  The parameter stream
   * may be used to get information about the identity or
   * class of the object but must not be used to read
   * other cached objects.
   * @see #initValue
   */
  @SuppressWarnings("unchecked")
  protected T createValue(IRInput in)
       throws IOException
  {
    // override for creation
    return (T) new Object();
  }

  /** Initialize the contents of the object.
   */
  protected void initValue(Object object, IRInput in)
       throws IOException
  {
    // override to read contents
  }
}
