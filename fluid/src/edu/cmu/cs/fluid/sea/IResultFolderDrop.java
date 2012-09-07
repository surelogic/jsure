package edu.cmu.cs.fluid.sea;

import java.util.Collection;

public interface IResultFolderDrop extends IAnalysisResultDrop {
	/**
	 * Gets all the analysis results directly within this folder. If sub-folders
	 * exist, analysis results within the sub-folders are <b>not</b> returned.
	 * 
	 * @return a non-null (possibly empty) set of analysis results.
	 */
	Collection<? extends IResultDrop> getAnalysisResults();

	/**
	 * Gets all the sub-folders within this folder.
	 * 
	 * @return a non-null (possibly empty) set of analysis result folders.
	 */
	Collection<? extends IResultFolderDrop> getSubFolders();

	/**
	 * Gets all the analysis results and sub-folders within this folder.
	 * 
	 * @return a non-null (possibly empty) set of analysis results and
	 *         sub-folders.
	 */
	Collection<? extends IAnalysisResultDrop> getContents();	
}
