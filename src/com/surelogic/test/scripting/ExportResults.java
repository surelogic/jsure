/**
 * 
 */
package com.surelogic.test.scripting;

import java.io.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;

import edu.cmu.cs.fluid.dcf.views.coe.XMLReport;

/**
 * @author ethan
 * 
 */
public class ExportResults extends AbstractCommand {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.test.scripting.ICommand#execute(com.surelogic.test.scripting.ICommandContext,
	 *      java.lang.String[])
	 *      
	 */
	/**
	 * Arguments are as follows:
	 * 1 - project name
	 * 2 - results file name
	 */
	public boolean execute(ICommandContext context, String[] contents)
			throws Exception {
		String resultsName = null;
		final IProject project = resolveProject(contents[1]);
//		final IWorkspace ws = project.getWorkspace();
		final File workspaceFile = new File(project.getLocationURI());//ws.getRoot().getFullPath().toFile();
		
		/*
		 * Handled by ConsistencyListener System.out.println("Updating consistency
		 * proof"); Sea.getDefault().updateConsistencyProof();
		 */

		// Export the results from this run
		try {
			File f = new File(workspaceFile, contents[2]);
			FileOutputStream out = new FileOutputStream(f);
			System.out.println("Exporting results w/ source");
			XMLReport.exportResultsWithSource(out);
			out.close();
			resultsName = f.getAbsolutePath();
			assert (f.exists());

		} catch (FileNotFoundException e) {
			System.out.println("Problem while creating results:");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Problem while closing results:");
			e.printStackTrace();
		}
		return false;
	}

}
