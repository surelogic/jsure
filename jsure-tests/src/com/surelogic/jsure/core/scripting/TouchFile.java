package com.surelogic.jsure.core.scripting;

import java.io.*;

import org.eclipse.core.resources.*;

import com.surelogic.common.FileUtility;

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
			f.create(new ByteArrayInputStream(FileUtility.noBytes), true, null);
		}  
		return true;
	}
}
