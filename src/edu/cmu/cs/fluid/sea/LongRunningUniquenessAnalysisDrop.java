package edu.cmu.cs.fluid.sea;

public final class LongRunningUniquenessAnalysisDrop extends
		LongRunningAnalysisDrop {
	private LongRunningUniquenessAnalysisDrop() {
		super("Long running Uniqueness analysis warnings");
		setCategory(Category.getInstance(630));
	}
	
	public static final Factory factory = new Factory() {
		public InfoDrop create(String type) {
			return new LongRunningUniquenessAnalysisDrop();
		}
	};
}
