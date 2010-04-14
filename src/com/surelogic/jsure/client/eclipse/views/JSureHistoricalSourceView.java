package com.surelogic.jsure.client.eclipse.views;

import java.net.*;

import com.surelogic.common.ISourceZipFileHandles;
import com.surelogic.common.eclipse.views.AbstractHistoricalSourceView;
import com.surelogic.fluid.javac.Config;
import com.surelogic.fluid.javac.JavaSourceFile;

public class JSureHistoricalSourceView extends AbstractHistoricalSourceView {
    private static Config config;
    private static ISourceZipFileHandles zips;
    private static boolean viewIsEnabled = false;
    
    @Override
    protected ISourceZipFileHandles findSources(String run) {
        //if (config.getRun().equals(run)) {
            return zips;
        //}
        //return null;
    }

    public static void tryToOpenInEditor(final String pkg,
            final String type, int lineNumber) {
    	if (viewIsEnabled) {
    		tryToOpenInEditor(JSureHistoricalSourceView.class, null, pkg, type, lineNumber);      
    	}
    }
    
    public static void tryToOpenInEditor(final String pkg,
            final String type, final String field) {
    	if (viewIsEnabled) {
    		tryToOpenInEditor(JSureHistoricalSourceView.class, null, pkg, type, field);       
    	}
    }

    public static void setLastRun(Config cfg, ISourceZipFileHandles handles) {
        config = cfg;
        zips = handles;
    }

    public static String tryToMapPath(String path) {
    	
		try {
			JavaSourceFile f = config.mapPath(new URI(path));
	        String mapped = f.file.toURI().toString();
	        if (mapped != null) {
	            return mapped;
	        }
		} catch (URISyntaxException e) {
			// Nothing to do
		}
        return path;
    }
}
