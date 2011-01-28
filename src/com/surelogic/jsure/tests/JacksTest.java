package com.surelogic.jsure.tests;

import java.io.*;
import java.net.URL;
import java.util.zip.*;

import org.eclipse.core.runtime.CoreException;

import edu.cmu.cs.fluid.util.*;

import junit.framework.*;
import tcl.lang.*;

/**
 * Code to run the Jacks test suite on our front end 
 */
public class JacksTest extends TestCase {
  private static final String JACKS_PREFIX = "jacks/tests/";
  static final String tcltestScript = getScript("tcltest");
  static final String jacksScript   = getScript("jacks");
  static final String utilsScript   = getScript("utils");
  final Interp interp = new Interp();
  
	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
	    // Nothing to do
	}

	/**
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
	    // Nothing to do
	}

  public void testTcl() throws Throwable {
    String thestr  = "noggy";
    String script  = "string length \"" + thestr + "\"";
    TclObject rv   = interpretScript(script, true);
    int thestr_len = rv == null ? -1 : TclInteger.get(interp, rv);

    System.out.println("string length was " + thestr_len);  
    System.out.println();
    assertEquals(thestr.length(), thestr_len);
  }

  private TclObject interpretScript(String script) {
    return interpretScript(script, false);
  }
  
  private TclObject interpretScript(String script, boolean dispose) {  
    try {
      interp.eval(script);
      return interp.getResult();
    } catch (TclException ex) {
      handleTclException(interp, ex);
    } finally {
      if (dispose) {
        interp.dispose();
      }
    }
    return null;
  }
  
  private void handleTclException(Interp interp, TclException ex) {
    int code = ex.getCompletionCode();
    switch (code) {
        case TCL.ERROR:
            System.err.println(interp.getResult().toString());
            break;
        case TCL.BREAK:
            System.err.println(
                    "invoked \"break\" outside of a loop");
            break;
        case TCL.CONTINUE:
            System.err.println(
                    "invoked \"continue\" outside of a loop");
            break;
        default:
            System.err.println(
                    "command returned bad error code: " + code);
            break;
    }
  }

  private static URL getResource(String name) {
    return Activator.getDefault().getBundle().getResource(name);
  }

  public void testGetResource() {
    assertNotNull("URL: "+getResource("/lib/tcl/tcltest.tcl"));
  }
  
  private static String readURLasString(URL url) throws IOException {    
    InputStream is = url.openStream();
    return readStreamAsString(is);
  }

  private static String readStreamAsString(InputStream is) throws IOException {
    Reader r       = new InputStreamReader(is);
    return TextFileToString.readFile(r);
  }
  
  private static String getScript(String name) {
    URL res = getResource("/lib/tcl/"+name+".tcl");
    try {
      return readURLasString(res);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
  
  public void testCreateScratchProject() throws CoreException {
    assert(CalledFromTcl.ensureScratchProjectExists());
  }
  
  public void testTclTest() throws Throwable {
    String script = 
      "tcltest::test 9.1.1-1 {should generate error on synchronized interface} {"+
      "  saveas SynchronizedInterface.java "+
      "  {synchronized interface SynchronizedInterface {}} \n"+
      "  compile SynchronizedInterface.java \n"+
      "} FAIL";

    int rv = runTestcase("testTclTest", script);
    System.out.println("Final result :       "+rv);
    assertEquals(0, rv);
  }

  private void printIfNotEmpty(String msg, TclObject rv) {
    String result = rv.toString();
    if (result.length() != 0) {
      System.out.println(msg+result);
    }
  }
  
  private int runTestcase(String label, String script) throws IOException {
    if (script == null || script.equals("")) {
      return 0;
    }
    if (!Interp.commandComplete(script)) {
      System.out.println(label+": Skipping due to missing brace");
      return 0;
    }
    
    TclObject rv1 = interpretScript(tcltestScript);
    TclObject rv2 = interpretScript(jacksScript);
    TclObject rv3 = interpretScript(utilsScript);
    TclObject rv4 = interpretScript(script); 
    printIfNotEmpty("Result from tcltest: ", rv1);
    printIfNotEmpty("Result from jacks:   ", rv2);
    printIfNotEmpty("Result from utils:   ", rv3);
    try {
      return TclInteger.get(interp, rv4);
    } catch (TclException e) {
      e.printStackTrace();
      return -1;
    }    
  }
  
  public void testTclFailure() throws Throwable {
    String script = 
      "tcltest::test 9.1.1-1 {should generate error on synchronized interface} {"+
      "  foo SynchronizedInterface.java "+
      "  {synchronized interface SynchronizedInterface {}} \n"+
      "  bar SynchronizedInterface.java \n"+
      "} FAIL";

    int rv = runTestcase("testTclFailure", script);
    System.out.println("Final result :       "+rv);
    assertEquals(rv, 1);
  }
  
  static class TestData {  
    private final ZipEntry entry;
    private final String script;
    
    TestData(ZipEntry e, String s) {
      entry  = e;
      script = s;
    }

    public String getName() {
      return entry.getName();
    }

    public String getScript() {
      return script;
    }

    public int getSize() {
      long size = entry.getSize();
      if (size > Integer.MAX_VALUE || size < 0) {
        return -1;
      }
      return (int) size;
    }
  }
  
  private Iteratable<TestData> getJacksTests() {
    return getJacksTests(false);
  }
  
  private Iteratable<TestData> getJacksTests(final boolean read) {
    URL res = getResource("/lib/jacks.zip");
    try {
      final InputStream is     = res.openStream();
      final ZipInputStream zip = new ZipInputStream(is);
      return new SimpleRemovelessIterator<TestData>() {
        @Override
        protected Object computeNext() {
          ZipEntry e         = null;
          try {
            while ((e = zip.getNextEntry()) != null) {
              String name = e.getName();
              if (name.startsWith(JACKS_PREFIX) && name.endsWith("/tests.tcl")) {
                if (read) {
                  long size  = e.getSize();
                  String script;
                  if (size == -1) {
                    script = readStreamAsString(zip);
                  } else {
                    byte[] buf = new byte[(int) size];
                    zip.read(buf);
                    script = new String(buf);
                  }
                  return new TestData(e, script);
                } else {
                  return new TestData(e, null);
                }                
              }
            }
          } catch (IOException ioe) {
            ioe.printStackTrace();
            fail(ioe.getMessage());
          }
          return IteratorUtil.noElement;
        } 
      };
    } catch (IOException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    return null;
  }
  
  public void testJacksArchive() {
    for(TestData td : getJacksTests()) {
      int size    = td.getSize();
      String name = td.getName();

      if (size < 0 && size != -1) {
        fail("unknown size: "+name.substring(JACKS_PREFIX.length()));
      }
    }
  }
  
	public void testFrontend() throws Throwable {
    for(TestData td : getJacksTests(true)) {
      String name = td.getName();
      /*
      System.out.println(name.substring(JACKS_PREFIX.length())+": ");
      System.out.println(td.getScript());
      */
      System.out.println();

      // Eliminate backslash/CR/newline
      String script = td.getScript().replace("\\\015\n", "");
      /*
      int backslash = script.indexOf('\\');
      if (backslash >= 0) {
        System.out.println(+ script.charAt(backslash+1));
        System.out.println(+ script.charAt(backslash+2));
        System.out.println(+ '\015');
        System.out.println(+ '\n');
        fail();
      }
      */
      int rv = runTestcase(name, script);
      assertEquals(name, 0, rv);
    }
	}
}
