/**
 * 
 */
package com.surelogic.test.scripting;

import java.io.FileNotFoundException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

/**
 * @author ethan
 *
 */
public class DeleteFile extends AbstractCommand {

	/* (non-Javadoc)
	 * @see com.surelogic.test.scripting.ICommand#execute(com.surelogic.test.scripting.ICommandContext, java.lang.String[])
	 */
	/**
	 * @param context
	 * @param contents The arguments in the following order:
	 * 1 - the file name to delete
	 */
	public boolean execute(ICommandContext context, String[] contents)
			throws Exception {
		IFile file = resolveFile(contents[1]);
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
