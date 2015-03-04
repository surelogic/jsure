package com.surelogic.jsure.core.scripting;

import com.surelogic.analysis.AnalysisConstants;

public interface ScriptCommands {
	String NAME = "test.script.txt";
	String ANALYSIS_SETTINGS = "jsure.analysis.settings";
	String TEST_PROPERTIES = "test.properties";
	
	String AUTO_BUILD = "autobuild";
	
	String GO_FIRST = "!";
	
	boolean USE_EXPECT = false;
	String RUN_JSURE = "runJSure";
	
	String EXPECT_BUILD = "expectBuild";
	String EXPECT_BUILD_FIRST = "!"+EXPECT_BUILD;
	String EXPECT_ANALYSIS = AnalysisConstants.EXPECT_ANALYSIS;
	String EXPECT_ANALYSIS_FIRST = "!"+AnalysisConstants.EXPECT_ANALYSIS;
	
	String IMPORT = "import";
	String TOUCH_FILE = "touchFile";
	String PATCH_FILE = "patchFile";
	String DELETE_FILE = "deleteFile";
	String CREATE_PROJECT = "createProject";
	String DELETE_PROJECT = "deleteProject";
	String OPEN_PROJECT = "openProject";
	String CLOSE_PROJECT = "closeProject";

    String CLEANUP_DROPS = "cleanupDrops";
    String CLEANUP_DROPS_FIRST = "!cleanupDrops";
    String EXPORT_RESULTS = "exportResults";
    String COMPARE_RESULTS = "compareResults";
}
