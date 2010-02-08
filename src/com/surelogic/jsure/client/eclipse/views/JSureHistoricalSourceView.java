package com.surelogic.jsure.client.eclipse.views;

import com.surelogic.common.ISourceZipFileHandles;
import com.surelogic.common.eclipse.views.AbstractHistoricalSourceView;

public class JSureHistoricalSourceView extends AbstractHistoricalSourceView {
    @Override
    protected ISourceZipFileHandles findSources(String run) {
        // TODO Auto-generated method stub
        return null;
    }

    public static void tryToOpenInEditor(final String run, final String pkg,
            final String type, int lineNumber) {
        tryToOpenInEditor(JSureHistoricalSourceView.class, run, pkg, type, lineNumber);      
    }
    
    public static void tryToOpenInEditor(final String run, final String pkg,
            final String type, final String field) {
        tryToOpenInEditor(JSureHistoricalSourceView.class, run, pkg, type, field);       
    }
}
