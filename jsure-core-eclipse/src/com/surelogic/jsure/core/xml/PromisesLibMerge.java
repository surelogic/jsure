package com.surelogic.jsure.core.xml;

import java.io.*;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.xml.*;

/**
 * Code to help merge promises XML between fluid and clients' data directories
 * 
 * @author Edwin
 */
public final class PromisesLibMerge {
	/*
	public static File getFluidXMLDir() {
		File fluidDir = null;
		try {
			fluidDir = new File(JavacEclipse.getDefault().getResourceRoot().toURI());
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Bad URL: "+JavacEclipse.getDefault().getResourceRoot());
		}
		final File fLibDir = new File(fluidDir, TestXMLParserConstants.PROMISES_XML_PATH);
		if (!fLibDir.isDirectory()) {
			throw new IllegalStateException("Couldn't find directory "+fLibDir);
		}
		return fLibDir;
	}
	*/
	
	/**
	 * @param toClient update if true; merge to fluid otherwise
	 */
	public static void merge(boolean toClient) {
		merge(toClient, "");
	}
	
	/**
	 * Update "local" XML (deltas) specific to the client
	 */
	public static void updateClient() {
		merge(true, "");
	}
	
	public static void merge(boolean toClient, String relativePath) {
		final File fLibRoot = PromisesXMLParser.getFluidXMLDir();
		final File libRoot = JSurePreferencesUtility.getJSureXMLDirectory();

		final File fLibPath = new File(fLibRoot, relativePath);
		final File libPath = new File(libRoot, relativePath);
		if (!libPath.exists()) {			
			//System.out.println("Nothing to merge: "+relativePath);
			return; // Nothing else to do
		}		
		if (toClient) {
			PromisesXMLMerge.merge(toClient, libPath, fLibPath);
		} else {
			PromisesXMLMerge.merge(toClient, fLibPath, libPath);
		}
	}
	
	/**
	 * From fluid to local
	 * 
	 * @return true if there is an update to Fluid
	 */
	public static boolean checkForUpdate(String relativePath) {
		final File fLibRoot = PromisesXMLParser.getFluidXMLDir();
		final File libRoot = JSurePreferencesUtility.getJSureXMLDirectory();

		final File fLibPath = new File(fLibRoot, relativePath);
		final File libPath = new File(libRoot, relativePath);
		return PromisesXMLMerge.checkForUpdate(fLibPath, libPath);
	}
}
