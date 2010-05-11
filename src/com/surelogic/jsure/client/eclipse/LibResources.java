package com.surelogic.jsure.client.eclipse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class LibResources {
	/**
	 * The name of the current promises Jar file.
	 * <p>
	 * If you change this you <i>must</i> add the current name to the {@code
	 * PROMISES_JAR_OLD_VERSIONS} array.
	 */
	public static final String PROMISES_JAR = "promises-3.2.1.jar";

	/**
	 * Holds the names of the old library files that may need to be upgraded to
	 * the new promises Jar file.
	 */
	public static final String[] PROMISES_JAR_OLD_VERSIONS = { "promises.jar",
			"promises-3.0.0.jar", "promises-3.1.0.jar", "promises-3.2.0.jar" };

	public static final String PATH = "/lib/";
	public static final String PROMISES_JAR_PATHNAME = PATH + PROMISES_JAR;

	public static InputStream getPromisesJar() throws IOException {
		final URL url = LibResources.class.getResource(PROMISES_JAR_PATHNAME);
		final InputStream is = url.openStream();
		return is;
	}
}
