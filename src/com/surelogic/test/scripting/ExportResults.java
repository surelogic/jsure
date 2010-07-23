/**
 * 
 */
package com.surelogic.test.scripting;

import java.io.*;

import org.eclipse.core.resources.*;

import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.xml.*;

/**
 * Export the Drop-Sea results as XML to the specified file name
 * 
 * @author ethan
 */
public class ExportResults extends AbstractCommand {
	/**
	 * Arguments are as follows:
	 * 1 - project name
	 * 2 - results file name prefix (w/o suffix)
	 */
	public boolean execute(ICommandContext context, String... contents)
			throws Exception {
		final IProject project = resolveProject(contents[1]);
		final File workspaceFile = new File(project.getLocationURI());
		//ws.getRoot().getFullPath().toFile();

		// Export the results from this run
		try {
			final File location = new File(workspaceFile, contents[2] + SeaSnapshot.SUFFIX);
			SeaSummary.summarize(project.getName(), Sea.getDefault(), location);
			assert (location.exists());
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
