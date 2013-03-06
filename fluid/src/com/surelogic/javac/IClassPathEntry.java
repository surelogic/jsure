package com.surelogic.javac;

import java.io.*;
import java.net.URI;

import com.surelogic.dropsea.irfree.XmlCreator;

public interface IClassPathEntry {
	/**
     *  whether a classpath entry is exported to dependent projects (or not)
     */
	boolean isExported();
	void init(JavacProject jp, JavacClassParser loader) throws IOException; 
	void zipSources(File zipDir) throws IOException;
	void copySources(File zipDir, File targetDir) throws IOException;
	JavaSourceFile mapPath(URI path);
	void relocateJars(File targetDir) throws IOException;
	
	void outputToXML(XmlCreator.Builder b);
	/**
	 * @return a File suitable for adding to the classpath, or null
	 */
	File getFileForClassPath();
}
