package com.surelogic.jsure.client.eclipse.analysis;

import java.net.URL;

import com.surelogic.common.eclipse.Resources;
import com.surelogic.fluid.javac.Javac;

public class JavacEclipse extends Javac {
    static final JavacEclipse instance = new JavacEclipse();  
    
    public static void initialize() {
        // Nothing to do right now, besides create the instance above
    }
    
    @Override
    public URL getResourceRoot() {
        return Resources.findRoot("edu.cmu.cs.fluid");
    }
}
