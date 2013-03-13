// $Header:
// /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedIterator.java,v 1.8
// 2003/07/02 20:19:24 thallora Exp $
package edu.cmu.cs.fluid.version;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;

/**
 * A wrapper around an iterator that sets the version correctly before calling
 * the iterator functions. It does not support remove. (It would not make sense
 * to support remove, since versioned state is immutable.)
 * 
 * @see VersionedEnumeration
 */
public final class VersionedListIterator<T>
  extends AbstractRemovelessIterator<T> implements ListIteratable<T> {
  /**
	 * Logger for this class
	 */
  protected static final Logger LOG = SLLogger.getLogger("IR.version");

  final Version v;
  final ListIterator<T> i;

  public VersionedListIterator(Version v, ListIterator<T> i) {
    super();
    this.v = v;
    this.i = i;
  }
  
  public VersionedListIterator(ListIterator<T> i) {
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

  @Override
  public boolean hasPrevious() {
    try {
      Version.saveVersion();
      Version.setVersion(v);
      return i.hasPrevious();
    } finally {
      Version.restoreVersion();
    }
  }

  @Override
  public T previous() {
    try {
      Version.saveVersion();
      Version.setVersion(v);
      return i.previous();
    } finally {
      Version.restoreVersion();
    }
  }

  @Override
  public int nextIndex() {
    try {
      Version.saveVersion();
      Version.setVersion(v);
      return i.nextIndex();
    } finally {
      Version.restoreVersion();
    }
  }

  @Override
  public int previousIndex() {
    try {
      Version.saveVersion();
      Version.setVersion(v);
      return i.previousIndex();
    } finally {
      Version.restoreVersion();
    }
  }

  @Override
  public void set(T val) {
    try {
      Version.saveVersion();
      Version.setVersion(v);
      i.set(val);
    } finally {
      Version.restoreVersion();
    }
  }

  @Override
  public void add(T val) {
    try {
      Version.saveVersion();
      Version.setVersion(v);
      i.add(val);
    } finally {
      Version.restoreVersion();
    }
  }
}
