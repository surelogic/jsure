package com.surelogic.jsure.core.scripting;

import com.surelogic.fluid.javac.Util;

public interface ScriptCommands {
	String NAME = "test.script.txt";
	String ANALYSIS_SETTINGS = "jsure.analysis.settings";
	String TEST_PROPERTIES = "test.properties";
	
	String AUTO_BUILD = "autobuild";
	
	String GO_FIRST = "!";
	
	String EXPECT_BUILD = "expectBuild";
	String EXPECT_BUILD_FIRST = "!"+EXPECT_BUILD;
	String EXPECT_ANALYSIS = Util.EXPECT_ANALYSIS;
	String EXPECT_ANALYSIS_FIRST = "!"+Util.EXPECT_ANALYSIS;
	
	String IMPORT = "import";
	String TOUCH_FILE = "touchFile";
	String PATCH_FILE = "patchFile";
	String DELETE_FILE = "deleteFile";
	String CREATE_PROJECT = "createProject";
	String DELETE_PROJECT = "deleteProject";
	String OPEN_PROJECT = "openProject";
	String CLOSE_PROJECT = "closeProject";
    String ADD_NATURE = "addNature";
    String REMOVE_NATURE = "removeNature";
    String CLEANUP_DROPS = "cleanupDrops";
    String CLEANUP_DROPS_FIRST = "!cleanupDrops";
    String EXPORT_RESULTS = "exportResults";
    String COMPARE_RESULTS = "compareResults";
}
