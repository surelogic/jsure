package edu.cmu.cs.fluid.unparse;



public class MandatoryBP extends Breakpoint {
	MandatoryBP(int prio, Glue horz, Glue indent) {
		this.prio = prio; horzGlue = horz; indentGlue = indent;
	}
	static public final MandatoryBP BREAKBP = 
		new MandatoryBP(1, Glue.JUXT, Glue.JUXT);
	@Override
  public String toString() {
		return ("{M}");
	}
}
