/**
 * 
 */
package com.surelogic.test.scripting;

import java.io.FileNotFoundException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

/**
 * Deletes the file if it exists, and throws an exception otherwise
 * 
 * @author ethan
 */
public class DeleteFile extends AbstractFileCommand {
	/**
	 * @param context
	 * @param contents The arguments in the following order:
	 * 1 - the file name to delete (relative to the workspace)
	 */
	@Override
	protected boolean execute(ICommandContext context, IFile file) throws Exception {
		if(file != null && file.exists()){
    		file.delete(IResource.NONE, null);
		}
		else
		{
			throw new FileNotFoundException(file + " does not exist.");
		}
		if(file.exists()){
			throw new Exception("File: " + file + " could not be deleted.");
		}
		return true;
	}

}
