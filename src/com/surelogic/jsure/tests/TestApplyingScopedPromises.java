/**
 * 
 */
package com.surelogic.jsure.tests;

import java.io.StringReader;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.parse.AstGen;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.ParseException;
import junit.framework.TestCase;

/**
 * @author ethan
 *
 */
public class TestApplyingScopedPromises extends TestCase {

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
    @Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
    @Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	@SuppressWarnings("unused")
	public void testApplyingPromises(){
		String testCode = "package com.surelogic.foo;\n" + 
		                  "@Promise (\"'reads Instance' for new()\")\n" +
		                  "public class Foo{\n"
              				+ "public Foo(){}\n" + "}";
		try {
			IRNode compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
//					AASTStore.getASTsByClass(Foo.class);
				}
				for (IRNode field : VisitUtil.getClassFieldDecls(typeDecl)) {
				    //
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
//		} catch (RecognitionException e) {
//			e.printStackTrace();
//			fail("Unexpected exception: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		}
	}
}
