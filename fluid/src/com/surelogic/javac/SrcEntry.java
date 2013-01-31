package com.surelogic.javac;

import java.io.File;
import java.io.IOException;

import com.surelogic.dropsea.irfree.XmlCreator;
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
	
	@Override
  public void init(JavacProject jp, JavacClassParser loader)
			throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
  public void outputToXML(XmlCreator.Builder proj) {
		XmlCreator.Builder b = proj.nest(PersistenceConstants.SRC);
		b.addAttribute(PersistenceConstants.PATH, projectRelativePath);
		b.addAttribute(PersistenceConstants.IS_EXPORTED, isExported());
		b.end();
	}

	@Override
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
