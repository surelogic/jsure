/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/NamedSrcRef.java,v 1.3 2008/05/28 20:55:46 chance Exp $*/
package edu.cmu.cs.fluid.java;

import com.surelogic.common.HashGenerator;

public final class NamedSrcRef extends AbstractSrcRef {
  private final Object file;
  private final String pkg, cunit;
  private final String project;
  
  public NamedSrcRef(String proj, Object f, String p, String cu) {
	project = proj;
    file = f;
    pkg = CommonStrings.intern(p);
    cunit = cu;
  }
  
  @Override
  public Object getEnclosingFile() {
    return file;
  }
  
  public Long getHash() {
	  return HashGenerator.UNKNOWN;
  }

  public String getCUName() {
	  return cunit;
  }

  public String getPackage() {
	  return pkg;
  }
  
  public String getProject() {
	  return project;
  }
  
  @Override
  public String getRelativePath() {
	  return cunit;
  }
}
