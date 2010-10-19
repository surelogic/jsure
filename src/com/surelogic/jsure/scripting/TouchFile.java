package com.surelogic.jsure.scripting;

import java.io.*;

import org.eclipse.core.resources.*;

/**
 * Touch a file, or create it if it doesn't exist
 * 
 * @author Edwin
 */
public class TouchFile extends AbstractFileCommand {
	@Override
	protected boolean execute(ICommandContext context, IFile f) throws Exception {
		if (f.exists()) {
			f.touch(null);
		} else {
			f.create(new ByteArrayInputStream(noBytes), true, null);
		}  
		return true;
	}
}
