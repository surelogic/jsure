package com.surelogic.javac;

import java.io.File;
import java.io.IOException;

import com.surelogic.common.xml.XMLCreator;
import com.surelogic.javac.persistence.PersistenceConstants;

/**
 * Used to record the source path
 * 
 * @author Edwin
 */
public class SrcEntry extends AbstractClassPathEntry {
	@SuppressWarnings("unused")
	private final Config project;
	private final String projectRelativePath;
	
	public SrcEntry(Config c, String path) {
		super(true); // TODO is this right?
		project = c;
		projectRelativePath = path;
	}

	public String getProjectRelativePath() {
		return projectRelativePath;
	}
	
	public void init(JavacProject jp, JavacClassParser loader)
			throws IOException {
		// TODO Auto-generated method stub
	}

	public void outputToXML(XMLCreator.Builder proj) {
		XMLCreator.Builder b = proj.nest(PersistenceConstants.SRC);
		b.addAttribute(PersistenceConstants.PATH, projectRelativePath);
		b.addAttribute(PersistenceConstants.IS_EXPORTED, isExported());
		b.end();
	}

	public void relocateJars(File targetDir) throws IOException {
		// TODO Auto-generated method stub
	}
	
	@Override
	public int hashCode() {
		return projectRelativePath.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof SrcEntry) {
			SrcEntry s2 = (SrcEntry) o;
			return projectRelativePath.equals(s2.projectRelativePath);
		}
		return false;
	}
}
