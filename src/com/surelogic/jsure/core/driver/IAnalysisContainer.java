package com.surelogic.jsure.core.driver;

import java.util.Set;

import com.surelogic.analysis.IAnalysisInfo;

public interface IAnalysisContainer {
	Iterable<IAnalysisInfo> getAllAnalysisInfo();
	
	boolean isIncludedExtensionsChanged(Set<String> onIds);
	void updateIncludedExtensions(Set<String> ids);

	Set<IAnalysisInfo> getPrerequisiteAnalysisExtensionPoints(
			IAnalysisInfo extension);
}
