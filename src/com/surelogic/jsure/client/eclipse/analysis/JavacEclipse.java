package com.surelogic.jsure.client.eclipse.analysis;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.jface.preference.IPreferenceStore;

import com.surelogic.common.XUtil;
import com.surelogic.common.eclipse.Resources;
import com.surelogic.fluid.eclipse.preferences.PreferenceConstants;
import com.surelogic.fluid.javac.Config;
import com.surelogic.fluid.javac.Javac;

import edu.cmu.cs.fluid.dc.Plugin;
import edu.cmu.cs.fluid.ide.IDEPreferences;

public class JavacEclipse extends Javac {
    static final JavacEclipse instance = new JavacEclipse();  
    
    public static void initialize() {
        // Nothing to do right now, besides create the instance above
    }
    
    {
        prefs.put(IDEPreferences.DATA_DIRECTORY, PreferenceConstants.getJSureDataDirectory().getAbsolutePath());
    }
    
    public void synchronizeAnalysisPrefs(IPreferenceStore store) {
		for(String id : Config.getAvailableAnalyses()) {
			boolean val = store.getBoolean(Plugin.ANALYSIS_ACTIVE_PREFIX + id);
			if (XUtil.testing) {
				System.out.println("Setting "+id+" to "+(val ? "active" : "inactive"));
			}
			prefs.put(id, val);
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
