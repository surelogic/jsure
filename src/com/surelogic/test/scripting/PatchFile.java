/**
 * Mimics editing a file by replacing the contents of one file with that of another.
 * Originally this was supposed to use the eclipse Patch APIs (org.eclipse.compare.patch) but that
 * API doesn't exist in version prior to 3.3 (Europa)
 */
package com.surelogic.test.scripting;

import java.io.*;

import org.eclipse.core.resources.*;

/**
 * @author ethan
 * 
 */
public class PatchFile extends AbstractCommand {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.test.scripting.ICommand#execute(com.surelogic.test.scripting.ICommandContext,
	 *      java.lang.String[]) The contents array should contain, in the
	 *      following order 
	 *      1 - the file to edit 
	 *      2 - the file containing the new contents of file(1) 
	 *      3 - the project name
	 */
	public boolean execute(ICommandContext context, String[] contents)
			throws Exception {
		if (contents[1] == null || "".equals(contents[1]) || contents[2] == null
				|| "".equals(contents[2])) {
			throw new IllegalArgumentException("An argument is null or empty.");
		}

		IFile file = resolveFile(contents[1]);

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

		/*
		IFilePatch[] patches = ApplyPatchOperation.parsePatch(diff);

		PatchConfiguration config = new PatchConfiguration();

		for (IFilePatch filePatch : patches) {
			IFilePatchResult result = filePatch.apply(file, config, null);

			if (result.hasRejects()) {
				System.err.println("Some patches could not be applied.");
				return false;
			}

			InputStream in = result.getPatchedContents();
			file.setContents(in, IResource.NONE, null);
		}
		*/
		file.setContents(fin, IResource.NONE, null);
		
		return true;
	}

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
