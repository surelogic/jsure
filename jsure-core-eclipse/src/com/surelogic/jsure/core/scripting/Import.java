/**
 * 
 */
package com.surelogic.jsure.core.scripting;

import java.io.*;
import java.net.URI;

import org.eclipse.core.resources.*;

/**
 * Copies a file (or directory and its contents) into a project
 * 
 * @author ethan
 */
public class Import extends AbstractCommand {
	private final int BUFFER_SIZE = 1024;
	private final int INPUT_SIZE = 2048;
	private final int OUTPUT_SIZE = 2048;
	

	/* (non-Javadoc)
	 * @see com.surelogic.test.scripting.ICommand#execute(com.surelogic.test.scripting.ICommandContext, java.lang.String[])
	 */
	/**
	 * @param context
	 * @param contents Arguments for this class in the following order:
	 * 1 - directory/project name to import into
	 * 2 - file or directory to import
	 */
	  @Override
	public boolean execute(ICommandContext context, String... contents)
			throws Exception {
		final String target = contents[1];
		IResource res;
		if (target.contains("/")) {
			IFile file = resolveIFile(target, true);
			if(file == null){
				throw new IllegalArgumentException("The file, " + contents[1] + " cannot be created in the workspace.");
			}
			res = file;
		} else {
			IProject project = resolveProject(contents[1]);
			if(project == null || !project.exists()){
				throw new IllegalArgumentException("The project, " + contents[1] + " doesn't exist in the workspace.");
			}
			res = project;
		}
		URI projLoc = res.getLocationURI();
		//Needs to copy all files from the source dir to the project dir
		File file = resolveFile(context, contents[2]);
		if(file.exists()){
			if(file.isDirectory()){
				copyDir(new File(projLoc.getPath()), file);
			}
			else{
				copyFile(file, new File(projLoc.getPath(), file.getName()));
			}
			res.refreshLocal(IResource.DEPTH_INFINITE, null);
			return true;
		}
		throw new FileNotFoundException(file + " does not exist");
	}
	
	/**
	 * @param srcDir the directory to copy all contents from
	 * @param destDir The destination folder
	 * @throws IOException
	 */
	private void copyDir(File srcDir, File destDir) throws IOException{
		File[] files = srcDir.listFiles();
		for (File file : files) {
			if(file.isDirectory()){
				//recreate the dir hierarchy
				copyDir(file, new File(destDir, file.getName()));
			}
			else{
				copyFile(file, new File(destDir, file.getName()));
			}
		}
	}

	/**
	 * Copies one file to another
	 * @param src
	 * @param dest
	 * @throws IOException
	 */
	private void copyFile(File src, File dest) throws IOException{
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		try{
			System.out.println("Copying from " + src + " to " + dest);
    		in = new BufferedInputStream(new FileInputStream(src), INPUT_SIZE);
    		//Create the new file
    		dest.getParentFile().mkdirs();
    		dest.createNewFile();
    		out = new BufferedOutputStream(new FileOutputStream(dest), OUTPUT_SIZE);
    		byte[] buffer = new byte[BUFFER_SIZE];
    		int length = 0;
    		int read = 0;
    		while((length = in.available()) > 0){
        		read = in.read(buffer, 0, (length > BUFFER_SIZE ? BUFFER_SIZE: length));
        		
        		if(read != -1 ){
            		out.write(buffer, 0, read);
        		}
    		}
    		out.flush();
		}
		finally{
			if(in != null){
				try{
    				in.close();
    				System.out.println("Closed input for "+src);
				}catch(IOException e){
					//do nothing
					e.printStackTrace();
				}
			}
			if(out != null){
				try{
    				out.close();
    				System.out.println("Closed output for "+dest);
				}catch(IOException e){
					//do nothing
					e.printStackTrace();
				}
			}
		}
	}

}
