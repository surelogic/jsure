package edu.cmu.cs.fluid.unparse;


public class NoBP extends Breakpoint {
  // Don't break here!
  static public NoBP NOTBP = new NoBP();

  NoBP(Glue horz) {
    horzGlue = horz; 
    indentGlue = Glue.JUXT; 
    this.prio = MAXPRIO;
  }
  NoBP() {
    this(Glue.JUXT);
  }
  @Override
  public String toString() {
    return "{X}";
  }
}
