package com.surelogic.jsure.core.driver;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.javac.Javac;

import edu.cmu.cs.fluid.ide.IDEPreferences;

public class JavacEclipse extends Javac {
    static final JavacEclipse instance = new JavacEclipse();  
    
    public static void initialize() {
        // Nothing to do right now, besides create the instance above
    }
    
    public static JavacEclipse getDefault() {
    	return instance;
    }
       
    public void synchronizeAnalysisPrefs() {
		for(String id : getAvailableAnalyses()) {
			boolean value = EclipseUtility.getBooleanPreference(IDEPreferences.ANALYSIS_ACTIVE_PREFIX + id);
			setPreference(IDEPreferences.ANALYSIS_ACTIVE_PREFIX + id, value);
		}
		for(String pref : IDEPreferences.BOOL_PREFS_TO_SYNC) {
			boolean value = EclipseUtility.getBooleanPreference(pref);			
			setPreference(pref, value);
		}
		for(String pref : IDEPreferences.INT_PREFS_TO_SYNC) {
			int value = EclipseUtility.getIntPreference(pref);			
			setPreference(pref, value);
		}
		for(String pref : IDEPreferences.STR_PREFS_TO_SYNC) {
			String value = EclipseUtility.getStringPreference(pref);			
			setPreference(pref, value);
		}
		/*
		if (XUtil.testing) {
			new Throwable("For stack trace").printStackTrace();
		}
*/
    }
    
    @Override
    public URL getResourceRoot() {
    	try {
    		File f = new File(EclipseUtility.getDirectoryOf("edu.cmu.cs.fluid"));
    		return f.toURI().toURL();
    	} catch(Throwable e) {
    		// Try to use this plugin to find fluid
    		String here = EclipseUtility.getDirectoryOf("com.surelogic.jsure.core");
    		File f = new File(here);
    		System.out.println("j.core = "+f);
    		for(File f2 : f.getParentFile().listFiles()) {
    			if (f2.getName().startsWith("edu.cmu.cs.fluid_")) {
    				System.out.println("Found "+f2);
    				try {
    					return f2.toURI().toURL();
    				} catch (MalformedURLException e1) {
    					e1.printStackTrace();
    				}
    			}
    		}
    	}
    	return null;
    }
}
