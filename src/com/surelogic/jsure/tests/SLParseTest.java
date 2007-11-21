/**
 * 
 */
package com.surelogic.jsure.tests;

import junit.framework.TestCase;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.*;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.*;
import com.surelogic.annotation.rules.*;
import com.surelogic.annotation.rules.LockRulesTestHelper.*;
import com.surelogic.annotation.rules.MethodEffectsRulesHelper.Reads_ParseRuleHelper;
import com.surelogic.annotation.rules.MethodEffectsRulesHelper.Writes_ParseRuleHelper;
import com.surelogic.annotation.rules.RegionRulesTestHelper.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author ethan
 * 
 */
public class SLParseTest extends TestCase {

		/**
		 * @param name
		 */
		public SLParseTest(String name) {
				super(name);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see junit.framework.TestCase#setUp()
		 */
		@Override
		protected void setUp() throws Exception {
				super.setUp();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see junit.framework.TestCase#tearDown()
		 */
		@Override
		protected void tearDown() throws Exception {
				super.tearDown();
		}

		public void testGoodLockAAST() {
				String expected;
				try {
						expected = "LockDeclaration\n  id=L\n  ThisExpression\n  RegionName\n    id=Instance\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"L is this protects Instance").lock().getTree();
						LockDeclarationNode ldn = (LockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse(true).equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "LockDeclaration\n  id=L1\n  FieldRef\n    ThisExpression\n    id=lock\n  RegionName\n    id=region\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"L1 is lock protects region").lock().getTree();
						LockDeclarationNode ldn = (LockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse(true).equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "LockDeclaration\n  id=L1\n  FieldRef\n    ThisExpression\n    id=lock\n  RegionName\n    id=region\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"L1 is this.lock protects region").lock().getTree();
						LockDeclarationNode ldn = (LockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse(true).equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "LockDeclaration\n  id=L1\n  QualifiedThisExpression\n    NamedType\n      type=Type\n  RegionName\n    id=region\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"L1 is Type.this protects region").lock().getTree();
						LockDeclarationNode ldn = (LockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse(true).equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "LockDeclaration\n  id=L1\n  FieldRef\n    ThisExpression\n    id=lock\n  QualifiedRegionName\n    NamedType\n      type=Class1\n    id=region\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"L1 is lock protects Class1:region").lock().getTree();
						LockDeclarationNode ldn = (LockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse(true).equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "LockDeclaration\n  id=L1\n  FieldRef\n    TypeExpression\n      NamedType\n        type=Class1\n    id=lock\n  QualifiedRegionName\n    NamedType\n      type=Class1\n    id=region\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"L1 is Class1.lock protects Class1:region").lock()
										.getTree();
						LockDeclarationNode ldn = (LockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse(true).equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected =  "LockDeclaration\n" +
												"  id=L1\n"+
												"  FieldRef\n" +
												"    TypeExpression\n" +
												"      NamedType\n"+
												"        type=outer.inner\n"+
												"    id=lock\n"+
												"  QualifiedRegionName\n"+
												"    NamedType\n"+
												"      type=Class1.region\n"+
												"    id=region\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"L1 is outer.inner.lock protects Class1.region:region")
										.lock().getTree();
						LockDeclarationNode ldn = (LockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						System.out.println(expected);
						System.out.println(ldn.unparse(true));
						assertTrue(ldn.unparse(true).equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "LockDeclaration\n  id=L1\n  FieldRef\n    ThisExpression\n    id=lock\n  RegionName\n    id=[]\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"L1 is lock protects []").lock().getTree();
						LockDeclarationNode ldn = (LockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
//						System.out.println(expected);
//						System.out.println(ldn.unparse(true));
						assertTrue(ldn.unparse(true).equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
		}

		public void testBadLockAAST() {
				try {
						SLParse.initParser("").lock().getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected Exception");
				}
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"L1 is class:lock protects region").lock().getTree();
						LockDeclarationNode ldn = (LockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"L1 is Type.Type:class protects Class1:region").lock()
										.getTree();
						LockDeclarationNode ldn = (LockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"L1 is class.class protects region").lock().getTree();
						LockDeclarationNode ldn = (LockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				/*
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"L1 is this.class protects region").lock().getTree();
						LockDeclarationNode ldn = (LockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				*/
				try {
						SLParse.initParser("is this protects Instance").lock().getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected Exception");
				}
				try {
						SLParse.initParser("L is protects Instance").lock().getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected Exception");
				}
				try {
						SLParse.initParser("L is this Instance").lock().getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected Exception");
				}
				try {
						SLParse.initParser("L this protects Instance").lock().getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected Exception");
				}
				try {
						SLParse.initParser("L is this protects").lock().getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected Exception");
				}
				try {
						SLParse.initParser("L is this protects").lock().getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected Exception");
				}
		}

		public void testGoodRegionAAST() {
				String expected;
				try {
						expected = "NewRegionDeclaration\n" +
											 "  modifiers=" + JavaNode.PUBLIC + "\n" +
											 "  id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"public region1").region().getTree();
						RegionDeclarationNode ldn = (RegionDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "NewRegionDeclaration\n" +
											 "  modifiers=" + JavaNode.PROTECTED + "\n" +
											 "  id=region1\n" +
											 "  RegionName\n" +
											 "    id=region2\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"protected region1 extends region2").region().getTree();
						RegionDeclarationNode ldn = (RegionDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "NewRegionDeclaration\n" +
											 "  modifiers=" + (JavaNode.PRIVATE | JavaNode.STATIC) + "\n" +
											 "  id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"private static region1").region().getTree();
						RegionDeclarationNode ldn = (RegionDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "NewRegionDeclaration\n" +
											 "  modifiers="+ JavaNode.STATIC + "\n" +
											 "  id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"static region1").region().getTree();
						RegionDeclarationNode ldn = (RegionDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "NewRegionDeclaration\n" +
											 "  modifiers=" + (JavaNode.PUBLIC | JavaNode.STATIC)+"\n" +
											 "  id=region1\n" +
											 "  RegionName\n" +
											 "    id=region2\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"public static region1 extends region2").region().getTree();
						RegionDeclarationNode ldn = (RegionDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "NewRegionDeclaration\n" +
											 "  modifiers=" + (JavaNode.PUBLIC | JavaNode.STATIC)+"\n" +
											 "  id=region1\n" +
											 "  QualifiedRegionName\n" +
											 "    NamedType\n" +
											 "      type=Class2\n" +
											 "    id=region2\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"public static region1 extends Class2:region2").region()
										.getTree();
						RegionDeclarationNode ldn = (RegionDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "NewRegionDeclaration\n" +
											 "  modifiers=" + (JavaNode.PUBLIC | JavaNode.STATIC)+"\n" +
											 "  id=region1\n" +
											 "  QualifiedRegionName\n" +
											 "    NamedType\n" +
											 "      type=inner.inner2\n" +
											 "    id=region2\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"public static region1 extends inner.inner2:region2")
										.region().getTree();
						RegionDeclarationNode ldn = (RegionDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "NewRegionDeclaration\n" +
											 "  modifiers=" + (JavaNode.PUBLIC | JavaNode.STATIC)+"\n" +
											 "  id=region1\n" +
											 "  RegionName\n" +
											 "    id=[]\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"public static region1 extends []").region().getTree();
						RegionDeclarationNode ldn = (RegionDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
//						System.out.println(expected);
//						System.out.println(ldn.unparse(true));
						
						assertTrue(ldn.unparse(true).equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
		}

		public void testBadRegionAAST() {
				try {
						SLParse.initParser("").region().getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Should have thrown a recognition exception.");
				}
				/*
				 * try { SLParse.initParser("1region").region().getTree(); fail("Should
				 * have thrown an exception."); } catch (RecognitionException e) { }
				 * catch (Exception e) { e.printStackTrace(); fail("Should have thrown a
				 * recognition exception."); }
				 */
				try {
						SLParse.initParser("static public region1 extends").region()
										.getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Should have thrown a recognition exception.");
				}
				try {
						SLParse.initParser("private region1 extends this:[]").region()
										.getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Should have thrown a recognition exception.");
				}
				try {
						SLParse.initParser("region1 extends this.region").region()
										.getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Should have thrown a recognition exception.");
				}

				try {
						SLParse.initParser("public this.region1 extends this.region")
										.region().getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Should have thrown a recognition exception.");
				}
				try {
						SLParse.initParser("public region1 extends this:region").region()
										.getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Should have thrown a recognition exception.");
				}
				try {
						SLParse.initParser("final region1").region().getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Should have thrown a recognition exception.");
				}
		}

		public void testGoodRequiresLockAAST() {
				String expected;
				try {
						expected = "RequiresLockNode\n"+
											 "  SimpleLockName\n"+
											 "    id=lock1\n\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock1").requiresLock().getTree();
						RequiresLockNode ldn = (RequiresLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
//						System.out.println(expected + "<<<");
//						System.out.println(ldn.unparse(true) + "<<<");
						assertTrue(ldn.unparse(true).equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception.");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception.");
				}
				try {
						expected = "RequiresLockNode\n"+
											 "  SimpleLockName\n"+
											 "    id=lock1\n"+
											 "\n" +
											 "  SimpleLockName\n"+
											 "    id=lock2\n" +
											 "\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock1, lock2").requiresLock().getTree();
						RequiresLockNode ldn = (RequiresLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
//						System.out.println(expected + "<<<");
//						System.out.println(ldn.unparse(true) + "<<<");
						assertTrue(ldn.unparse(true).equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception.");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception.");
				}
				try {
						expected = "RequiresLockNode\n"+
											 "  SimpleLockName\n"+
											 "    id=lock1\n"+
											 "\n" +
											 "  SimpleLockName\n"+
											 "    id=lock2\n"+
											 "\n" +
											 "  SimpleLockName\n"+
											 "    id=lock3\n" +
											 "\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock1, lock2, lock3").requiresLock().getTree();
						RequiresLockNode ldn = (RequiresLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
//						System.out.println(expected + "<<<");
//						System.out.println(ldn.unparse(true) + "<<<");
						assertTrue(ldn.unparse(true).equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception.");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception.");
				}

				try {
						expected = "RequiresLockNode\n"+
											 "  SimpleLockName\n"+
											 "    id=lock1\n"+
											 "\n" +
											 "  SimpleLockName\n"+
											 "    id=lock2\n"+
											 "\n" +
											 "  QualifiedLockName\n"+
											 "    VariableUseExpression\n" +
											 "      id=Type\n"+
											 "    id=lock3\n"+
											 "\n" ;
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock1, lock2, Type:lock3").requiresLock().getTree();
						RequiresLockNode ldn = (RequiresLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
//						System.out.println(expected + "<<<");
//						System.out.println(ldn.unparse(true) + "<<<");
						assertTrue(ldn.unparse(true).equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception.");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception.");
				}
				try {
						expected = "RequiresLockNode\n"+
											 "  QualifiedLockName\n"+
											 "    QualifiedThisExpression\n" +
											 "      NamedType\n"+
											 "        type=Type\n"+
											 "    id=lock1\n"+
											 "\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"Type.this:lock1").requiresLock().getTree();
						RequiresLockNode ldn = (RequiresLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
//						System.out.println(expected + "<<<");
//						System.out.println(ldn.unparse(true) + "<<<");
						assertTrue(ldn.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception.");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception.");
				}
		}

		public void testBadRequiresLockAAST() {
				try {
						SLParse.initParser("").requiresLock().getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Should have thrown a recognition exception.");
				}
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"this.lock1").requiresLock().getTree();
						RequiresLockNode ldn = (RequiresLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception.");
				}

				try {
						SLParse.initParser("lock1,").requiresLock().getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Should have thrown a recognition exception.");
				}

				try {
						SLParse.initParser(":lock1").requiresLock().getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Should have thrown a recognition exception.");
				}

				try {
						SLParse.initParser("Type:this.lock1").requiresLock().getTree();
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Should have thrown a recognition exception.");
				}
		}

		public void testGoodReturnsLockAAST() {
				String expected;
				try {
						expected = "ReturnsLockNode\n"+
											 "    SimpleLockName\n"+
											 "    id=lock1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock1").returnsLock().getTree();
						ReturnsLockNode ldn = (ReturnsLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "ReturnsLockNode\n"+
											 "    QualifiedLockName\n"+
											 "    QualifiedThisExpression\n"+
											 "      NamedType\n"+
											 "        type=Type\n"+
											 "    id=lock1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"Type.this:lock1").returnsLock().getTree();
						ReturnsLockNode ldn = (ReturnsLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

		}

		public void testBadReturnsLockAAST() {
				try {
						SLParse.initParser("").returnsLock().getTree();
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"this.lock1").returnsLock().getTree();
						ReturnsLockNode ldn = (ReturnsLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				/*
				 * try { SLParse.initParser("1lock").returnsLock().getTree();
				 * fail("Should have thrown a RecognitionException"); } catch
				 * (RecognitionException e) { } catch (Exception e) {
				 * e.printStackTrace(); fail("Unexpected exception"); }
				 */
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock1, lock2").returnsLock().getTree();
						ReturnsLockNode ldn = (ReturnsLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown an exception.");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
		}

		public void testGoodMapIntoAAST() {
				String expected;
				try {
						expected = "MapIntoNode\n"+
											 "    RegionName\n"+
											 "    id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"region1").inRegion().getTree();
						InRegionNode ldn = (InRegionNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "InRegionNode\n"+
											 "    QualifiedRegionName\n"+
											 "    NamedType\n"+
											 "      type=Type\n"+
											 "    id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"Type:region1").inRegion().getTree();
						InRegionNode ldn = (InRegionNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "InRegionNode\n"+
											 "    QualifiedRegionName\n"+
											 "    NamedType\n"+
											 "      type=Type.type2\n"+
											 "    id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"Type.type2:region1").inRegion().getTree();
						InRegionNode ldn = (InRegionNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(ldn.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "InRegionNode\n"+
											 "    RegionName\n"+
											 "    id=[]\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser("[]")
										.inRegion().getTree();
						InRegionNode ldn = (InRegionNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
//						System.out.println(expected + "<<<");
//						System.out.println(ldn.unparse(true) + "<<<");
						assertTrue(ldn.unparse(true).equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
		}

		public void testBadMapIntoAAST() {
				try {
						SLParse.initParser("").inRegion().getTree();
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						SLParse.initParser(":region1").inRegion().getTree();
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				
				try {
						SLParse.initParser("region1, region2").inRegion().getTree();
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
		}

		/*
		 * public void testGoodMapFields(){ try{ AASTAdaptor.Node root =
		 * (AASTAdaptor.Node)SLParse.initParser("region1 into
		 * SuperRegion").mapFields().getTree(); MapFieldsNode node =
		 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype); }
		 * catch(RecognitionException e){ e.printStackTrace(); fail("Unexpected
		 * exception"); } catch(Exception e){ e.printStackTrace(); fail("Unexpected
		 * exception"); }
		 * 
		 * try{ AASTAdaptor.Node root =
		 * (AASTAdaptor.Node)SLParse.initParser("region1, region2 into
		 * SuperRegion").mapFields().getTree(); MapFieldsNode node =
		 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype); }
		 * catch(RecognitionException e){ e.printStackTrace(); fail("Unexpected
		 * exception"); } catch(Exception e){ e.printStackTrace(); fail("Unexpected
		 * exception"); }
		 * 
		 * try{ AASTAdaptor.Node root =
		 * (AASTAdaptor.Node)SLParse.initParser("region1, region2, region3 into
		 * SuperRegion").mapFields().getTree(); MapFieldsNode node =
		 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype); }
		 * catch(RecognitionException e){ e.printStackTrace(); fail("Unexpected
		 * exception"); } catch(Exception e){ e.printStackTrace(); fail("Unexpected
		 * exception"); }
		 * 
		 * try{ AASTAdaptor.Node root =
		 * (AASTAdaptor.Node)SLParse.initParser("region1 into
		 * []").mapFields().getTree(); MapFieldsNode node =
		 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype); }
		 * catch(RecognitionException e){ e.printStackTrace(); fail("Unexpected
		 * exception"); } catch(Exception e){ e.printStackTrace(); fail("Unexpected
		 * exception"); }
		 * 
		 * try{ AASTAdaptor.Node root =
		 * (AASTAdaptor.Node)SLParse.initParser("region1, region2, region3 into
		 * []").mapFields().getTree(); MapFieldsNode node =
		 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype); }
		 * catch(RecognitionException e){ e.printStackTrace(); fail("Unexpected
		 * exception"); } catch(Exception e){ e.printStackTrace(); fail("Unexpected
		 * exception"); }
		 * 
		 * try{ AASTAdaptor.Node root =
		 * (AASTAdaptor.Node)SLParse.initParser("region1 into
		 * Type:SuperRegion").mapFields().getTree(); MapFieldsNode node =
		 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype); }
		 * catch(RecognitionException e){ e.printStackTrace(); fail("Unexpected
		 * exception"); } catch(Exception e){ e.printStackTrace(); fail("Unexpected
		 * exception"); }
		 * 
		 * try{ AASTAdaptor.Node root =
		 * (AASTAdaptor.Node)SLParse.initParser("region1 into
		 * Type.this:SuperRegion").mapFields().getTree(); MapFieldsNode node =
		 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype); }
		 * catch(RecognitionException e){ e.printStackTrace(); fail("Unexpected
		 * exception"); } catch(Exception e){ e.printStackTrace(); fail("Unexpected
		 * exception"); }
		 * 
		 * try{ AASTAdaptor.Node root =
		 * (AASTAdaptor.Node)SLParse.initParser("region1 into
		 * Type.type2:SuperRegion").mapFields().getTree(); MapFieldsNode node =
		 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype); }
		 * catch(RecognitionException e){ e.printStackTrace(); fail("Unexpected
		 * exception"); } catch(Exception e){ e.printStackTrace(); fail("Unexpected
		 * exception"); } }
		 */

		/*
		 * public void testBadMapFields(){ try{ AASTAdaptor.Node root =
		 * (AASTAdaptor.Node)SLParse.initParser("").mapFields().getTree();
		 * MapFieldsNode node =
		 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
		 * fail("Should have thrown a RecognitionException"); }
		 * catch(RecognitionException e){ } catch(Exception e){ e.printStackTrace();
		 * fail("Unexpected exception"); }
		 * 
		 * try{ AASTAdaptor.Node root =
		 * (AASTAdaptor.Node)SLParse.initParser("region1, region2 into
		 * ").mapFields().getTree(); MapFieldsNode node =
		 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
		 * fail("Should have thrown a RecognitionException"); }
		 * catch(RecognitionException e){ } catch(Exception e){ e.printStackTrace();
		 * fail("Unexpected exception"); }
		 * 
		 * try{ AASTAdaptor.Node root =
		 * (AASTAdaptor.Node)SLParse.initParser("region1, region2, region3 into
		 * this.SuperRegion").mapFields().getTree(); MapFieldsNode node =
		 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
		 * fail("Should have thrown a RecognitionException"); }
		 * catch(RecognitionException e){ } catch(Exception e){ e.printStackTrace();
		 * fail("Unexpected exception"); }
		 * 
		 * try{ AASTAdaptor.Node root =
		 * (AASTAdaptor.Node)SLParse.initParser("region1 into
		 * Array[]").mapFields().getTree(); MapFieldsNode node =
		 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
		 * fail("Should have thrown a RecognitionException"); }
		 * catch(RecognitionException e){ } catch(Exception e){ e.printStackTrace();
		 * fail("Unexpected exception"); }
		 * 
		 * try{ AASTAdaptor.Node root =
		 * (AASTAdaptor.Node)SLParse.initParser("region1, region2, region3 into
		 * Type:[]").mapFields().getTree(); MapFieldsNode node =
		 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
		 * fail("Should have thrown a RecognitionException"); }
		 * catch(RecognitionException e){ } catch(Exception e){ e.printStackTrace();
		 * fail("Unexpected exception"); }
		 * 
		 * try{ AASTAdaptor.Node root =
		 * (AASTAdaptor.Node)SLParse.initParser("region1").mapFields().getTree();
		 * MapFieldsNode node =
		 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
		 * fail("Should have thrown a RecognitionException"); }
		 * catch(RecognitionException e){ } catch(Exception e){ e.printStackTrace();
		 * fail("Unexpected exception"); }
		 * 
		 * try{ AASTAdaptor.Node root =
		 * (AASTAdaptor.Node)SLParse.initParser("region1, region2,
		 * region3").mapFields().getTree(); MapFieldsNode node =
		 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
		 * fail("Should have thrown a RecognitionException"); }
		 * catch(RecognitionException e){ } catch(Exception e){ e.printStackTrace();
		 * fail("Unexpected exception"); } }
		 */

		public void testGoodAggregate() {
				String expected;
				try {
						expected = "AggregateNode\n"+
											 "    MappedRegionSpecification\n"+
											 "    RegionMapping\n" +
											 "      RegionName\n"+
											 "        id=region1\n" +
											 "      RegionName\n"+
											 "        id=SuperRegion1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"region1 into SuperRegion1").aggregate().getTree();
						AggregateNode node = (AggregateNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						expected = "AggregateNode\n"+
											 "    MappedRegionSpecification\n"+
											 "    RegionMapping\n" +
											 "      RegionName\n"+
											 "        id=region1\n" +
											 "      RegionName\n"+
											 "        id=SuperRegion1\n" +
											 "    RegionMapping\n" +
											 "      RegionName\n"+
											 "        id=region2\n" +
											 "      RegionName\n"+
											 "        id=SuperRegion1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"region1 into SuperRegion1, region2 into SuperRegion1")
										.aggregate().getTree();
						AggregateNode node = (AggregateNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						expected = "AggregateNode\n"+
											 "    MappedRegionSpecification\n"+
											 "    RegionMapping\n" +
											 "      RegionName\n"+
											 "        id=region1\n" +
											 "      RegionName\n"+
											 "        id=SuperRegion1\n" +
											 "    RegionMapping\n" +
											 "      RegionName\n"+
											 "        id=region2\n" +
											 "      RegionName\n"+
											 "        id=SuperRegion1\n" +
											 "    RegionMapping\n" +
											 "      RegionName\n"+
											 "        id=region3\n" +
											 "      RegionName\n"+
											 "        id=SuperRegion1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse
										.initParser(
														"region1 into SuperRegion1, region2 into SuperRegion1, region3 into SuperRegion1")
										.aggregate().getTree();
						AggregateNode node = (AggregateNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						expected = "AggregateNode\n"+
											 "    MappedRegionSpecification\n"+
											 "    RegionMapping\n" +
											 "      QualifiedRegionName\n"+
											 "        NamedType\n"	+
											 "          type=Type\n" +
											 "        id=region1\n" +
											 "      RegionName\n"+
											 "        id=SuperRegion1\n" +
											 "    RegionMapping\n" +
											 "      QualifiedRegionName\n"+
											 "        NamedType\n"	+
											 "          type=Type\n" +
											 "        id=region2\n" +
											 "      RegionName\n"+
											 "        id=SuperRegion1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse
										.initParser(
														"Type:region1 into SuperRegion1, Type:region2 into SuperRegion1")
										.aggregate().getTree();
						AggregateNode node = (AggregateNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						expected = "AggregateNode\n"+
											 "    MappedRegionSpecification\n"+
											 "    RegionMapping\n" +
											 "      RegionName\n"+
											 "        id=region1\n" +
											 "      QualifiedRegionName\n"+
											 "        NamedType\n"	+
											 "          type=Type\n" +
											 "        id=SuperRegion1\n" +
											 "    RegionMapping\n" +
											 "      RegionName\n"+
											 "        id=region2\n" +
											 "      QualifiedRegionName\n"+
											 "        NamedType\n"	+
											 "          type=Type\n" +
											 "        id=SuperRegion1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse
										.initParser(
														"region1 into Type:SuperRegion1, region2 into Type:SuperRegion1")
										.aggregate().getTree();
						AggregateNode node = (AggregateNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						expected = "AggregateNode\n"+
											 "    MappedRegionSpecification\n"+
											 "    RegionMapping\n" +
											 "      QualifiedRegionName\n"+
											 "        NamedType\n"	+
											 "          type=Type.Region\n" +
											 "        id=region1\n" +
											 "      RegionName\n"+
											 "        id=SuperRegion1\n" +
											 "    RegionMapping\n" +
											 "      QualifiedRegionName\n"+
											 "        NamedType\n"	+
											 "          type=Type.Region\n" +
											 "        id=region2\n" +
											 "      RegionName\n"+
											 "        id=SuperRegion1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse
										.initParser(
														"Type.Region:region1 into SuperRegion1, Type.Region:region2 into SuperRegion1")
										.aggregate().getTree();
						AggregateNode node = (AggregateNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						expected = "AggregateNode\n"+
											 "    MappedRegionSpecification\n"+
											 "    RegionMapping\n" +
											 "      RegionName\n"+
											 "        id=region1\n" +
											 "      QualifiedRegionName\n"+
											 "        NamedType\n"	+
											 "          type=Type.Region\n" +
											 "        id=SuperRegion1\n" +
											 "    RegionMapping\n" +
											 "      RegionName\n"+
											 "        id=region2\n" +
											 "      QualifiedRegionName\n"+
											 "        NamedType\n"	+
											 "          type=Type.Region\n" +
											 "        id=SuperRegion1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse
										.initParser(
														"region1 into Type.Region:SuperRegion1, region2 into Type.Region:SuperRegion1")
										.aggregate().getTree();
						AggregateNode node = (AggregateNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						expected = "AggregateNode\n"+
											 "    MappedRegionSpecification\n"+
											 "    RegionMapping\n" +
											 "      RegionName\n"+
											 "        id=[]\n" +
											 "      RegionName\n"+
											 "        id=SuperRegion1\n" +
											 "    RegionMapping\n" +
											 "      RegionName\n"+
											 "        id=[]\n" +
											 "      RegionName\n"+
											 "        id=SuperRegion1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"[] into SuperRegion1, [] into SuperRegion1").aggregate()
										.getTree();
						AggregateNode node = (AggregateNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
//						System.out.println(expected + "<<<");
//						System.out.println(node.unparse(true) + "<<<");
						assertTrue(node.unparse(true).equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						expected = "AggregateNode\n"+
											 "    MappedRegionSpecification\n"+
											 "    RegionMapping\n" +
											 "      RegionName\n"+
											 "        id=region1\n" +
											 "      RegionName\n"+
											 "        id=[]\n" +
											 "    RegionMapping\n" +
											 "      RegionName\n"+
											 "        id=region2\n" +
											 "      RegionName\n"+
											 "        id=[]\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"region1 into [], region2 into []")
										.aggregate().getTree();
						AggregateNode node = (AggregateNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse(true).equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						expected = "AggregateNode\n"+
											 "    MappedRegionSpecification\n"+
											 "    RegionMapping\n" +
											 "      RegionName\n"+
											 "        id=[]\n" +
											 "      RegionName\n"+
											 "        id=[]\n" +
											 "    RegionMapping\n" +
											 "      RegionName\n"+
											 "        id=[]\n" +
											 "      RegionName\n"+
											 "        id=[]\n";
					AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"[] into [], [] into []").aggregate().getTree();
						AggregateNode node = (AggregateNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));

				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
		}

		public void testBadAggregate() {
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser("")
										.aggregate().getTree();
						AggregateNode node = (AggregateNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"region1 into SuperRegion1, SuperRegion1").aggregate()
										.getTree();
						AggregateNode node = (AggregateNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"region1, region2 into SuperRegion1").aggregate().getTree();
						AggregateNode node = (AggregateNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"Type.region1 into SuperRegion1 ").aggregate().getTree();
						AggregateNode node = (AggregateNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"region1 into Type:Type:SuperRegion1").aggregate()
										.getTree();
						AggregateNode node = (AggregateNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"Type.Region.region1 into this.SuperRegion1").aggregate()
										.getTree();
						AggregateNode node = (AggregateNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"region1 into SuperRegion1 region2 into SuperRegion1")
										.aggregate().getTree();
						AggregateNode node = (AggregateNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
		}

		public void testGoodPolicyLock() {
				String expected;
				try {
						expected = "PolicyLockDeclaration\n"+
											 "  id=lock\n"+
											 "  ThisExpression\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock is this").policyLock().getTree();
						PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						expected = "PolicyLockDeclaration\n"+
											 "  id=lock\n"+
											 "  ImplicitClassLockExpression\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock is class").policyLock().getTree();
						PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						expected = "PolicyLockDeclaration\n"+
											 "  id=lock\n"+
											 "  ImplicitClassLockExpression\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock is this.class").policyLock().getTree();
						PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						expected = "PolicyLockDeclaration\n"+
											 "  id=lock\n"+
											 "  FieldRef\n"+
											 "    ThisExpression\n" +
											 "    id=Lock\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock is Lock").policyLock().getTree();
						PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						expected = "PolicyLockDeclaration\n"+
											 "  id=lock\n"+
											 "  FieldRef\n"+
											 "    TypeExpression\n" +
											 "      NamedType\n"+
											 "        type=Type\n"+
											 "    id=Lock\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock is Type.Lock").policyLock().getTree();
						PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						expected = "PolicyLockDeclaration\n"+
											 "  id=lock\n"+
											 "  FieldRef\n"+
											 "    TypeExpression\n" +
											 "      NamedType\n"+
											 "        type=Type.lock\n"+
											 "    id=Lock\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock is Type.lock.Lock").policyLock().getTree();
						PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						expected = "PolicyLockDeclaration\n"+
											 "  id=lock\n"+
											 "  QualifiedThisExpression\n"+
											 "    NamedType\n"+
											 "      type=Type\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock is Type.this").policyLock().getTree();
						PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
		}

		public void testBadPolicyLock() {
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser("")
										.policyLock().getTree();
						PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock").policyLock().getTree();
						PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock is ").policyLock().getTree();
						PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										" is this").policyLock().getTree();
						PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}

				/*
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock is this.class").policyLock().getTree();
						PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
*/
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock is Type:this").policyLock().getTree();
						PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
		}

		public void testGoodReads() {
				String expected;
				try {
						expected = "Reads\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"nothing").reads().getTree();
						ReadsNode node = (ReadsNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "Reads\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    ThisExpression\n"+
											 "    RegionName\n"+
											 "      id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"region1").reads().getTree();
						ReadsNode node = (ReadsNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "Reads\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    AnyInstanceExpression\n"+
											 "      NamedType\n"+
											 "        type=Type\n"+
											 "    RegionName\n"+
											 "      id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"any(Type):region1").reads().getTree();
						ReadsNode node = (ReadsNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "Reads\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    AnyInstanceExpression\n"+
											 "      NamedType\n"+
											 "        type=Type.type\n"+
											 "    RegionName\n"+
											 "      id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"any(Type.type):region1").reads().getTree();
						ReadsNode node = (ReadsNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "Reads\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    SuperExpression\n"+
											 "    RegionName\n"+
											 "      id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"super:region1").reads().getTree();
						ReadsNode node = (ReadsNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "Reads\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    ThisExpression\n"+
											 "    RegionName\n"+
											 "      id=region1\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    ThisExpression\n"+
											 "    RegionName\n"+
											 "      id=region2\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"region1, region2").reads().getTree();
						ReadsNode node = (ReadsNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "Reads\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    QualifiedThisExpression\n"+
											 "      NamedType\n"+
											 "        type=Class\n"+
											 "    RegionName\n"+
											 "      id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"Class.this:region1").reads().getTree();
						ReadsNode node = (ReadsNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "Reads\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    QualifiedThisExpression\n"+
											 "      NamedType\n"+
											 "        type=Class\n"+
											 "    RegionName\n"+
											 "      id=[]\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"Class.this:[]").reads().getTree();
						ReadsNode node = (ReadsNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "Reads\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    ThisExpression\n"+
											 "    RegionName\n"+
											 "      id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"this:region1").reads().getTree();
						ReadsNode node = (ReadsNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
		}

		public void testBadReads() {

				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser("")
										.reads().getTree();
						ReadsNode node = (ReadsNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"any(Type).region").reads().getTree();
						ReadsNode node = (ReadsNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"region1, nothing").reads().getTree();
						ReadsNode node = (ReadsNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"Type.this.region.[]").reads().getTree();
						ReadsNode node = (ReadsNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
		}

		public void testGoodWrites() {
				String expected;
				try {
						expected = "Writes\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"nothing").writes().getTree();
						WritesNode node = (WritesNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "Writes\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    ThisExpression\n"+
											 "    RegionName\n"+
											 "      id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"region1").writes().getTree();
						WritesNode node = (WritesNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "Writes\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    AnyInstanceExpression\n"+
											 "      NamedType\n"+
											 "        type=Type\n"+
											 "    RegionName\n"+
											 "      id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"any(Type):region1").writes().getTree();
						WritesNode node = (WritesNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "Writes\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    AnyInstanceExpression\n"+
											 "      NamedType\n"+
											 "        type=Type.type\n"+
											 "    RegionName\n"+
											 "      id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"any(Type.type):region1").writes().getTree();
						WritesNode node = (WritesNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "Writes\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    SuperExpression\n"+
											 "    RegionName\n"+
											 "      id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"super:region1").writes().getTree();
						WritesNode node = (WritesNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "Writes\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    ThisExpression\n"+
											 "    RegionName\n"+
											 "      id=region1\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    ThisExpression\n"+
											 "    RegionName\n"+
											 "      id=region2\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"region1, region2").writes().getTree();
						WritesNode node = (WritesNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "Writes\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    QualifiedThisExpression\n"+
											 "      NamedType\n"+
											 "        type=Class\n"+
											 "    RegionName\n"+
											 "      id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"Class.this:region1").writes().getTree();
						WritesNode node = (WritesNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "Writes\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    QualifiedThisExpression\n"+
											 "      NamedType\n"+
											 "        type=Class\n"+
											 "    RegionName\n"+
											 "      id=[]\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"Class.this:[]").writes().getTree();
						WritesNode node = (WritesNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "Writes\n"+
											 "  EffectSpecification\n" +
											 "    isWrite=false\n"+
											 "    ThisExpression\n"+
											 "    RegionName\n"+
											 "      id=region1\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"this:region1").writes().getTree();
						WritesNode node = (WritesNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
		}

		public void testBadWrites() {
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser("")
										.writes().getTree();
						WritesNode node = (WritesNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"any(Type).region").writes().getTree();
						WritesNode node = (WritesNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"region1, nothing").writes().getTree();
						WritesNode node = (WritesNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"Type.this.region.[]").writes().getTree();
						WritesNode node = (WritesNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
		}

		public void testGoodIsLock() {
				String expected;

				try {
						expected = "IsLockNode\n"+
											 "    SimpleLockName\n"+
											 "    id=L\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser("L")
										.isLock().getTree();
						IsLockNode node = (IsLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "IsLockNode\n"+
											 "    SimpleLockName\n"+
											 "    id=lock\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock").isLock().getTree();
						IsLockNode node = (IsLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						expected = "IsLockNode\n"+
											 "    SimpleLockName\n"+
											 "    id=myLock\n";
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"myLock").isLock().getTree();
						IsLockNode node = (IsLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						assertTrue(node.unparse().equals(expected));
				} catch (RecognitionException e) {
						e.printStackTrace();
						fail("Unexpected exception");
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
		}

		public void testBadIsLock() {

				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser("")
										.isLock().getTree();
						IsLockNode node = (IsLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"lock1, lock2").isLock().getTree();
						IsLockNode node = (IsLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"this.lock").isLock().getTree();
						IsLockNode node = (IsLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"this.class").isLock().getTree();
						IsLockNode node = (IsLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
				try {
						AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.initParser(
										"Type:lock").isLock().getTree();
						IsLockNode node = (IsLockNode) root
										.finalizeAST(IAnnotationParsingContext.nullPrototype);
						fail("Should have thrown a RecognitionException");
				} catch (RecognitionException e) {
				} catch (Exception e) {
						e.printStackTrace();
						fail("Unexpected exception");
				}
		}

		public void testGoodLocksRulePlacement() {
				LockRulesTestHelper lrth = LockRulesTestHelper.getInstance();

				Lock_ParseRuleHelper lockRulesHelper = new Lock_ParseRuleHelper();

				RequiresLock_ParseRuleHelper requiresLockRulesHelper = new RequiresLock_ParseRuleHelper();

				ReturnsLock_ParseRuleHelper returnsLockRulesHelper = new ReturnsLock_ParseRuleHelper();

				PolicyLock_ParseRuleHelper policyLockRulesHelper = new PolicyLock_ParseRuleHelper();

				IsLock_ParseRuleHelper isLockRulesHelper = new IsLock_ParseRuleHelper();

				SelfProtected_ParseRuleHelper selfProtectedRulesHelper = new SelfProtected_ParseRuleHelper();

				SingleThreaded_ParseRuleHelper singleThreadedRulesHelper = new SingleThreaded_ParseRuleHelper();

				TestContext context = new TestContext(null, ClassDeclaration.prototype);

				/* ****************** Test @Lock ******************************* */
				lockRulesHelper.parse(context, "L1 is this protects Instance");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				context.setOp(InterfaceDeclaration.prototype);
				lockRulesHelper.parse(context, "L1 is this protects Instance");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				context.setOp(EnumDeclaration.prototype);
				lockRulesHelper.parse(context, "L1 is this protects Instance");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				/* ****************** Test @RequiresLock ******************************* */

				context.setOp(MethodDeclaration.prototype);
				requiresLockRulesHelper.parse(context, "L1");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				/* ****************** Test @ReturnsLock ******************************* */
				context.setOp(MethodDeclaration.prototype);
				returnsLockRulesHelper.parse(context, "L1");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				/* ****************** Test @PolicyLock ******************************* */
				context.setOp(ClassDeclaration.prototype);
				policyLockRulesHelper.parse(context, "L1 is this");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				context.setOp(InterfaceDeclaration.prototype);
				policyLockRulesHelper.parse(context, "L1 is this");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				context.setOp(EnumDeclaration.prototype);
				policyLockRulesHelper.parse(context, "L1 is this");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				/* ****************** Test @IsLock ******************************* */
				context.setOp(MethodDeclaration.prototype);
				isLockRulesHelper.parse(context, "L1");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				/*
				 * ****************** Test @SingleThreaded
				 * *******************************
				 */
				context.setOp(ConstructorDeclaration.prototype);
				singleThreadedRulesHelper.parse(context, "");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				/*
				 * ****************** Test @SelfProtected
				 * *******************************
				 */
				context.setOp(ClassDeclaration.prototype);
				selfProtectedRulesHelper.parse(context, "");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				context.setOp(InterfaceDeclaration.prototype);
				selfProtectedRulesHelper.parse(context, "");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				context.setOp(EnumDeclaration.prototype);
				selfProtectedRulesHelper.parse(context, "");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);
		}

		public void testBadLocksRulePlacement() {
				LockRulesTestHelper lrth = LockRulesTestHelper.getInstance();

				Lock_ParseRuleHelper lockRulesHelper = new Lock_ParseRuleHelper();

				RequiresLock_ParseRuleHelper requiresLockRulesHelper = new RequiresLock_ParseRuleHelper();

				ReturnsLock_ParseRuleHelper returnsLockRulesHelper = new ReturnsLock_ParseRuleHelper();

				PolicyLock_ParseRuleHelper policyLockRulesHelper = new PolicyLock_ParseRuleHelper();

				IsLock_ParseRuleHelper isLockRulesHelper = new IsLock_ParseRuleHelper();

				SelfProtected_ParseRuleHelper selfProtectedRulesHelper = new SelfProtected_ParseRuleHelper();

				SingleThreaded_ParseRuleHelper singleThreadedRulesHelper = new SingleThreaded_ParseRuleHelper();

				TestContext context = new TestContext(null,
								PackageDeclaration.prototype);

				/* ****************** Test @Lock ******************************* */
				lockRulesHelper.parse(context, "L1 is this protects Instance");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(MethodDeclaration.prototype);
				lockRulesHelper.parse(context, "L1 is this protects Instance");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(ParameterDeclaration.prototype);
				lockRulesHelper.parse(context, "L1 is this protects Instance");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				/* ****************** Test @RequiresLock ******************************* */

				context.setOp(PackageDeclaration.prototype);
				requiresLockRulesHelper.parse(context, "L1");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(ClassInitDeclaration.prototype);
				requiresLockRulesHelper.parse(context, "L1");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(ParameterDeclaration.prototype);
				requiresLockRulesHelper.parse(context, "L1");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(InterfaceDeclaration.prototype);
				requiresLockRulesHelper.parse(context, "L1");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				/* ****************** Test @ReturnsLock ******************************* */
				context.setOp(ClassDeclaration.prototype);
				returnsLockRulesHelper.parse(context, "L1");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(InterfaceDeclaration.prototype);
				returnsLockRulesHelper.parse(context, "L1");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(ParameterDeclaration.prototype);
				returnsLockRulesHelper.parse(context, "L1");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(FieldDeclaration.prototype);
				returnsLockRulesHelper.parse(context, "L1");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				/* ****************** Test @PolicyLock ******************************* */
				context.setOp(ParameterDeclaration.prototype);
				policyLockRulesHelper.parse(context, "L1 is this");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(FieldDeclaration.prototype);
				policyLockRulesHelper.parse(context, "L1 is this");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(ClassInitDeclaration.prototype);
				policyLockRulesHelper.parse(context, "L1 is this");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(PackageDeclaration.prototype);
				policyLockRulesHelper.parse(context, "L1 is this");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(ConstructorDeclaration.prototype);
				policyLockRulesHelper.parse(context, "L1 is this");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				/* ****************** Test @IsLock ******************************* */
				context.setOp(ClassDeclaration.prototype);
				isLockRulesHelper.parse(context, "L1");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(FieldDeclaration.prototype);
				isLockRulesHelper.parse(context, "L1");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(InterfaceDeclaration.prototype);
				isLockRulesHelper.parse(context, "L1");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(PackageDeclaration.prototype);
				isLockRulesHelper.parse(context, "L1");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(ParameterDeclaration.prototype);
				isLockRulesHelper.parse(context, "L1");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				/*
				 * ****************** Test @SingleThreaded
				 * *******************************
				 */
				context.setOp(MethodDeclaration.prototype);
				singleThreadedRulesHelper.parse(context, "");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(ClassDeclaration.prototype);
				singleThreadedRulesHelper.parse(context, "");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(ParameterDeclaration.prototype);
				singleThreadedRulesHelper.parse(context, "");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(FieldDeclaration.prototype);
				singleThreadedRulesHelper.parse(context, "");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(PackageDeclaration.prototype);
				singleThreadedRulesHelper.parse(context, "");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				/*
				 * ****************** Test @SelfProtected
				 * *******************************
				 */
				context.setOp(MethodDeclaration.prototype);
				selfProtectedRulesHelper.parse(context, "");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(PackageDeclaration.prototype);
				selfProtectedRulesHelper.parse(context, "");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(ParameterDeclaration.prototype);
				selfProtectedRulesHelper.parse(context, "");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(FieldDeclaration.prototype);
				selfProtectedRulesHelper.parse(context, "");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

				context.setOp(ConstructorDeclaration.prototype);
				selfProtectedRulesHelper.parse(context, "");
				assertTrue(context.getError() != null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);
		}

		public void testGoodRegionsRulesPlacement() {
				RegionRulesTestHelper rrth = RegionRulesTestHelper.getInstance();
				Aggregate_ParseRuleHelper aggregateHelper = new Aggregate_ParseRuleHelper();
				Region_ParseRuleHelper regionHelper = new Region_ParseRuleHelper();
				InRegion_ParseRuleHelper mapIntoHelper = new InRegion_ParseRuleHelper();
				MapFields_ParseRuleHelper mapFieldsHelper = new MapFields_ParseRuleHelper();

				TestContext context = new TestContext(null, ClassDeclaration.prototype);

				/* ********** Test @Region ****************** */
				regionHelper.parse(context, "region1");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				context.setOp(InterfaceDeclaration.prototype);
				regionHelper.parse(context, "region1");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				context.setOp(EnumDeclaration.prototype);
				regionHelper.parse(context, "region1");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				/* ********** Test @Aggregate ****************** */
				context.setOp(FieldDeclaration.prototype);
				aggregateHelper.parse(context, "region1 into region2");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				/* ********** Test @MapInto ****************** */
				context.setOp(FieldDeclaration.prototype);
				mapIntoHelper.parse(context, "region1");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				/***********************************************************************
				 * ********** Test
				 * 
				 * @MapFields context.setOp(ClassDeclaration.prototype);
				 *            mapFieldsHelper.parse(context, "region1 into region2");
				 *            assertTrue(context.getError() == null);
				 *            context.getException().printStackTrace();
				 *            assertTrue(context.getException() == null);
				 *            assertTrue(context.getOffset() == 0);
				 * 
				 * context.setOp(InterfaceDeclaration.prototype);
				 * mapFieldsHelper.parse(context, "region1 into region2");
				 * assertTrue(context.getError() == null);
				 * assertTrue(context.getException() == null);
				 * assertTrue(context.getOffset() == 0);
				 * 
				 * context.setOp(EnumDeclaration.prototype);
				 * mapFieldsHelper.parse(context, "region1 into region2");
				 * assertTrue(context.getError() == null);
				 * assertTrue(context.getException() == null);
				 * assertTrue(context.getOffset() == 0);
				 */
		}

		public void testBadRegionsRulesPlacement() {
				RegionRulesTestHelper rrth = RegionRulesTestHelper.getInstance();
				Aggregate_ParseRuleHelper aggregateHelper = new Aggregate_ParseRuleHelper();
				Region_ParseRuleHelper regionHelper = new Region_ParseRuleHelper();
				InRegion_ParseRuleHelper mapIntoHelper = new InRegion_ParseRuleHelper();
				MapFields_ParseRuleHelper mapFieldsHelper = new MapFields_ParseRuleHelper();

				TestContext context = new TestContext(null,
								PackageDeclaration.prototype);

				/* ********** Test @Region ****************** */
				regionHelper.parse(context, "region1");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(ParameterDeclaration.prototype);
				regionHelper.parse(context, "region1");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(MethodDeclaration.prototype);
				regionHelper.parse(context, "region1");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(FieldDeclaration.prototype);
				regionHelper.parse(context, "region1");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(ClassInitDeclaration.prototype);
				regionHelper.parse(context, "region1");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				/* ********** Test @Aggregate ****************** */
				context.setOp(ClassDeclaration.prototype);
				aggregateHelper.parse(context, "region1 into region2");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(MethodDeclaration.prototype);
				aggregateHelper.parse(context, "region1 into region2");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(EnumDeclaration.prototype);
				aggregateHelper.parse(context, "region1 into region2");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(ConstructorDeclaration.prototype);
				aggregateHelper.parse(context, "region1 into region2");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(ClassInitDeclaration.prototype);
				aggregateHelper.parse(context, "region1 into region2");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				/* ********** Test @MapInto ****************** */
				context.setOp(PackageDeclaration.prototype);
				mapIntoHelper.parse(context, "region1");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(ClassDeclaration.prototype);
				mapIntoHelper.parse(context, "region1");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(EnumDeclaration.prototype);
				mapIntoHelper.parse(context, "region1");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(ParameterDeclaration.prototype);
				mapIntoHelper.parse(context, "region1");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(ClassInitDeclaration.prototype);
				mapIntoHelper.parse(context, "region1");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(ConstructorDeclaration.prototype);
				mapIntoHelper.parse(context, "region1");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

		}

		public void testGoodMethodEffectsRulesPlacement() {
				MethodEffectsRulesHelper merth = MethodEffectsRulesHelper.getInstance();
				Reads_ParseRuleHelper readsHelper = new Reads_ParseRuleHelper();
				Writes_ParseRuleHelper writesHelper = new Writes_ParseRuleHelper();

				TestContext context = new TestContext(null, MethodDeclaration.prototype);

				/* ****************** Test @Reads ****************** */

				readsHelper.parse(context, "nothing");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);

				/* ****************** Test @Writes ****************** */

				writesHelper.parse(context, "nothing");
				assertTrue(context.getError() == null);
				assertTrue(context.getException() == null);
				assertTrue(context.getOffset() == 0);
		}

		public void testBadMethodEffectsRulesPlacement() {
				MethodEffectsRulesHelper merth = MethodEffectsRulesHelper.getInstance();
				Reads_ParseRuleHelper readsHelper = new Reads_ParseRuleHelper();
				Writes_ParseRuleHelper writesHelper = new Writes_ParseRuleHelper();

				TestContext context = new TestContext(null,
								PackageDeclaration.prototype);

				/* ****************** Test @Reads ****************** */

				readsHelper.parse(context, "nothing");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(ConstructorDeclaration.prototype);
				readsHelper.parse(context, "nothing");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(ClassDeclaration.prototype);
				readsHelper.parse(context, "nothing");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(FieldDeclaration.prototype);
				readsHelper.parse(context, "nothing");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(ParameterDeclaration.prototype);
				readsHelper.parse(context, "nothing");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(ClassInitDeclaration.prototype);
				readsHelper.parse(context, "nothing");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				/* ****************** Test @Writes ****************** */

				context.setOp(ConstructorDeclaration.prototype);
				writesHelper.parse(context, "nothing");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(PackageDeclaration.prototype);
				writesHelper.parse(context, "nothing");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(InterfaceDeclaration.prototype);
				writesHelper.parse(context, "nothing");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(EnumDeclaration.prototype);
				writesHelper.parse(context, "nothing");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(ClassDeclaration.prototype);
				writesHelper.parse(context, "nothing");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

				context.setOp(ParameterDeclaration.prototype);
				writesHelper.parse(context, "nothing");
				assertFalse(context.getError() == null);
				assertTrue(context.getException() == null);
				assertFalse(context.getOffset() == 0);

		}

		static class TestContext extends AbstractAnnotationParsingContext {
				/**
				 * Used for testing
				 */
				Operator op;
				String errorMsg = null;
				int errorOffset = 0;
				Exception exception = null;

				/**
				 * @param src
				 */
				protected TestContext(AnnotationSource src, Operator op) {
						super(src);
						this.op = op;
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see com.surelogic.annotation.IAnnotationParsingContext#getOp()
				 */
				public Operator getOp() {
						return op;
				}

				/**
				 * Used for testing
				 * 
				 * @param op
				 */
				public void setOp(Operator op) {
						this.op = op;
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see com.surelogic.annotation.IAnnotationParsingContext#reportAAST(int,
				 *      com.surelogic.annotation.AnnotationLocation, java.lang.Object,
				 *      com.surelogic.aast.IAASTRootNode)
				 */
				public <T extends IAASTRootNode> void reportAAST(int offset,
								AnnotationLocation loc, Object o, T ast) {

				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see com.surelogic.annotation.IAnnotationParsingContext#reportError(int,
				 *      java.lang.String)
				 */
				public void reportError(int offset, String msg) {
						errorOffset = offset;
						errorMsg = msg;
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see com.surelogic.annotation.IAnnotationParsingContext#reportException(int,
				 *      java.lang.Exception)
				 */
				public void reportException(int offset, Exception e) {
						exception = e;
						errorOffset = offset;
				}

				public String getError() {
						return errorMsg;
				}

				public int getOffset() {
						return errorOffset;
				}

				public Exception getException() {
						return exception;
				}

        @Override
        protected IRNode getNode() {
          return null;
        }

		}

}
