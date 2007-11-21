/**
 * Compares the output of JSure to a given oracle file.
 */
package com.surelogic.test.scripting;

import java.io.File;
import java.io.FilenameFilter;

import org.eclipse.core.resources.IFile;

import com.surelogic.test.ITestOutput;

import edu.cmu.cs.fluid.eclipse.logging.EclipseLogHandler;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.logging.XMLLogDiff;
import edu.cmu.cs.fluid.srv.Results;

/**
 * @author ethan
 * 
 */
public class CompareResults extends AbstractCommand {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.test.scripting.ICommand#execute(com.surelogic.test.scripting.ICommandContext,
	 *      java.lang.String[]) The contents array should contain, in the
	 *      following order 
	 *      1 - results oracle file to compare against 
	 *      2 - the results file 
	 *      3 - the results diffs file 
	 *      4 - The log oracle file to compare 
	 *      5 - the log diffs file
	 */
	public boolean execute(ICommandContext context, String[] contents)
			throws Exception {
		final IFile oracleFile 		  = resolveFile(contents[1]);
		String oracleName 		= oracleFile.getLocationURI().getPath();
		final IFile resultsFile 		= resolveFile(contents[2]);
		final String resultsName 		= resultsFile.getLocationURI().getPath();
		final String diffsName 			= resolveFile(contents[3], true).getLocationURI().getPath();
		
    final ITestOutput XML_LOG = IDE.getInstance().makeLog("EclipseLogHandler");
		boolean ok = true;
		System.out
				.println("Try to compare these results to the results oracle");
		assert (new File(oracleName).exists());
		assert (new File(resultsName).exists());

		final Results results = Results.doDiff(oracleName, resultsName);
		if(results == null){
			throw new Exception("Results are null");
		}

		results.generateXML(diffsName);
		System.out.println("diffs.xml = " + diffsName);
		ok = ok && !results.root.hasChildren();
		
		
		/*
		final IFile oracleLogFile 	= resolveFile(contents[4]);
		final String oracleLogName 	= oracleLogFile.getLocationURI().getPath();
		final String logDiffsName 	= resolveFile(contents[5], true).getLocationURI().getPath();

		System.out.println("Try to compare the log to the log oracle");
		assert (new File(oracleLogName).exists());
		try {
			System.out.println("Starting log diffs");
			int numDiffs = XMLLogDiff.diff(XML_LOG, oracleLogName, logName,
					logDiffsName);
			System.out.println("#diffs = " + numDiffs);
			ok = ok && (numDiffs == 0);
			System.out.println("log diffs = " + logDiffsName);
		} catch (Exception e) {
			System.out.println("Problem while diffing the log: "
					+ oracleName + ", " + "logName" + ", " + logDiffsName);
			e.printStackTrace();
			throw e;
		} finally {
		  XML_LOG.close();
		}
		*/
		if(!ok){
			throw new Exception("Results " + resultsFile + " don't match the oracle file: " + oracleName);
		}
	return false;
	}
	
	private static FilenameFilter oracleFilter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.startsWith("oracle") && name.endsWith(".zip");
		}
	};

	private static FilenameFilter logOracleFilter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.startsWith("oracle") && name.endsWith(".log.xml");
		}
	};

	private String getOracleName(String projectPath, FilenameFilter filter,
			String defaultName) {
		File path = new File(projectPath);
		File[] files = path.listFiles(filter);
		File file = null;
		for (File zip : files) {
			if (file == null) {
				file = zip;
			} else if (zip.getName().length() > file.getName().length()) {
				// Intended for comparing 3.2.4 to 070221
				file = zip;
			} else if (zip.getName().length() == file.getName().length()
					&& zip.getName().compareTo(file.getName()) > 0) {
				// Intended for comparing 070107 to 070221
				file = zip;
			}
		}
		return (file != null) ? file.getAbsolutePath() : projectPath
				+ File.separator + defaultName;
	}
}
