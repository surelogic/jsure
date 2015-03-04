package com.surelogic.jsure.core.scripting;

import java.io.*;

import org.eclipse.compare.patch.*;
import org.eclipse.core.resources.*;
import org.eclipse.jdt.launching.sourcelookup.*;

/**
 * Uses the eclipse Patch APIs (org.eclipse.compare.patch) to patch the specified file
 * 
 * @author ethan
 */
public class PatchFile extends AbstractCommand {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.test.scripting.ICommand#execute(com.surelogic.test.scripting.ICommandContext,
	 *      java.lang.String[]) The contents array should contain, in the
	 *      following order 
	 *      1 - the file to edit 
	 *      2 - the patch
	 */
	@SuppressWarnings("deprecation")
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

		IStorage diff = resolveIFile(contents[2]);		
		if (diff == null) {
			// Not in the workspace
			File f = resolveFile(context, contents[2]);
			if (f != null) {
				diff = new LocalFileStorage(f);
			}
		}
		//printStream(file.getName(), diff.getContents());
		
		IFilePatch[] patches = ApplyPatchOperation.parsePatch(diff);

		PatchConfiguration config = new PatchConfiguration();

		// This should only apply to this file, and not any others
		for (IFilePatch filePatch : patches) {						
			IFilePatchResult result = filePatch.apply(file, config, null);

			if (result.hasRejects()) {
				System.err.println("Some patches could not be applied to "+contents[2]);
				return false;
			}

			InputStream in = result.getPatchedContents();
			file.setContents(in, IResource.FORCE, null);
			//printStream(file.getName()+" AFTER", file.getContents());
			// TODO sync the file						
		}
		// Ensure the file is considered changed, even if the patch is otherwise empty
		file.touch(null); 
		return true;
	}
	
	@SuppressWarnings("unused")
	private void printStream(String name, InputStream is) throws IOException {
		LineNumberReader r = new LineNumberReader(new InputStreamReader(is));
		String line = null;
		while ((line = r.readLine()) != null) {
			System.out.println(name+": "+line);
		}
		r.close();
	}
}
