package com.surelogic.jsure.core.xml;

import java.io.*;
import java.net.URISyntaxException;

import com.surelogic.common.FileUtility;
import com.surelogic.jsure.core.driver.JavacEclipse;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.xml.*;

/**
 * Code to help merge promises XML between fluid and clients' data directories
 * 
 * @author Edwin
 */
public final class PromisesLibMerge {
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
	
	/**
	 * @param toClient update if true; merge to fluid otherwise
	 */
	public static void merge(boolean toClient) {
		merge(toClient, "");
	}
	
	public static void merge(boolean toClient, String relativePath) {
		final File fLibRoot = getFluidXMLDir();
		final File libRoot = JSurePreferencesUtility.getJSureXMLDirectory();

		final File fLibPath = new File(fLibRoot, relativePath);
		final File libPath = new File(libRoot, relativePath);
		if (!fLibPath.exists() || !libPath.exists()) {			
			System.out.println("Nothing to merge: "+relativePath);
			return; // Nothing else to do
		}		
		if (toClient) {
			merge(toClient, libPath, fLibPath);
		} else {
			merge(toClient, fLibPath, libPath);
		}
	}
	
	/**
	 * From fluid to local
	 * 
	 * @return true if there is an update to Fluid
	 */
	public static boolean checkForUpdate(String relativePath) {
		final File fLibRoot = getFluidXMLDir();
		final File libRoot = JSurePreferencesUtility.getJSureXMLDirectory();

		final File fLibPath = new File(fLibRoot, relativePath);
		final File libPath = new File(libRoot, relativePath);
		if (!fLibPath.isFile() || !libPath.isFile()) {		
			return false;
		}
		try {
			PackageElement fluid = PromisesXMLReader.load(fLibPath);
			PackageElement local = PromisesXMLReader.load(libPath);			
			return fluid.needsToUpdate(local);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * @param onlyMerge also copy (to fluid) if false
	 */
	private static void merge(final boolean onlyMerge, File to, File from) {
		if (from.isFile()) {			
			if (!to.exists()) {
				// Check if I should copy 
				if (onlyMerge) {
					//System.out.println("Ignoring "+from);
					return; // No need to do anything
				}
				// Copy 
				System.out.println("Copying "+from+" into "+to);
				to.getParentFile().mkdirs();
				FileUtility.copy(from, to);
			} else {
				try {					
					// Merge
					System.out.println("Merging "+from+" into "+to);
					PackageElement target = PromisesXMLReader.load(to);
					PackageElement source = PromisesXMLReader.load(from);
					PromisesXMLMerge.merge(onlyMerge, target, source);
					
					PromisesXMLWriter w = new PromisesXMLWriter(to);
					w.write(target);
					if (!onlyMerge) {
						// Merging all changes to fluid, so they should both be the same afterward
						// (or the local one should be deleted/empty)
						// TODO what about conflicts?
						if (PromisesXMLMerge.onlyKeepDiffs) {
							from.delete();
						} else {
							w = new PromisesXMLWriter(from);
							w.write(target);
						}
					}
				} catch (Exception e) {
					System.err.println("While merging "+from+" into "+to);
					e.printStackTrace();
				}
			}
		} 
		else if (from.isDirectory()) {
			for(File f : from.listFiles(TestXMLParserConstants.XML_FILTER)) {
				merge(onlyMerge, new File(to, f.getName()), f);
			}
		}
		// 'from' doesn't exist, so nothing to do
	}
}
