package com.surelogic.xml;

import java.io.File;
import java.net.URI;
import java.net.URL;

import com.surelogic.common.Pair;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ide.IDERoot;

/**
 * Does the same thing as TestXMLParser, but uses the Java XML model to apply
 * the promises
 * 
 * @author Edwin
 */
public final class PromisesXMLParser {

	/**
	 * Gets the release library annotation XML directory in the fluid project.
	 * 
	 * @return the release library annotation XML directory.
	 * 
	 * @throws Exception
	 *             if the release library annotation XML directory cannot be
	 *             found or is not a directory.
	 */
	public static File getFluidXMLDir() {
		File fluidDir = null;
		final URL url = IDERoot.getInstance().getResourceRoot();
		try {
			final URI uri = url.toURI();
			fluidDir = new File(uri);
		} catch (Exception e) {
			if ("file".equals(url.getProtocol())) {
				final String path = url.getPath();
				fluidDir = new File(path);
			} else {
				throw new IllegalStateException(I18N.err(251, url));
			}
		}
		final File fLibDir = new File(fluidDir,
				TestXMLParserConstants.PROMISES_XML_REL_PATH);
		if (!fLibDir.isDirectory()) {
			System.out.println("Unable to find fluid libs via "+IDERoot.getInstance());
			throw new IllegalStateException(I18N.err(252, fLibDir));
		}
		return fLibDir;
	}

	/**
	 * Gets the local library annotation XML diff path if one exists or
	 * {@code null}.
	 * 
	 * @return the local library annotation XML diff path if one exists or
	 *         {@code null}.
	 */
	public static File getLocalXMLDirOrNull() {
		final String XmlDiffPath = IDERoot.getInstance().getStringPreference(
				IDEPreferences.JSURE_XML_DIFF_DIRECTORY);
		if (XmlDiffPath != null) {
			final File result = new File(XmlDiffPath);
			if (!result.isDirectory()) {
				SLLogger.getLogger().warning(I18N.err(250, XmlDiffPath));
				return null;
			}
			return result;
		}
		return null;
	}

	/**
	 * @return non-null Pair of files for fluid and local
	 */
	public static Pair<File, File> findPromisesXML(final String path) {
		File fluid = null;
		File local = null;
		File localXml = getLocalXMLDirOrNull();
		if (localXml != null) {
			File f = new File(localXml, path);
			// if (f.isFile()) {
			local = f;
			// }
		}
		// Try fluid
		final File xml = getFluidXMLDir();
		if (xml != null) {
			File f = new File(xml, path);
			// if (f.isFile()) {
			fluid = f;
			// }
		}
		return new Pair<File, File>(fluid, local);
	}

	public static PackageElement load(String xmlPath) {
		return load(xmlPath, false);
	}

	public static PackageElement load(String xmlPath, boolean ignoreDiffs) {
		Pair<File, File> f = findPromisesXML(xmlPath);
		PackageElement p = PromisesXMLReader.load(xmlPath, f.first(),
				ignoreDiffs ? null : f.second());
		return p;
	}
}
