package com.surelogic.analysis;

import java.util.List;

public interface IAnalysisInfo {
	String getLabel();
	String getUniqueIdentifier();
	boolean isIncluded();
	boolean isProduction();
	boolean runsUniqueness();
	String getCategory();
	String[] getPrerequisiteIds();
	String getAnalysisClassName(); 
	boolean isActive(List<IAnalysisInfo> activeAnalyses);
}
