package com.surelogic.fluid.javac.persistence;

import java.io.File;

public class JSureFileInfo {
	final File srcZip, resultZip;
	
	JSureFileInfo(File src, File result) {
		srcZip = src;
		resultZip = result;
	}
}
