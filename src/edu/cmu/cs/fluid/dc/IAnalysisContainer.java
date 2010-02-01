package edu.cmu.cs.fluid.dc;

import java.util.Set;

public interface IAnalysisContainer {
	Iterable<IAnalysisInfo> getAllAnalysisInfo();
	
	boolean isIncludedExtensionsChanged(Set<String> onIds);
	void updateIncludedExtensions(Set<String> ids);

	Set<IAnalysisInfo> getPrerequisiteAnalysisExtensionPoints(
			IAnalysisInfo extension);
}
