package com.surelogic.javac;

import java.io.File;
import java.net.URI;

import com.surelogic.common.FileUtility;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.javac.persistence.PersistenceConstants;

import edu.cmu.cs.fluid.util.Pair;

public class JavaSourceFile {
	public final String qname;
	public final File file;
	public final String relativePath;
	public final boolean asBinary;
	
	public JavaSourceFile(String name, File f, String path, boolean asBinary) {
		if (name != null && name.startsWith(".")) {
			throw new IllegalArgumentException();
		}
		qname = name;
		file = f;		
		relativePath = FileUtility.normalizePath(path);
		this.asBinary = asBinary;
	}
	
	@Override
	public String toString() {
		return relativePath == null ? file.toString() : relativePath;
	}
	
	@Override 
	public int hashCode() {
		return file.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof JavaSourceFile) {
			JavaSourceFile f2 = (JavaSourceFile) o;
			if (file.equals(f2.file)) {
				if (!qname.equals(f2.qname) || !relativePath.equals(f2.relativePath)) {
					throw new IllegalStateException();
				}
				return true;
			}			
		}
		return false;
	}

	public Pair<URI, String> getLocation() {
		return new Pair<URI, String>(file.toURI(), relativePath);
	}

	public void outputToXML(XMLCreator.Builder proj) {
		XMLCreator.Builder b = proj.nest(PersistenceConstants.FILE);		
		b.addAttribute(PersistenceConstants.PATH, relativePath);
		b.addAttribute(PersistenceConstants.QNAME, qname);
		b.addAttribute(PersistenceConstants.LOCATION, file.getAbsolutePath());
		b.addAttribute(PersistenceConstants.AS_BINARY, asBinary);
		b.end();
	}
}
