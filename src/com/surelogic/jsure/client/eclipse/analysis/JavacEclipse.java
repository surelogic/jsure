package com.surelogic.jsure.client.eclipse.analysis;

import java.net.URL;

import com.surelogic.common.eclipse.Resources;
import com.surelogic.fluid.eclipse.preferences.PreferenceConstants;
import com.surelogic.fluid.javac.Javac;

import edu.cmu.cs.fluid.ide.IDEPreferences;

public class JavacEclipse extends Javac {
    static final JavacEclipse instance = new JavacEclipse();  
    
    public static void initialize() {
        // Nothing to do right now, besides create the instance above
    }
    
    {
        prefs.put(IDEPreferences.DATA_DIRECTORY, PreferenceConstants.getJSureDataDirectory().getAbsolutePath());
    }
    
    @Override
    public URL getResourceRoot() {
        return Resources.findRoot("edu.cmu.cs.fluid");
    }
}
