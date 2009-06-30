/*
 * Created on Mar 4, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package edu.cmu.cs.fluid.java.analysis;

/**
 * @author dfsuther
 */
public class CPKind {

  private final String name;

  private CPKind(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  public static final CPKind DECLARE = new CPKind("declare");
  public static final CPKind GRANT = new CPKind("grant");
  public static final CPKind REQUIRE = new CPKind("require");
  public static final CPKind NOTE = new CPKind("note");
  public static final CPKind REVOKE = new CPKind("revoke");
  
}
