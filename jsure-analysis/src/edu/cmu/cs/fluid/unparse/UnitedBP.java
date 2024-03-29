
package edu.cmu.cs.fluid.unparse;


public class UnitedBP extends Breakpoint {
  String name;
  public UnitedBP(int prio, String n, Glue horz, Glue indent) {
    this(prio, horz, indent, n);
  }
  public UnitedBP(int prio, Glue horz, Glue indent, String n) {
    super(prio,horz,indent);
    name = n;
  }
  public UnitedBP(int prio, String name) {
    this(prio, Glue.UNIT, Glue.INDENT, name);
  }
  @Override
  public String getName() {
    return name;
  }
  @Override
  public String toString() {
    return ("{U:" + name + ":" + prio + "}");
  }
}

