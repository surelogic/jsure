package com.surelogic.analysis;

public interface IAnalysisInfo {
	String getLabel();
	String getUniqueIdentifier();
	boolean isIncluded();
	boolean isProduction();
	boolean runsUniqueness();
	String getCategory();
	String[] getPrerequisiteIds();
	Class<?> getAnalysisClass();
}
