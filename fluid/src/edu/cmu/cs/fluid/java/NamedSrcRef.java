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
  public final String getEnclosingFile() {
    return file;
  }

  public Long getHash() {
    return HashGenerator.UNKNOWN;
  }

  public final String getCUName() {
    return cunit;
  }

  public final String getPackage() {
    return pkg;
  }

  public final String getProject() {
    return project;
  }

  @Override
  public final String getRelativePath() {
    return cunit;
  }
}
