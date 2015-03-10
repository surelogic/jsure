/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedEnumeration.java,v 1.10 2007/07/05 18:15:13 aarong Exp $ */
package edu.cmu.cs.fluid.version;

import java.util.Enumeration;
import java.util.NoSuchElementException;

@Deprecated
@SuppressWarnings("all")
public class VersionedEnumeration implements Enumeration {
  private Enumeration base;
  private Version version;

  public VersionedEnumeration(Enumeration base, Version version) {
    this.base = base;
    this.version = version;
  }
  public VersionedEnumeration(Enumeration base) {
    this(base, Version.getVersionLocal());
    version.mark();
  }

  public boolean hasMoreElements() {
    if (version == null)
      return false;
    Version current = Version.getVersionLocal();
    try {
      Version.setVersion(version);
      if (base.hasMoreElements()) {
        return true;
      } else {
        finish();
        return false;
      }
    } finally {
      Version.setVersion(current);
    }
  }

  public Object nextElement() throws NoSuchElementException {
    Version current = Version.getVersionLocal();
    try {
      Version.setVersion(version);
      return base.nextElement();
    } catch (NoSuchElementException ex) {
      finish();
      throw ex;
    } finally {
      Version.setVersion(current);
    }
  }

  public void finish() {
    if (version != null)
      version.release();
    version = null;
  }

  @Override
  protected void finalize() throws Throwable {
    finish();
    super.finalize();
  }
}
