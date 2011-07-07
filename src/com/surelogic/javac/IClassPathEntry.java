package com.surelogic.fluid.javac;

import java.io.*;
import java.net.URI;

import com.surelogic.fluid.javac.persistence.JSureProjectsXMLCreator;

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
	
	void outputToXML(JSureProjectsXMLCreator creator, int indent, StringBuilder b);
}
