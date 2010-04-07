/**
 * Tests the matching capabilities of the scoped promises
 */
package com.surelogic.jsure.tests;

import java.io.StringReader;

import junit.framework.TestCase;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.promise.*;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.parse.ScopedPromiseAdaptor;
import com.surelogic.annotation.parse.ScopedPromiseParse;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.parse.AstGen;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.ParseException;

/**
 * @author ethan
 * 
 */
public class TestScopedPromiseMatching extends TestCase {

	/**
	 * @param name
	 */
	public TestScopedPromiseMatching(String name) {
		super(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testBasicParsing() {
		try {
			ScopedPromiseParse.main(new String[0]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception thrown in ScopedPromiseParse: " + e.getMessage());
		}
	}

	public void testConstructorMatching() {
		String testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
				+ "public Foo(){}\n" + "}";

		try {
			IRNode compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			ConstructorDeclPatternNode cNode = createConstructorDeclPattern("@Reads(Instance) for public new () in Foo");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					assertTrue(cNode.matches(constructor));
				}
			}

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "private Foo(){}\n" + "protected Foo() {}\n"
					+ "public Foo(int i){}\n" + "}";

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					assertFalse(cNode.matches(constructor));
				}
			}

			cNode = createConstructorDeclPattern("@Reads(Instance) for new() in Foo");

			int count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					count++;
					if (count != 3) {
						assertTrue(cNode.matches(constructor));
					} else {
						assertFalse(cNode.matches(constructor));
					}
				}
			}

			cNode = createConstructorDeclPattern("@Reads(Instance) for public new(int) in Foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					count++;
					if (count != 3) {
						assertFalse(cNode.matches(constructor));
					} else {
						assertTrue(cNode.matches(constructor));
					}
				}
			}

			cNode = createConstructorDeclPattern("@Reads(Instance) for private new() in Foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					count++;
					if (count != 1) {
						assertFalse(cNode.matches(constructor));
					} else {
						assertTrue(cNode.matches(constructor));
					}
				}
			}

			cNode = createConstructorDeclPattern("@Reads(Instance) for protected new() in Foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					count++;
					if (count != 2) {
						assertFalse(cNode.matches(constructor));
					} else {
						assertTrue(cNode.matches(constructor));
					}
				}
			}

			cNode = createConstructorDeclPattern("@Reads(Instance) for new(**) in Foo");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					assertTrue(cNode.matches(constructor));
				}
			}

			cNode = createConstructorDeclPattern("@Reads(Instance) for private new() in Foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					count++;
					if(count != 1){
  					assertFalse(cNode.matches(constructor));
					}else{
  					assertTrue(cNode.matches(constructor));
					}
				}
			}

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "private Foo(int i, char c) throws IllegalCastException{}\n"
					+ "protected Foo() {}\n" + "public Foo(int i){}\n" + "}";

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					assertFalse(cNode.matches(constructor));
				}
			}

			cNode = createConstructorDeclPattern("@Reads(Instance) for private new(**) in Foo");
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					count++;
					if (count != 1) {
						assertFalse(cNode.matches(constructor));
					} else {
						assertTrue(cNode.matches(constructor));
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	public void testConstructorMatchingWithIn() {
		String testCode = "package com.surelogic.foo;\n public class Foo{\n"
				+ "public Foo(){}\n" + "}";

		try {
			IRNode compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			ConstructorDeclPatternNode cNode = createConstructorDeclPattern("@Reads(Instance) for public new () in com.surelogic.foo.Foo");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					assertTrue(cNode.matches(constructor));
				}
			}

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "public Foo(){}\n" + "}";
			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			cNode = createConstructorDeclPattern("@Reads(Instance) for public new () in Foo in com.surelogic.foo");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					assertTrue(cNode.matches(constructor));
				}
			}

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "private Foo(){}\n" + "protected Foo() {}\n"
					+ "public Foo(int i){}\n" + "}";

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					assertFalse(cNode.matches(constructor));
				}
			}

			// **.Foo means a nested class
			cNode = createConstructorDeclPattern("@Reads(Instance) for new() in Foo");

			int count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					count++;
					if (count != 3) {
						assertTrue(cNode.matches(constructor));
					} else {
						assertFalse(cNode.matches(constructor));
					}
				}
			}

			cNode = createConstructorDeclPattern("@Reads(Instance) for public new(int) in Foo in **");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					count++;
					if (count != 3) {
						assertFalse(cNode.matches(constructor));
					} else {
						assertTrue(cNode.matches(constructor));
					}
				}
			}

			// Fail, bad package name
			cNode = createConstructorDeclPattern("@Reads(Instance) for private new() in *Foo* in com.*");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					count++;
					assertFalse(cNode.matches(constructor));
				}
			}

			cNode = createConstructorDeclPattern("@Reads(Instance) for protected new() in Foo in **.foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					count++;
					if (count != 2) {
						assertFalse(cNode.matches(constructor));
					} else {
						assertTrue(cNode.matches(constructor));
					}
				}
			}

			cNode = createConstructorDeclPattern("@Reads(Instance) for new(**) in Foo in com.*.foo");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					assertTrue(cNode.matches(constructor));
				}
			}

			cNode = createConstructorDeclPattern("@Reads(Instance) for new(**) in Foo in com.*");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					assertFalse(cNode.matches(constructor));
				}
			}

			cNode = createConstructorDeclPattern("@Reads(Instance) for new() in **");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					count++;
					if (count < 3) {
						assertTrue(cNode.matches(constructor));
					} else {
						assertFalse(cNode.matches(constructor));
					}
				}
			}

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "private Foo(int i, char c) {}\n" + "protected Foo() {}\n"
					+ "public Foo(int i){}\n" + "}";

			count = 0;
			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					count++;
					if (count == 2) {
						assertTrue(cNode.matches(constructor));
					} else {
						assertFalse(cNode.matches(constructor));
					}
				}
			}

			cNode = createConstructorDeclPattern("@Reads(Instance) for new(**) in Foo in com.**");
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					count++;
					System.err.println("Count = " + count);
					assertTrue(cNode.matches(constructor));
				}
			}

			cNode = createConstructorDeclPattern("@Reads(Instance) for new(**) in *Foo* in com.**");
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					count++;
					assertTrue(cNode.matches(constructor));
				}
			}

			cNode = createConstructorDeclPattern("@Reads(Instance) for new(**) in * in **.foo");
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode constructor : VisitUtil.getClassConstructors(typeDecl)) {
					count++;
					assertTrue(cNode.matches(constructor));
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	public void testMethodMatching() {
		String testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
				+ "public Foo(){}\n" + "public void bar(){}\n" + "}";

		try {
			IRNode compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			MethodDeclPatternNode mNode = createMethodDeclPattern("@Reads(Instance) for public bar()");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						assertTrue(mNode.matches(method));
					}
				}
			}

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "public Foo(){}\n" + "private void bar(){}\n"
					+ "private void bar() {}\n" + "protected int bar() {}\n"
					+ "protected char bar() {}\n" + "protected String bar() {}\n"
					+ "public void bar(int i){}\n" + "}";

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			int count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						assertFalse(mNode.matches(method));
					}
				}
			}

			mNode = createMethodDeclPattern("@Reads(Instance) for bar()");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						if (count != 6) {
							assertTrue(mNode.matches(method));
						} else {
							assertFalse(mNode.matches(method));
						}
					}
				}
			}

			mNode = createMethodDeclPattern("@Reads(Instance) for bar(int)");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						if (count == 6) {
							assertTrue(mNode.matches(method));
						} else {
							assertFalse(mNode.matches(method));
						}
					}
				}
			}

			mNode = createMethodDeclPattern("@Reads(Instance) for bar(**)");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						assertTrue(mNode.matches(method));
					}
				}
			}

			mNode = createMethodDeclPattern("@Reads(Instance) for public bar(**) ");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						if(count != 6){
  						assertFalse(mNode.matches(method));
						}
						else{
  						assertTrue(mNode.matches(method));
						}
					}
				}
			}
			
			mNode = createMethodDeclPattern("@Reads(Instance) for *(**) ");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						assertTrue(mNode.matches(method));
					}
				}
			}


			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "public Foo(){}\n" + "private void bar(){}\n"
					+ "private void bar() throws InvalidStateException, Exception {}\n"
					+ "protected int bar() {}\n"
					+ "protected char bar() throws Exception{}\n"
					+ "protected int foo() {}\n"
					+ "protected char foo() throws Exception{}\n"
					+ "protected char foobar() throws Exception{}\n"
					+ "protected void fubar() throws Exception{}\n"
					+ "protected int fubar() throws Exception{}\n"
					+ "protected String bar() {}\n" + "public void bar(int i){}\n" + "}";
			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			mNode = createMethodDeclPattern("@Reads(Instance) for protected foo*(**) ");
			
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						if (count == 5 || count == 6 || count ==7) {
							assertTrue(mNode.matches(method));
						} else {
							assertFalse(mNode.matches(method));
						}
					}
				}
			}
			
			mNode = createMethodDeclPattern("@Reads(Instance) for protected *bar(**) ");
			
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						if (count == 1 || count == 2 || count == 11 || count == 5 || count == 6 ) {
							assertFalse(mNode.matches(method));
						} else {
							assertTrue(mNode.matches(method));
						}
					}
				}
			}

			mNode = createMethodDeclPattern("@Reads(Instance) for bar()");

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "public Foo(){}\n" + "private void bar(){}\n"
					+ "private void bar() {}\n" + "protected int bar() {}\n"
					+ "protected void foobar() throws Exception{}\n"
					+ "protected String fubar() {}\n" + "public void blargh(int i){}\n"
					+ "}";
			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						if (count <= 3) {
							assertTrue(mNode.matches(method));
						} else {
							assertFalse(mNode.matches(method));
						}
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		}
	}
	
	
	public void testMethodMatchingWithIn() {
		String testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
				+ "public Foo(){}\n" + "public void bar(){}\n" + "}";

		try {
			IRNode compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			MethodDeclPatternNode mNode = createMethodDeclPattern("@Reads(Instance) for public bar() in com.surelogic.foo.Foo");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						assertTrue(mNode.matches(method));
					}
				}
			}

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "public Foo(){}\n" + "private void bar(){}\n"
					+ "private void bar() {}\n" + "protected int bar() {}\n"
					+ "protected char bar() {}\n" + "protected String bar() {}\n"
					+ "public void bar(int i){}\n" + "}";

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			int count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						assertFalse(mNode.matches(method));
					}
				}
			}

			mNode = createMethodDeclPattern("@Reads(Instance) for bar() in Foo in com.surelogic.*");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						if (count != 6) {
							assertTrue(mNode.matches(method));
						} else {
							assertFalse(mNode.matches(method));
						}
					}
				}
			}

			mNode = createMethodDeclPattern("@Reads(Instance) for bar(int) in Foo in com.surelogic.foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						if (count == 6) {
							assertTrue(mNode.matches(method));
						} else {
							assertFalse(mNode.matches(method));
						}
					}
				}
			}

			mNode = createMethodDeclPattern("@Reads(Instance) for bar(**) in Foo in **");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						assertTrue(mNode.matches(method));
					}
				}
			}

			mNode = createMethodDeclPattern("@Reads(Instance) for bar(**) in * in com.surelogic.*");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						assertTrue(mNode.matches(method));
					}
				}
			}

			mNode = createMethodDeclPattern("@Reads(Instance) for bar(**) in F*o in **.foo");

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "public Foo(){}\n" + "private void bar(){}\n"
					+ "private void bar() throws InvalidStateException, Exception {}\n"
					+ "protected int bar() {}\n"
					+ "protected char bar() throws Exception{}\n"
					+ "protected void foobar() {}\n"
					+ "protected String bar() {}\n" + "public void bar(int i){}\n" + "}";
			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						System.out.println("Count: " + count);
						if (count == 5) {
							assertFalse(mNode.matches(method));
						} else {
							assertTrue(mNode.matches(method));
						}
					}
				}
			}

			mNode = createMethodDeclPattern("@Reads(Instance) for bar() in *Foo in *.surelogic.*");

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "public Foo(){}\n" + "private void bar(){}\n"
					+ "private void bar() {}\n" + "protected int bar() {}\n"
					+ "protected void foobar() throws Exception{}\n"
					+ "protected String fubar() {}\n" + "public void blargh(int i){}\n"
					+ "}";
			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						if (count <= 3) {
							assertTrue(mNode.matches(method));
						} else {
							assertFalse(mNode.matches(method));
						}
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		}
	}
	

	public void testFieldMatching() {

		String testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
				+ "private String string;\n" + "public Foo(){}\n"
				+ "public void bar(){}\n" + "}";

		try {
			IRNode compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			FieldDeclPatternNode fNode = createFieldDeclPattern("@Reads(Instance) for String string in Foo");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					assertTrue(fNode.matches(method));
				}
			}

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "private String string;\n" + "private static Integer integer;\n"
					+ "private final static Foo instance = new Foo();\n"
					+ "public Foo(){}\n" + "private void bar(){}\n"
					+ "private void bar() {}\n" + "protected int bar() {}\n"
					+ "protected char bar() {}\n" + "protected String bar() {}\n"
					+ "public void bar(int i){}\n" + "}";

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			int count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					if (count == 1) {
						assertTrue(fNode.matches(method));
					} else {
						assertFalse(fNode.matches(method));
					}
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for public * * in Foo");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					assertFalse(fNode.matches(method));
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for private * * in Foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					System.out.println("Count: " + count);
					assertTrue(fNode.matches(method));
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for static * * in Foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					if (count >= 2) {
						assertTrue(fNode.matches(method));
					} else {
						assertFalse(fNode.matches(method));
					}
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for Foo instance in Foo");

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					if (count == 3) {
						assertTrue(fNode.matches(method));
					} else {
						assertFalse(fNode.matches(method));
					}
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for static * * in Foo");

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					System.out.println(count);
					if (count >= 2) {
						assertTrue(fNode.matches(method));
					} else {
						assertFalse(fNode.matches(method));
					}
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for * * in Foo");

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					System.out.println("Count: " + count);
					assertTrue(fNode.matches(method));
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for * st* in Foo");

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					System.out.println("Count: " + count);
					if (count == 1) {
						assertTrue(fNode.matches(method));
					} else {
						assertFalse(fNode.matches(method));
					}
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for * *ing in Foo");

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					System.out.println("Count: " + count);
					if (count == 1) {
						assertTrue(fNode.matches(method));
					} else {
						assertFalse(fNode.matches(method));
					}
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for * s*g in Foo");

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					System.out.println("Count: " + count);
					if (count == 1) {
						assertTrue(fNode.matches(method));
					} else {
						assertFalse(fNode.matches(method));
					}
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for * * in Bar");

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					System.out.println("count: " + count);
					assertFalse(fNode.matches(method));
				}
			}

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "private String string1;\n" + "private String string2;\n"
					+ "private String string3;\n" + "private String stringString1;\n"
					+ "private String stringString2;\n"
					+ "private String stringString3;\n"
					+ "public static Integer integer;\n"
					+ "private final static Foo instance;\n" + "public Foo(){}\n"
					+ "protected String bar() {}\n" + "public void bar(int i){}\n" + "}";

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			fNode = createFieldDeclPattern("@Reads(Instance) for private String *1 in Foo");
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					System.out.println("count: " + count);
					if (count == 1 || count == 4) {
						assertTrue(fNode.matches(method));
					} else {
						assertFalse(fNode.matches(method));
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	public void testFieldMatchingWithIn() {

		String testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
				+ "private String string;\n" + "public Foo(){}\n"
				+ "public void bar(){}\n" + "}";

		try {
			IRNode compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			FieldDeclPatternNode fNode = createFieldDeclPattern("@Reads(Instance) for String string in com.surelogic.foo.Foo");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					assertTrue(fNode.matches(method));
				}
			}
			
			fNode = createFieldDeclPattern("@Reads(Instance) for String string in Foo in com.surelogic.foo");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					assertTrue(fNode.matches(method));
				}
			}
			
			fNode = createFieldDeclPattern("@Reads(Instance) for String string in Foo");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					assertTrue(fNode.matches(method));
				}
			}
			
			fNode = createFieldDeclPattern("@Reads(Instance) for String string in Foo in **");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					assertTrue(fNode.matches(method));
				}
			}
			
			fNode = createFieldDeclPattern("@Reads(Instance) for String string in Foo in *");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					//FIXME
					assertTrue(fNode.matches(method));
				}
			}
			
			fNode = createFieldDeclPattern("@Reads(Instance) for String string in Foo in com.**");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					assertTrue(fNode.matches(method));
				}
			}
			
			fNode = createFieldDeclPattern("@Reads(Instance) for String string in Foo in com.surelogic*.**");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					assertTrue(fNode.matches(method));
				}
			}
			
			fNode = createFieldDeclPattern("@Reads(Instance) for String string in *Foo* in com.surelogic.**");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					assertTrue(fNode.matches(method));
				}
			}
			
			fNode = createFieldDeclPattern("@Reads(Instance) for String string in Foo in **.foo");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					assertTrue(fNode.matches(method));
				}
			}
			
			fNode = createFieldDeclPattern("@Reads(Instance) for String string in Foo in com.surelogic.*");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					assertTrue(fNode.matches(method));
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for String string in com.*.Foo ");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					assertFalse(fNode.matches(method));
				}
			}
			
			fNode = createFieldDeclPattern("@Reads(Instance) for String string in Foo ");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					assertTrue(fNode.matches(method));
				}
			}
			
			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "private String string;\n" + "private static Integer integer;\n"
					+ "private final static Foo instance = new Foo();\n"
					+ "public Foo(){}\n" + "private void bar(){}\n"
					+ "private void bar() {}\n" + "protected int bar() {}\n"
					+ "protected char bar() {}\n" + "protected String bar() {}\n"
					+ "public void bar(int i){}\n" + "}";

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			int count = 0;
			/*
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					assertTrue(fNode.matches(method));
				}
			}
			*/

			fNode = createFieldDeclPattern("@Reads(Instance) for public * * in Foo in **");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					assertFalse(fNode.matches(method));
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for private * * in Foo in **.foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					System.out.println("Count: " + count);
					assertTrue(fNode.matches(method));
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for static * * in Foo in **.foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					if (count >= 2) {
						assertTrue(fNode.matches(method));
					} else {
						assertFalse(fNode.matches(method));
					}
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for Foo instance in Foo in com.**");

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					if (count == 3) {
						assertTrue(fNode.matches(method));
					} else {
						assertFalse(fNode.matches(method));
					}
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for static * * in Foo in **");

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					System.out.println(count);
					if (count >= 2) {
						assertTrue(fNode.matches(method));
					} else {
						assertFalse(fNode.matches(method));
					}
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for * * in Foo in com.surelogic.*");

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					System.out.println("Count: " + count);
					assertTrue(fNode.matches(method));
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for * st* in *Foo* in com.surelogic.foo*");

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					System.out.println("Count: " + count);
					if (count == 1) {
						assertTrue(fNode.matches(method));
					} else {
						assertFalse(fNode.matches(method));
					}
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for * *ing in * in com.surelogic.foo");

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					System.out.println("Count: " + count);
					if (count == 1) {
						assertTrue(fNode.matches(method));
					} else {
						assertFalse(fNode.matches(method));
					}
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for * s*g in Foo in com.sure*.*");

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					System.out.println("Count: " + count);
					if (count == 1) {
						assertTrue(fNode.matches(method));
					} else {
						assertFalse(fNode.matches(method));
					}
				}
			}

			fNode = createFieldDeclPattern("@Reads(Instance) for * * in Bar in com.surelogic.foo");

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					System.out.println("count: " + count);
					assertFalse(fNode.matches(method));
				}
			}

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "private String string1;\n" + "private String string2;\n"
					+ "private String string3;\n" + "private String stringString1;\n"
					+ "private String stringString2;\n"
					+ "private String stringString3;\n"
					+ "public static Integer integer;\n"
					+ "private final static Foo instance;\n" + "public Foo(){}\n"
					+ "protected String bar() {}\n" + "public void bar(int i){}\n" + "}";

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			fNode = createFieldDeclPattern("@Reads(Instance) for private String *1 in Foo in com*.surelogic*.foo*");
			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					System.out.println("count: " + count);
					if (count == 1 || count == 4) {
						assertTrue(fNode.matches(method));
					} else {
						assertFalse(fNode.matches(method));
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		}
	}
	
	public void testTypeMatching() {

		String testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
				+ "public Foo(){}\n" + "public void bar(){}\n" + "}";

		try {
			IRNode compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			TypeDeclPatternNode tNode = createTypeDeclPattern("@Reads(Instance) for Foo");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					assertTrue(tNode.matches(typeDecl));
				}
			}

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "public Foo(){}\n" + "private void bar(){}\n" + "}"
					+ "public class Bar{\n" + "public Bar(){}\n" + "}";

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			int count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					count++;
					if (count == 1) {
						assertTrue(tNode.matches(typeDecl));

					} else {
						assertFalse(tNode.matches(typeDecl));
					}
				}
			}

			tNode = createTypeDeclPattern("@Reads(Instance) for Bar");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					count++;
					if (count == 2) {
						assertTrue(tNode.matches(typeDecl));

					} else {
						assertFalse(tNode.matches(typeDecl));
					}
				}
			}

			tNode = createTypeDeclPattern("@Reads(Instance) for private Foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					count++;
					assertFalse(tNode.matches(typeDecl));
				}
			}

			tNode = createTypeDeclPattern("@Reads(Instance) for protected Foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					count++;
					assertFalse(tNode.matches(typeDecl));
				}
			}

			tNode = createTypeDeclPattern("@Reads(Instance) for public static Foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					count++;
					assertFalse(tNode.matches(typeDecl));
				}
			}

			tNode = createTypeDeclPattern("@Reads(Instance) for *");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					count++;
					assertTrue(tNode.matches(typeDecl));
				}
			}

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "public Foo(){}\n" + "private void bar(){}\n" + "}"
					+ "public class BigFoo{\n" + "public BigFoo(){}\n"
					+ "private void bar(){}\n" + "}" + "public class LittleFoo{\n"
					+ "public LittleFoo(){}\n" + "private void bar(){}\n" + "}"
					+ "public class IPityDaFoo{\n" + "public IPityDaFoo(){}\n"
					+ "private void bar(){}\n" + "}" + "public class Bar{\n"
					+ "public Bar(){}\n" + "}";

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			tNode = createTypeDeclPattern("@Reads(Instance) for *Foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					count++;
					if (count <= 4) {
						assertTrue(tNode.matches(typeDecl));
					} else {
						assertFalse(tNode.matches(typeDecl));
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		}
	}
	
	
	public void testTypeMatchingWithIn() {

		String testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
				+ "public Foo(){}\n" + "public void bar(){}\n" + "}";

		try {
			IRNode compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			TypeDeclPatternNode tNode = createTypeDeclPattern("@Reads(Instance) for Foo in com.surelogic.foo");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					assertTrue(tNode.matches(typeDecl));
				}
			}

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "public Foo(){}\n" + "private void bar(){}\n" + "}"
					+ "public class Bar{\n" + "public Bar(){}\n" + "}";

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			int count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					count++;
					if (count == 1) {
						assertTrue(tNode.matches(typeDecl));

					} else {
						assertFalse(tNode.matches(typeDecl));
					}
				}
			}

			tNode = createTypeDeclPattern("@Reads(Instance) for Bar in com.surelogic.foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					count++;
					if (count == 2) {
						assertTrue(tNode.matches(typeDecl));

					} else {
						assertFalse(tNode.matches(typeDecl));
					}
				}
			}

			tNode = createTypeDeclPattern("@Reads(Instance) for private Foo in com.**");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					count++;
					assertFalse(tNode.matches(typeDecl));
				}
			}

			tNode = createTypeDeclPattern("@Reads(Instance) for protected Foo in com.*.foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					count++;
					assertFalse(tNode.matches(typeDecl));
				}
			}

			tNode = createTypeDeclPattern("@Reads(Instance) for public static * in com.surelogic.foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					count++;
					assertFalse(tNode.matches(typeDecl));
				}
			}

			tNode = createTypeDeclPattern("@Reads(Instance) for * in com.**");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					count++;
					assertTrue(tNode.matches(typeDecl));
				}
			}

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "public Foo(){}\n" + "private void bar(){}\n" + "}"
					+ "public class BigFoo{\n" + "public BigFoo(){}\n"
					+ "private void bar(){}\n" + "}" + "public class LittleFoo{\n"
					+ "public LittleFoo(){}\n" + "private void bar(){}\n" + "}"
					+ "public class IPityDaFoo{\n" + "public IPityDaFoo(){}\n"
					+ "private void bar(){}\n" + "}" + "public class Bar{\n"
					+ "public Bar(){}\n" + "}";

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			tNode = createTypeDeclPattern("@Reads(Instance) for *Foo in **");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					count++;
					if (count <= 4) {
						assertTrue(tNode.matches(typeDecl));
					} else {
						assertFalse(tNode.matches(typeDecl));
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		}
	}
	

	public void testAnd() {

		String testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
				+ "private String string;\n" + "public Foo(){}\n"
				+ "public void bar(){}\n" + "}";

		try {
			IRNode compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			AndTargetNode aNode = createAndTargetNode("@Reads(Instance) for String string & bar()");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						assertFalse(aNode.matches(method));
					}
				}
				for (IRNode field : VisitUtil.getClassFieldDecls(typeDecl)) {
					// assertFalse(aNode.matches(field));
				}
			}

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "private String string1;\n" 
					+ "private String string2;\n"
					+ "private String string3;\n" 
					+ "private String string31;\n" 
					+ "private String stringString1;\n"
					+ "private String stringString2;\n"
					+ "private String stringString3;\n"
					+ "private String stringString31;\n"
					+ "public static Integer integer;\n"
					+ "private final static Foo instance;\n" + "public Foo(){}\n"
					+ "protected String bar() {}\n" + "public void bar(int i){}\n" + "}";

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			aNode = createAndTargetNode("@Reads(Instance) for private String *1 & !(private String *31)");

			int count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode field : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					if (count == 1 || count == 5){
						assertTrue(aNode.matches(field));
					} else {
						assertFalse(aNode.matches(field));
					}
				}
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						assertFalse(aNode.matches(method));
					}
				}
			}

			aNode = createAndTargetNode("@Reads(Instance) for private String string* & !(private String *3)");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode field : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					if (count == 1 || count == 2 || count == 4 || count == 5 || count == 6 || count == 8) {
						assertTrue(aNode.matches(field));
					} else {
						assertFalse(aNode.matches(field));
					}
				}
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						assertFalse(aNode.matches(method));
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	public void testOr() {
		String testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
				+ "private String string;\n" + "public Foo(){}\n"
				+ "public void bar(){}\n" + "}";

		try {
			IRNode compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			OrTargetNode oNode = createOrTargetNode("@Reads(Instance) for String string | bar()");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						assertTrue(oNode.matches(method));
					}
				}
				for (IRNode field : VisitUtil.getClassFieldDecls(typeDecl)) {
					assertTrue(oNode.matches(field));
				}
			}

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "private String string1;\n" + "private String string2;\n"
					+ "private String string3;\n" + "private String stringString1;\n"
					+ "private String stringString2;\n"
					+ "private String stringString3;\n"
					+ "public static Integer integer;\n"
					+ "private final static Foo instance;\n" + "public Foo(){}\n"
					+ "protected String bar() {}\n" + "public void bar(int i){}\n" + "}";

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			oNode = createOrTargetNode("@Reads(Instance) for private String * | public Integer *");

			int count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode field : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					if (count <= 7) {
						assertTrue(oNode.matches(field));
					} else {
						assertFalse(oNode.matches(field));
					}
				}
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						assertFalse(oNode.matches(method));
					}
				}
			}

			oNode = createOrTargetNode("@Reads(Instance) for bar(**) | public Foo");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					count++;
					assertTrue(oNode.matches(typeDecl));
				}

				for (IRNode field : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					assertFalse(oNode.matches(field));
				}
				count = 0;
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						assertTrue(oNode.matches(method));
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	public void testNot() {
		String testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
				+ "private String string;\n" + "public Foo(){}\n"
				+ "public void bar(){}\n" + "}";

		try {
			IRNode compUnit = AstGen.genCompilationUnit(new StringReader(testCode));

			NotTargetNode nNode = createNotTargetNode("@Reads(Instance) for !(String string)");

			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						assertTrue(nNode.matches(method));
					}
				}
				for (IRNode field : VisitUtil.getClassFieldDecls(typeDecl)) {
					assertFalse(nNode.matches(field));
				}
			}

			testCode = "package com.surelogic.foo;\n" + "public class Foo{\n"
					+ "private String string1;\n" + "private String string2;\n"
					+ "private String string3;\n" + "private String stringString1;\n"
					+ "private String stringString2;\n"
					+ "private String stringString3;\n"
					+ "public static Integer integer;\n"
					+ "private final static Foo instance;\n" + "public Foo(){}\n"
					+ "protected String bar() {}\n" + "public void bar(int i){}\n" + "}";

			compUnit = AstGen.genCompilationUnit(new StringReader(testCode));
			nNode = createNotTargetNode("@Reads(Instance) for !(private String *)");

			int count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				for (IRNode field : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					if (count >= 7) {
						assertTrue(nNode.matches(field));
					} else {
						assertFalse(nNode.matches(field));
					}
				}
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						assertTrue(nNode.matches(method));
					}
				}
			}

			nNode = createNotTargetNode("@Reads(Instance) for !(bar(int))");

			count = 0;
			for (IRNode typeDecl : VisitUtil.getAllTypeDecls(compUnit)) {
				if (TypeDeclaration.prototype.includes(typeDecl)) {
					count++;
					assertTrue(nNode.matches(typeDecl));
				}

				for (IRNode field : VisitUtil.getClassFieldDecls(typeDecl)) {
					count++;
					assertTrue(nNode.matches(field));
				}
				count = 0;
				for (IRNode method : VisitUtil.getClassMethods(typeDecl)) {
					if (MethodDeclaration.prototype.includes(method)) {
						count++;
						if (count == 1) {
							assertTrue(nNode.matches(method));
						} else {
							assertFalse(nNode.matches(method));
						}
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());
		}

	}

	private ConstructorDeclPatternNode createConstructorDeclPattern(
			String annoText) throws RecognitionException, Exception {
		return (ConstructorDeclPatternNode) createScopedPromise(annoText);
	}

	/**
	 * @param string
	 * @return
	 * @throws Exception
	 * @throws RecognitionException
	 */
	private MethodDeclPatternNode createMethodDeclPattern(String string)
			throws RecognitionException, Exception {
		return (MethodDeclPatternNode) createScopedPromise(string);
	}

	/**
	 * @param string
	 * @return
	 * @throws Exception
	 * @throws RecognitionException
	 */
	private FieldDeclPatternNode createFieldDeclPattern(String string)
			throws RecognitionException, Exception {
		return (FieldDeclPatternNode) createScopedPromise(string);
	}

	/**
	 * @param string
	 * @return
	 * @throws Exception
	 * @throws RecognitionException
	 */
	private TypeDeclPatternNode createTypeDeclPattern(String string)
			throws RecognitionException, Exception {
		return (TypeDeclPatternNode) createScopedPromise(string);
	}

	/**
	 * @param string
	 * @return
	 * @throws Exception
	 * @throws RecognitionException
	 */
	private AndTargetNode createAndTargetNode(String string)
			throws RecognitionException, Exception {
		return (AndTargetNode) createScopedPromise(string);
	}

	/**
	 * @param string
	 * @return
	 * @throws Exception
	 * @throws RecognitionException
	 */
	private OrTargetNode createOrTargetNode(String string)
			throws RecognitionException, Exception {
		return (OrTargetNode) createScopedPromise(string);
	}

	/**
	 * @param string
	 * @return
	 * @throws Exception
	 * @throws RecognitionException
	 */
	private NotTargetNode createNotTargetNode(String string)
			throws RecognitionException, Exception {
		return (NotTargetNode) createScopedPromise(string);
	}

	private PromiseTargetNode createScopedPromise(String annoText)
			throws RecognitionException, Exception {
		ScopedPromiseAdaptor.Node root = (ScopedPromiseAdaptor.Node) ScopedPromiseParse.prototype
				.initParser(annoText).scopedPromise().getTree();

//		ScopedPromiseParse.printAST(root);

		ScopedPromiseNode node = (ScopedPromiseNode) root
				.finalizeAST(IAnnotationParsingContext.nullPrototype);

		return node.getTargets();
	}
}
