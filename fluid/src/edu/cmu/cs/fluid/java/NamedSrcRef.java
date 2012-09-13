package edu.cmu.cs.fluid.java;

import com.surelogic.common.HashGenerator;

public class NamedSrcRef extends AbstractSrcRef {
  private final String file;
  private final String pkg, cunit;
  private final String project;

  public NamedSrcRef(String proj, String f, String p, String cu) {
    project = proj;
    file = f;
    pkg = CommonStrings.intern(p);
    cunit = cu;
  }

  @Override
  public String getEnclosingFile() {
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
