/*$Header: /cvs/fluid/fluid/src/com/surelogic/xml/PackageAccessor.java,v 1.5 2007/09/17 19:00:06 chance Exp $*/
package com.surelogic.xml;

import java.io.*;
import java.net.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xml.sax.InputSource;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.util.DirectoryFileLocator;
import edu.cmu.cs.fluid.util.FileLocator;
import edu.cmu.cs.fluid.util.ZipFileLocator;

public class PackageAccessor implements TestXMLParserConstants {

	public static final String XML_EXT = "xml";
	public static final String ZIP_EXT = "zx";

	public static String packagePath(String pkgName) {
	  return packagePath(pkgName, true);
	}
	
	public static String packagePath(String pkgName, boolean systemSpecific) {
		/** Convert the package name into a directory path */
		String [] path = pkgName.split("\\.");
		StringBuffer dirName = new StringBuffer();

		for(int i = 0; i < path.length; i++) {
		  dirName.append(path[i]);
		  dirName.append(systemSpecific ? File.separator : '/');
		}
		return dirName.toString();
	}
	
	public static String promiseFileName(String classPath) {
		String [] path = classPath.split("\\.");
		
		// TODO this only works for .promise.xml
		return path[path.length - 3] + "." + path[path.length - 2] + "."+ path[path.length - 1];
	}

	public static String packageClassName(File f) throws FileNotFoundException {
		if(f == null)
			throw new FileNotFoundException();
		return f.getName().split("\\.")[0];
	} 

	public static String packageExtension(File f) {
		String [] fname = f.getName().split("\\.");

		/** Promise files are expected to be in the format: 
		 * classname.promise.xml or classname.promise.zx for zipped xml */
		if(fname.length < 3)
			return null;

		if(fname[fname.length - 2].equalsIgnoreCase("promises")) {
			if(fname[fname.length - 1].equalsIgnoreCase(XML_EXT))
				return XML_EXT;
			else if(fname[fname.length - 1].equalsIgnoreCase(ZIP_EXT))
				return ZIP_EXT;
		}
		return null;
	}

	public static InputSource readFile(File f) throws IOException {
		if (f == null) {
			throw new FileNotFoundException("File doesn't exist");  
		}
		String ext = packageExtension(f);

		if(ext == null) {
			throw new FileNotFoundException("Extension not understood");
		} else if (ext.equalsIgnoreCase(XML_EXT)) {
			return new InputSource(new FileReader(f));		
		} else if (ext.equalsIgnoreCase(ZIP_EXT)) {
//			ZipFileLocator z = new ZipFileLocator(f,ZipFileLocator.READ);
//			return new InputSource(z.openFileRead(packageClassName(f)));
			System.out.println("Trying to open zip file " + f.getPath());
			ZipFile z = new ZipFile(f,ZipFile.OPEN_READ);
			ZipEntry ze = z.getEntry(packageClassName(f));
			return new InputSource(new BufferedInputStream(z.getInputStream(ze)));
//			return new InputSource(new ZipInputStream(new FileInputStream(f)));
		}

		throw new FileNotFoundException();
	}

	public static OutputStreamWriter writePackage(String pkgName, String className) 
	throws IOException {
		String dirName = packagePath(pkgName);

		DirectoryFileLocator dir = new DirectoryFileLocator();
		/**
		 * NOTE: this creates the directory structure in the working 
		 * directory.
		 */
		dir.setAndCreateDirPath(dirName);
		return new OutputStreamWriter(dir.openFileWrite(className + ".promises.xml"));
	}

	public static InputSource readPackage(String pkgName, String className) 
	throws IOException {
		String dirName = packagePath(pkgName, false); 
		/*
		DirectoryFileLocator dir = new DirectoryFileLocator(DIR_PREFIX + dirName);
		File file = dir.locateFile(className, true);
		
		System.out.println("Looking for file " + className + " in path " + DIR_PREFIX + dirName);
		*/
		URL root  = IDE.getInstance().getResourceRoot();	
		URL clazz = new URL(root, "lib/promises/" + dirName + className);		
		InputStream is = clazz.openStream();
		
		if (is == null) {
			throw new FileNotFoundException(className + " doesn't exist");  
		}
		
		return new InputSource(is);
	}

	public static OutputStreamWriter writeZipPackage(String pkgName, String className) 
	throws IOException {
		String dirName = packagePath(pkgName); 

		DirectoryFileLocator dir = new DirectoryFileLocator(DIR_PREFIX + dirName);
		dir.setAndCreateDirPath(dirName);
		File file = dir.locateFile(className + ".promise.zx", false);
		if (file == null) {
			throw new FileNotFoundException(className + " doesn't exist");  
		}

		FileLocator z = new ZipFileLocator(file,ZipFileLocator.WRITE);

		return new OutputStreamWriter(z.openFileWrite(className));
	}

	public static InputSource readZipPackage(String pkgName, String className) 
	throws IOException {
		String dirName = packagePath(pkgName); 

		DirectoryFileLocator dir = new DirectoryFileLocator(DIR_PREFIX + dirName);
		File file = dir.locateFile(className, true);
		if (file == null) {
			throw new FileNotFoundException(className + " doesn't exist");  
		}
		ZipFileLocator z = new ZipFileLocator(file,ZipFileLocator.READ);

		return new InputSource(z.openFileRead(className));
	}

}
