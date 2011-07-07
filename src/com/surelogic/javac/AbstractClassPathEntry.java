package com.surelogic.javac;

import java.io.*;
import java.net.URI;

public abstract class AbstractClassPathEntry implements IClassPathEntry {
	private final boolean isExported;
	
	AbstractClassPathEntry(boolean export) {
		isExported = export;
	}
	
	public boolean isExported() {
		return isExported;
	}
	
	public void zipSources(File zipDir) throws IOException {
		// Nothing to do here
	}

	public void copySources(File zipDir, File targetDir) throws IOException {
		// Nothing to do here
	}

	public JavaSourceFile mapPath(URI path) {
		return null;
	}
}
