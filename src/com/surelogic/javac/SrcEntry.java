package com.surelogic.javac;

import java.io.File;
import java.io.IOException;

import com.surelogic.common.xml.Entities;
import com.surelogic.javac.persistence.JSureProjectsXMLCreator;
import com.surelogic.javac.persistence.PersistenceConstants;

/**
 * Used to record the source path
 * 
 * @author Edwin
 */
public class SrcEntry extends AbstractClassPathEntry {
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

	public void outputToXML(JSureProjectsXMLCreator creator, int indent,
			StringBuilder b) {
		Entities.start(PersistenceConstants.SRC, b, indent);
		creator.addAttribute(PersistenceConstants.PATH, projectRelativePath);
		creator.addAttribute(PersistenceConstants.IS_EXPORTED, isExported());
		Entities.closeStart(b, true);
	}

	public void relocateJars(File targetDir) throws IOException {
		// TODO Auto-generated method stub
	}
}
