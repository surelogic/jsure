package com.surelogic.javac;

import java.io.*;
import java.net.URI;

public abstract class AbstractClassPathEntry implements IClassPathEntry {
	private final boolean isExported;
	
	AbstractClassPathEntry(boolean export) {
		isExported = export;
	}
	
	@Override
  public boolean isExported() {
		return isExported;
	}
	
	@Override
  public void zipSources(File zipDir) throws IOException {
		// Nothing to do here
	}

	@Override
  public void copySources(File zipDir, File targetDir) throws IOException {
		// Nothing to do here
	}

	@Override
  public JavaSourceFile mapPath(URI path) {
		return null;
	}
	
	@Override
	public File getFileForClassPath() {
		return null;
	}
}
