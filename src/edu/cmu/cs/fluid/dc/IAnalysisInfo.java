package edu.cmu.cs.fluid.dc;

public interface IAnalysisInfo {
	String getLabel();
	String getUniqueIdentifier();
	boolean isIncluded();
	boolean isProduction();
	String getCategory();
}
