package com.surelogic.jsure.core.xml;

import java.io.*;
import java.util.logging.Level;

import com.surelogic.common.FileUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.xml.*;

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
			PromisesXMLMerge.merge(MergeType.JSURE_TO_LOCAL, local, jsure);
		}
	}

	/**
	 * Merges the passed XML diff file into the JSure codebase (for subsequent
	 * release).
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
		if (!local.exists()) {
			SLLogger.getLogger().log(Level.WARNING, I18N.err(239, local),
					new Exception());
			return; // Nothing to do
		}
		PromisesXMLMerge.merge(MergeType.LOCAL_TO_JSURE, local, jsure);
	}
}
