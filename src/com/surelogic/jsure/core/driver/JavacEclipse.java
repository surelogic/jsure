package com.surelogic.jsure.core.driver;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.Resources;
import com.surelogic.javac.*;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

import edu.cmu.cs.fluid.ide.IDEPreferences;

public class JavacEclipse extends Javac {
    static final JavacEclipse instance = new JavacEclipse();  
    
    public static void initialize() {
        // Nothing to do right now, besides create the instance above
    }
    
    public static JavacEclipse getDefault() {
    	return instance;
    }
    
    {
    	File dataDir = JSurePreferencesUtility.getJSureDataDirectory();
    	setPreference(IDEPreferences.JSURE_DATA_DIRECTORY, dataDir.getAbsolutePath());
    	setPreference(IDEPreferences.JSURE_XML_DIRECTORY, new File(dataDir, DriverConstants.XML_PATH_SEGMENT).getAbsolutePath());
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
            return Resources.findRoot("edu.cmu.cs.fluid");
    	} catch(IllegalStateException e) {
    		URL here = Resources.findRoot("com.surelogic.jsure.client.eclipse");
    		try {
    			File f = new File(here.toURI());
    			System.out.println("j.c.e = "+f);
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
    		} catch (URISyntaxException use) {
    			e.printStackTrace();
    		}
    	}
    	return null;
    }
}
