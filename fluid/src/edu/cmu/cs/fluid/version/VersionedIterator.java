// $Header:
// /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedIterator.java,v 1.8
// 2003/07/02 20:19:24 thallora Exp $
package edu.cmu.cs.fluid.version;

import java.util.Iterator;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;
import edu.cmu.cs.fluid.util.AbstractRemovelessIterator;

/**
 * A wrapper around an ietartor that sets the version correctly before calling
 * the iterator functions. It does not support remove. (It would not make sense
 * to support remove, since versioned state is immutable.)
 * 
 * @see VersionedEnumeration
 */
public final class VersionedIterator<T>
  extends AbstractRemovelessIterator<T> {
  /**
	 * Logger for this class
	 */
  protected static final Logger LOG = SLLogger.getLogger("IR.version");

  final Version v;
  final Iterator<T> i;

  public VersionedIterator(Version v, Iterator<T> i) {
    super();
    this.v = v;
    this.i = i;
  }
  
  public VersionedIterator(Iterator<T> i) {
    this(Version.getVersionLocal(), i);
    v.mark();
  }

  @Override
  public boolean hasNext() {
    try {
      Version.saveVersion();
      Version.setVersion(v);
      return i.hasNext();
    } finally {
      Version.restoreVersion();
    }
  }

  @Override
  public T next() {
    try {
      // LOG.info("Iterator from "+Version.getVersion()+" to "+v);
      Version.saveVersion(v);
      T o = i.next();
      // LOG.info("Got value "+o);
      return o;
    } finally {
      //Version v2 = Version.getVersion();
      Version.restoreVersion();
      // LOG.info("Iterator from "+v2+" back to "+Version.getVersion());
    }
  }
}
