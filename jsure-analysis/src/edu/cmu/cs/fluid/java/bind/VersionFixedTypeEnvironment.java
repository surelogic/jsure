/*
 * $header$
 * Created on Jul 13, 2005
 */
package edu.cmu.cs.fluid.java.bind;

import com.surelogic.analysis.IIRProject;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.version.Version;

public class VersionFixedTypeEnvironment extends AbstractTypeEnvironment {

  private final ITypeEnvironment fixed;
  private final Version atVersion;
  private final VersionFixedBinder binder;
  
  public VersionFixedTypeEnvironment(ITypeEnvironment e, Version v) {
    fixed = e;
    atVersion = v;
    binder = new VersionFixedBinder(e.getBinder(),v,this);
  }
  
  VersionFixedTypeEnvironment(ITypeEnvironment e, Version v, VersionFixedBinder b) {
    fixed = e;
    atVersion = v;
    binder = b;
  }
  
  @Override
  public IBinder getBinder() {
    return binder;
  }

  @Override
  public IRNode findNamedType(String qname) {
    Version.saveVersion(atVersion);
    try {
      return fixed.findNamedType(qname);
    } finally {
      Version.restoreVersion();
    }
  }

  @Override
  public IRNode findPackage(String name, IRNode context) {
    Version.saveVersion(atVersion);
    try {
      return fixed.findPackage(name, context);
    } finally {
      Version.restoreVersion();
    }
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.AbstractTypeEnvironment#getClassTable()
   */
  @Override
  public IJavaClassTable getClassTable() {
    return fixed.getClassTable();
  }

  @Override
  public IIRProject getProject() {
	  return fixed.getProject();
  }
}
