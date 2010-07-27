package com.surelogic.jsure.tests;

import com.surelogic.test.scripting.ScriptReader;

import junit.framework.TestCase;

public class TestScripting extends TestCase {
  public void testMain() throws Exception {
    ScriptReader r = new ScriptReader();
    r.executeScript(
        "set autobuild\n"+
        "createProject foo\n"+
        "openProject foo\n"+
        "addNature foo\n"+
        "touchFile foo/Foo.java\n"+
        "deleteFile foo/Foo.java\n"+
        "removeNature foo\n"+
        "closeProject foo\n"+
        "closeProject bar\n"+
        "#end");
  }
  
  public void testExporting() throws Exception{
    ScriptReader r = new ScriptReader();
    r.executeScript(
        "set autobuild\n"+
        "createProject foo2 /work/regression-test-workspace/util.concurrent_regression\n"+ // From zip??
        "openProject foo2\n"+
        "exportResults foo2 results\n"+
        "compareResults foo2 foo2/results2.sea.xml foo2/diff.xml\n"+
        "closeProject foo2\n"+
        "#end"
    );
  }
  
  public void testPatching() throws Exception{
    ScriptReader r = new ScriptReader();
    r.executeScript(
        "set autobuild\n"+
        "unset autosave\n"+
        "set compiler 1.5\n"+
        "createProject foo3\n"+
        "openProject foo3\n"+
        "touchFile foo3/Latch.java\n"+
//        "saveFile foo3/Foo.java\n"+
//        "patchFile foo3/Latch.java /Users/ethan/Desktop/Latch.java foo3\n"+
//        "exportResults foo3 results.zip\n"+
//        "import foo3 /Users/ethan/Desktop/oracle.zip\n"+
//        "compareResults foo3/oracle.zip foo3/results.zip results.diff log.oracle.zip log.diff\n" +
        //"saveAllFiles\n"+
        //"openProject bar\n"+
        //"cleanProject foo3, bar\n"+
        "closeProject foo3\n"+
        "closeProject bar\n"+
        "#end"
    );
  }
  
  public void testDeleting() throws Exception{
    ScriptReader r = new ScriptReader();
    r.executeScript(
        "set autobuild\n"+
        "unset autosave\n"+
        "set compiler 1.5\n"+
        "createProject foo4\n"+
        "openProject foo4\n"+
        "touchFile foo4/Foo.java\n"+
//        "saveFile foo4/Foo.java\n"+
//        "patchFile foo4/Foo.java /Users/ethan/Desktop/patch.txt foo4\n"+
        "deleteFile foo4/Foo.java\n"+
        //"saveAllFiles\n"+
        //"openProject bar\n"+
        //"cleanProject foo, bar\n"+
        "closeProject foo4\n"+
        "closeProject bar\n"+
        "#end"
    );
  }
}
