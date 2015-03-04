package com.surelogic.jsure.core.scripting;

import java.io.*;

import org.eclipse.core.resources.*;

/**
 * Replace the contents of the specified file with that of another file
 * 
 * @author Edwin
 */
public class ReplaceFileContents extends AbstractCommand {
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.test.scripting.ICommand#execute(com.surelogic.test.scripting.ICommandContext,
	 *      java.lang.String[]) The contents array should contain, in the
	 *      following order 
	 *      1 - the file to edit 
	 *      2 - the file containing the new contents of file(1) 
	 */
	  @Override
	public boolean execute(ICommandContext context, String... contents)
			throws Exception {
		if (contents[1] == null || "".equals(contents[1]) || contents[2] == null
				|| "".equals(contents[2])) {
			throw new IllegalArgumentException("An argument is null or empty.");
		}
		IFile file = resolveIFile(contents[1]);

		if (file == null || !file.exists()) {
			throw new FileNotFoundException("File, " + file + " is not a valid file.");
		}

		// might be outside of this project...
		File newFile = new File(contents[2]);
		if (!newFile.exists()) {
			throw new FileNotFoundException("Patch file, " + newFile
					+ " is not a valid file.");
		}
		FileInputStream fin = new FileInputStream(newFile);
		file.setContents(fin, IResource.NONE, null);		
		return true;
	}

	// Debug code
	@SuppressWarnings("unused")
	private void printInputStream(InputStream in) throws IOException {
		System.out.println("# of bytes in stream: " + in.available());
		try {
			while (in.available() > 0) {
				System.out.write(in.read());
			}
		} finally {
			in.close();
		}
	}

}
