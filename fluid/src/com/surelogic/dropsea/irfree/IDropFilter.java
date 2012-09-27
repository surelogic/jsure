package com.surelogic.dropsea.irfree;

import com.surelogic.dropsea.IDrop;

public interface IDropFilter {
    boolean showResource(IDrop d);
    @Deprecated
    boolean showResource(String path);
    
    static final IDropFilter nullFilter = new IDropFilter() {
    	public boolean showResource(String path) {
    		return true;
    	}

    	public boolean showResource(IDrop d) {
    		return true;
    	}
    };
}
