package com.surelogic.jsure.client.eclipse.views;

import java.net.*;

import com.surelogic.common.ISourceZipFileHandles;
import com.surelogic.common.eclipse.views.AbstractHistoricalSourceView;
import com.surelogic.fluid.javac.*;

public class JSureHistoricalSourceView extends AbstractHistoricalSourceView {
    private static Projects projects;
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
    		tryToOpenInEditorUsingFieldName(JSureHistoricalSourceView.class, null, pkg, type, field);       
    	}
    }

    public static void setLastRun(Projects p, ISourceZipFileHandles handles) {
    	projects = p;
        zips = handles;
    }

    public static String tryToMapPath(String path) {
    	if (projects == null) {
    		return path;
    	}
		try {
	    	final URI uri = new URI(path);
			for(Config config : projects.getConfigs()) {
				JavaSourceFile f = config.mapPath(uri);
				if (f != null) {
					String mapped = f.file.toURI().toString();
					if (mapped != null) {
						return mapped;
					}
				}
			}
		} catch (URISyntaxException e) {
			// Nothing to do
		}
        return path;
    }
}
