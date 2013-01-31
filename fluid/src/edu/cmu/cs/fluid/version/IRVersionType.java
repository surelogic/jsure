/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/IRVersionType.java,v 1.11 2007/05/25 02:12:41 boyland Exp $ */
package edu.cmu.cs.fluid.version;

import java.io.IOException;
import java.util.Comparator;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.IRInput;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.IRPersistent;
import edu.cmu.cs.fluid.ir.IRType;

public class IRVersionType implements IRType<Version> {
  public static final IRVersionType prototype = new IRVersionType();

  static {
    IRPersistent.registerIRType(prototype,'V');
  }

  /** Return true if a legally obtainable version. */
  @Override
  public boolean isValid(Object value) {
    return value instanceof Version && ((Version)value).isFrozen();
  }

  @Override
  public Comparator<Version> getComparator() 
  {
    return null;
  }

  /** Write a version to an IR stream. (We handle null as a special case) */
  @Override
  public void writeValue(Version value, IROutput out)
       throws IOException
  {
    writeVersion(value,out);
  }

  /** Write a version to an IR stream. (We handle null as a special case) */
  public static void writeVersion(Version v, IROutput out)
    throws IOException 
  {
    out.debugBegin("version");
    if (v == null) {
      out.writeInt(-1);
    } else if (v == Version.getInitialVersion()) {
      out.writeInt(0);
    } else {
      out.writeInt(v.getEraOffset());
      out.writePersistentReference(v.getEra());
    }
    out.debugEnd("version");
  }

  /** Read a version from an IR stream. */
  @Override
  public Version readValue(IRInput in)
       throws IOException
  {
    return readVersion(in);
  }

  /** Read a version from an IR stream. */
  public static Version readVersion(IRInput in)
       throws IOException
  {
    in.debugBegin("version");
    int offset = in.readInt();
    Version v;
    if (offset == 0) {
      v = Version.getInitialVersion();
    } else if (offset == -1) {
      v = null;
    } else {
      Era e = (Era)in.readPersistentReference();
      v = e.getVersion(offset);
    }
    in.debugEnd("version");
    return v;
  }

  @Override
  public void writeType(IROutput out) throws IOException
  {
    out.writeByte('V');
  }

  @Override
  public IRType<Version> readType(IRInput in) { return this; }

  /** @exception fluid.NotImplemented */
  @Override
  public Version fromString(String s) {
    throw new NotImplemented("fluid.version.IRVersionType.fromString()");
  }

  /** @exception fluid.NotImplemented */
  @Override
  public String toString(Version o) {
    throw new NotImplemented("fluid.version.IRVersionType.toString()");
  }

  public static void ensureLoaded() {
    // System.out.println("IRVersionType loaded");
  }
}
