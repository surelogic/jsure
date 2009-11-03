package com.surelogic.ant.jsure;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;

public class JSureScan extends Javac {
	/**
	 * The location of sierra-ant
	 */
	private String home;

	/**
	 * The intended location of the resulting scan document
	 */
	private String document;

	/**
	 * The name of the project being scanned
	 */
	private String project;

	public String getHome() {
		return home;
	}

	public void setHome(String home) {
		this.home = home;
	}

	public String getProjectName() {
		return project;
	}

	public void setProjectName(String p) {
		this.project = p;
	}

	public String getDocument() {
		return document;
	}

	public void setDocument(String doc) {
		this.document = doc;
	}

	@Override
	protected void scanDir(File srcDir, File destDir, String[] files) {
		File[] newFiles = new File[files.length];
		int i = 0;
		for (String name : files) {
			newFiles[i] = new File(srcDir, name);
			i++;
		}

		if (newFiles.length > 0) {
			File[] newCompileList = new File[compileList.length
					+ newFiles.length];
			System.arraycopy(compileList, 0, newCompileList, 0,
					compileList.length);
			System.arraycopy(newFiles, 0, newCompileList, compileList.length,
					newFiles.length);
			compileList = newCompileList;
		}
	}

	/**
	 * Modified from Javac.compile()
	 */
	@Override
	protected void compile() {
		File destDir = this.getDestdir();

		if (compileList.length > 0) {
			log("Scanning " + compileList.length + " source file"
					+ (compileList.length == 1 ? "" : "s") + " in "
					+ destDir.getAbsolutePath());

			if (listFiles) {
				for (int i = 0; i < compileList.length; i++) {
					String filename = compileList[i].getAbsolutePath();
					log(filename);
				}
			}

			CompilerAdapter adapter = new JSureJavacAdapter(this);

			// now we need to populate the compiler adapter
			adapter.setJavac(this);

			// finally, lets execute the compiler!!
			if (!adapter.execute()) {
				if (failOnError) {
					throw new BuildException("Failed", getLocation());
				} else {
					log("Failed", Project.MSG_ERR);
				}
			}
		}
	}
}
