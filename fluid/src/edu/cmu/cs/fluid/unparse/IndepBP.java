
package edu.cmu.cs.fluid.unparse;



public class IndepBP extends Breakpoint {
	IndepBP(int prio, Glue horz, Glue indent) {
		this.prio = prio; horzGlue = horz; indentGlue = indent;
	}
	IndepBP(int prio) {
		this(prio, Glue.UNIT, Glue.INDENT);
	}
	static public final IndepBP DONTBP = 
		new IndepBP(MAXPRIO, Glue.JUXT, Glue.JUXT);
	static public final IndepBP JUXTBP = DONTBP;
	static public final IndepBP SIMPLEBP = 
		new IndepBP(9);
	static public final IndepBP DEFAULTBP = SIMPLEBP;
	@Override
  public String toString() {
		return ("{I:" + prio + "}");
	}
}
