package com.surelogic.jsure.client.eclipse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class LibResources {
	public static final String PATH = "/lib/";
	public static final String PROMISES_JAR = PATH + "promises.jar";
	public static final String PROMISES_JAVADOC_JAR = PATH + "promises-javadoc.jar";
	
	public static InputStream getPromisesJar() throws IOException {
		URL url = LibResources.class.getResource(PROMISES_JAR);
		InputStream is = url.openStream();
		return is;
	}
	
	public static InputStream getPromisesJavadocJar() throws IOException {
		URL url = LibResources.class.getResource(PROMISES_JAVADOC_JAR);
		InputStream is = url.openStream();
		return is;
	}
}
