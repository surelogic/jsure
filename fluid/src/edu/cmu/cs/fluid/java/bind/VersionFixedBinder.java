/*
 * $header$
 * Created on Jul 13, 2005
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotUndefinedException;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.version.Version;

public class VersionFixedBinder extends AbstractBinder {
  private static final Logger LOG = SLLogger.getLogger("FLUID.java.bind");

  private final IBinder fixed;
  private final Version atVersion;
  private final VersionFixedTypeEnvironment tEnv;
  
  public static VersionFixedBinder fix(IBinder b) {
    if (b instanceof VersionFixedBinder) return (VersionFixedBinder)b;
    return new VersionFixedBinder(b);
  }
  
  public VersionFixedBinder(IBinder b, Version v) {
    fixed = b;
    atVersion = v;
    tEnv = new VersionFixedTypeEnvironment(b.getTypeEnvironment(),v,this);
  }
  
  public VersionFixedBinder(IBinder b) {
    this(b,Version.getVersion());
  }
  
  VersionFixedBinder(IBinder b, Version v, VersionFixedTypeEnvironment e) {
    fixed = b;
    atVersion = v;
    tEnv = e;
  }
  
  public Version getAtVersion() {
    return atVersion;
  }
  
  @Override
  protected IBinding getIBinding_impl(IRNode name) {
    Version.saveVersion(atVersion);
    try {
      return fixed.getIBinding(name);
    } catch (SlotUndefinedException ex) {
      Version.restoreVersion();
      LOG.warning("undefined slot for " + name + " at version " + atVersion);
      LOG.warning("  " + DebugUnparser.toString(name));
      Version.saveVersion(atVersion);
      throw ex;
    } finally {
      Version.restoreVersion();
    }
  }
  
  @Override
  public Iteratable<IBinding> findOverriddenParentMethods(IRNode mth) {
    Version.saveVersion(atVersion);
    try {
      return fixed.findOverriddenParentMethods(mth);
    } finally {
      Version.restoreVersion();
    }
  }

  @Override
  public ITypeEnvironment getTypeEnvironment() {
    return tEnv;
  }

  @Override
  public IJavaType getJavaType(IRNode n) {
    Version.saveVersion(atVersion);
    try {
      return fixed.getJavaType(n);
    } finally {
      Version.restoreVersion();
    }
  }
  
  
}
