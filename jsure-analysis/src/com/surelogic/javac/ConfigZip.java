package com.surelogic.javac;

import java.io.*;
import java.util.zip.ZipOutputStream;

import com.surelogic.common.*;
import com.surelogic.common.java.*;

/**
 * A hack to zip up all the files for the Config
 * 
 * @author Edwin
 */
public class ConfigZip extends AbstractJavaZip<JavaSourceFile> {
	private final Config config;
	private final JavaSourceFile root = new JavaSourceFile(null, null, null, false, null);
	
	public ConfigZip(Config c) {
		config = c;
	}
	
	@Override
	protected void addAnnotatedResourcesToZip(final ZipOutputStream out, 
			TempInfo info, final JavaSourceFile ignored) {
		for(JavaSourceFile f : config.getFiles()) {
			addAnnotatedFileToZip(out, info, f);
		}
	}

	@Override
	protected InputStream getFileContents(JavaSourceFile res)
			throws IOException {
		return new FileInputStream(res.file);
	}

	@Override
	protected String getFullPath(JavaSourceFile res) throws IOException {
		return res.relativePath;
	}

	@Override
	protected String[] getIncludedTypes(JavaSourceFile res) {
		return new String[] { res.qname };
	}

	@Override
	protected String getJavaPackageNameOrNull(JavaSourceFile res) {
		final int lastDot = res.qname.lastIndexOf('.');
		return lastDot < 0 ? "" : res.qname.substring(0, lastDot);
	}

	@Override
	protected String getName(JavaSourceFile res) {
		final int lastSep = res.relativePath.lastIndexOf(File.separatorChar);
		String rv = lastSep < 0 ? res.relativePath : res.relativePath.substring(lastSep+1);
		//System.out.println("Name = "+rv);
		return rv;
	}

	@Override
	protected JavaSourceFile getRoot() {
		return root;
	}

	@Override
	protected long getTimestamp(JavaSourceFile res) {
		return res.file.lastModified();
	}

	@Override
	protected boolean isAccessible(JavaSourceFile res) {
		return true;
	}

	@Override
	protected boolean isFile(JavaSourceFile res) {
		return res != null;
	}
	
	@Override
	protected JavaSourceFile getFile(JavaSourceFile res, String name) {
		if (res == getRoot()) {
			// TODO tools properties
		}
		return null;
	}
	
	@Override
	protected JavaSourceFile[] getMembers(JavaSourceFile res)
			throws IOException {
		throw new UnsupportedOperationException();
	}
}
