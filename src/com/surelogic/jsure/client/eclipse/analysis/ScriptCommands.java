package com.surelogic.jsure.client.eclipse.analysis;

public interface ScriptCommands {
	String NAME = "test.script.txt";
	
	String IMPORT = "import";
	String TOUCH_FILE = "touchFile";
	String PATCH_FILE = "patchFile";
	String DELETE_FILE = "deleteFile";
	String CREATE_PROJECT = "createProject";
	String OPEN_PROJECT = "openProject";
	String CLOSE_PROJECT = "closeProject";
    String ADD_NATURE = "addNature";
    String REMOVE_NATURE = "removeNature";
    String EXPORT_RESULTS = "exportResults";
    String COMPARE_RESULTS = "compareResults";
}
