package com.surelogic.jsure.core.xml;

import java.io.File;
import java.util.logging.Level;

import com.surelogic.common.FileUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.xml.PromisesXMLMerge;
import com.surelogic.xml.PromisesXMLParser;
import com.surelogic.xml.TestXMLParserConstants;

/**
 * Code to help merge promises XML between JSure release (in the fluid project)
 * and clients workspace directories (the diff files).
 */
public final class PromisesLibMerge {

	/**
	 * Cleans out the <tt>promises-xml</tt> directory by removing any empty
	 * paths.
	 */
	public static void removeEmptyPathsOnClient() {
		final File libRoot = JSurePreferencesUtility.getJSureXMLDirectory();
		if (libRoot != null) {
			FileUtility.deleteEmptySubDirectories(libRoot);
		}
	}

	/**
	 * Merges any possible JSure release updates to local edits. All XML diff
	 * files in the workspace are updated.
	 */
	public static void mergeJSureToLocal() {
		final File fluidRoot = PromisesXMLParser.getFluidXMLDir();
		final File wsRoot = JSurePreferencesUtility.getJSureXMLDirectory();

		mergeJSureToLocalHelper(wsRoot, fluidRoot);
	}

	private static void mergeJSureToLocalHelper(File local, File jsure) {
		if (local.isDirectory()) {
			for (File f : local.listFiles(TestXMLParserConstants.XML_FILTER)) {
				mergeJSureToLocalHelper(f, new File(jsure, f.getName()));
			}
		} else {
			/*
			 * A bug could leave 0 byte diff files. We can protect against this
			 * by checking for this here.
			 */
			if (local.exists() && local.length() > 0) {
				PromisesXMLMerge.mergeJSureXMLIntoLocalXML(local, jsure);
			} else {
				final boolean result = local.delete();
				SLLogger.getLogger().log(
						result ? Level.WARNING : Level.SEVERE,
						I18N.err(247, local, result ? "deleted successfully"
								: "delete failed"), new Exception());
			}
		}
	}

	/**
	 * Merges the passed XML diff file into the JSure codebase (for subsequent
	 * release).
	 * <p>
	 * <i>Warning: This method only works if the tool user is running from
	 * source code in a meta-Eclipse.</i>
	 * 
	 * @param relativePathToFile
	 *            the path to the XML diff file relative to the
	 *            <tt>promises-xml</tt> directory in the workspace.
	 */
	public static void mergeLocalToJSure(String relativePathToFile) {
		final File fluidRoot = PromisesXMLParser.getFluidXMLDir();
		final File wsRoot = JSurePreferencesUtility.getJSureXMLDirectory();

		final File jsure = new File(fluidRoot, relativePathToFile);
		final File local = new File(wsRoot, relativePathToFile);
		boolean fileExistsOnDisk = local.isFile() && local.exists();
		if (!fileExistsOnDisk) {
			SLLogger.getLogger().log(Level.WARNING, I18N.err(239, local),
					new Exception());
			return; // Nothing to do
		}
		PromisesXMLMerge.mergeLocalXMLIntoJSureXML(local, jsure);
	}

	/**
	 * Rewrites a JSure release promises XML file incrementing its release
	 * version. This method may be used to update the file format after a
	 * structural change. If the passed file doesn't exist this method does
	 * nothing.
	 * <p>
	 * <i>Warning: This method only works if the tool user is running from
	 * source code in a meta-Eclipse.</i>
	 * 
	 * @param relativePathToFile
	 *            the path to the XML file relative to the <tt>lib/promises</tt>
	 *            directory in the fluid project
	 */
	public static void rewriteJSure(String relativePathToFile) {
		final File fluidRoot = PromisesXMLParser.getFluidXMLDir();
		final File jsure = new File(fluidRoot, relativePathToFile);

		PromisesXMLMerge.rewriteJSureXML(jsure, true);
	}
}
