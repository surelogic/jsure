package com.surelogic.jsure.tests;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.antlr.runtime.RecognitionException;

import com.surelogic.Part;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.*;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.AASTAdaptor;
import com.surelogic.annotation.parse.SLParse;
import com.surelogic.annotation.rules.*;
import com.surelogic.annotation.rules.LockRulesTestHelper.*;
import com.surelogic.annotation.rules.MethodEffectsRulesHelper.RegionEffects_ParseRuleHelper;
import com.surelogic.annotation.rules.RegionRulesTestHelper.*;
import com.surelogic.javac.Javac;
import com.surelogic.jsure.core.Eclipse;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.tree.Operator;

@SuppressWarnings("unused")
public class SLParseTest extends TestCase {

	public SLParseTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {		
		super.setUp();
		Eclipse.initialize();
		Javac.initialize();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * @param expected
	 *            null if expected to fail
	 * @return null if successful, otherwise a non-null failure message
	 */
	private String tryLockAAST(final String text, final String expected) {
		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser(text).lock().getTree();
			LockDeclarationNode ldn = (LockDeclarationNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			if (expected != null) {
				String unparse = ldn.unparse(true);
				return unparse.equals(expected) ? null : "Got: " + unparse
						+ "\nInstead of: " + expected;
			} else {
				return "Should have thrown an parse exception.";
			}
		} catch (RecognitionException e) {
			if (expected != null) {
				e.printStackTrace();
				return "Unexpected exception";
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return "Unexpected exception";
		}
	}

	public void testGoodLockAAST() {
		String expected = "LockDeclaration\n  id=L\n  ThisExpression\n  RegionName\n    id=Instance\n";
		String msg = tryLockAAST("L is this protects Instance", expected);
		if (msg != null) {
			fail(msg);
		}

		expected = "LockDeclaration\n  id=L1\n  FieldRef\n    ThisExpression\n    id=lock\n  RegionName\n    id=region\n";
		msg = tryLockAAST("L1 is lock protects region", expected);
		if (msg != null) {
			fail(msg);
		}

		expected = "LockDeclaration\n  id=L1\n  FieldRef\n    ThisExpression\n    id=lock\n  RegionName\n    id=region\n";
		msg = tryLockAAST("L1 is this.lock protects region", expected);
		if (msg != null) {
			fail(msg);
		}

		expected = "LockDeclaration\n  id=L1\n  QualifiedThisExpression\n    NamedType\n      type=Type\n  RegionName\n    id=region\n";
		msg = tryLockAAST("L1 is Type.this protects region", expected);
		if (msg != null) {
			fail(msg);
		}

		/*
		 * try { expected =
		 * "LockDeclaration\n  id=L1\n  FieldRef\n    ThisExpression\n    id=lock\n  QualifiedRegionName\n    NamedType\n      type=Class1\n    id=region\n"
		 * ; AASTAdaptor.Node root = (AASTAdaptor.Node)
		 * SLParse.prototype.initParser(
		 * "L1 is lock protects Class1:region").lock().getTree();
		 * LockDeclarationNode ldn = (LockDeclarationNode) root
		 * .finalizeAST(IAnnotationParsingContext.nullPrototype);
		 * assertTrue(ldn.unparse(true).equals(expected)); } catch
		 * (RecognitionException e) { e.printStackTrace();
		 * fail("Unexpected exception"); } catch (Exception e) {
		 * e.printStackTrace(); fail("Unexpected exception"); }
		 * 
		 * try { expected =
		 * "LockDeclaration\n  id=L1\n  FieldRef\n    TypeExpression\n      NamedType\n        type=Class1\n    id=lock\n  QualifiedRegionName\n    NamedType\n      type=Class1\n    id=region\n"
		 * ; AASTAdaptor.Node root = (AASTAdaptor.Node)
		 * SLParse.prototype.initParser(
		 * "L1 is Class1.lock protects Class1:region").lock() .getTree();
		 * LockDeclarationNode ldn = (LockDeclarationNode) root
		 * .finalizeAST(IAnnotationParsingContext.nullPrototype);
		 * assertTrue(ldn.unparse(true).equals(expected)); } catch
		 * (RecognitionException e) { e.printStackTrace();
		 * fail("Unexpected exception"); } catch (Exception e) {
		 * e.printStackTrace(); fail("Unexpected exception"); }
		 * 
		 * try { expected = "LockDeclaration\n" + "  id=L1\n"+ "  FieldRef\n" +
		 * "    TypeExpression\n" + "      NamedType\n"+
		 * "        type=outer.inner\n"+ "    id=lock\n"+
		 * "  QualifiedRegionName\n"+ "    NamedType\n"+
		 * "      type=Class1.region\n"+ "    id=region\n"; AASTAdaptor.Node
		 * root = (AASTAdaptor.Node) SLParse.prototype.initParser(
		 * "L1 is outer.inner.lock protects Class1.region:region")
		 * .lock().getTree(); LockDeclarationNode ldn = (LockDeclarationNode)
		 * root .finalizeAST(IAnnotationParsingContext.nullPrototype);
		 * System.out.println(expected); System.out.println(ldn.unparse(true));
		 * assertTrue(ldn.unparse(true).equals(expected)); } catch
		 * (RecognitionException e) { e.printStackTrace();
		 * fail("Unexpected exception"); } catch (Exception e) {
		 * e.printStackTrace(); fail("Unexpected exception"); }
		 * 
		 * try { expected =
		 * "LockDeclaration\n  id=L1\n  FieldRef\n    ThisExpression\n    id=lock\n  RegionName\n    id=[]\n"
		 * ; AASTAdaptor.Node root = (AASTAdaptor.Node)
		 * SLParse.prototype.initParser(
		 * "L1 is lock protects []").lock().getTree(); LockDeclarationNode ldn =
		 * (LockDeclarationNode) root
		 * .finalizeAST(IAnnotationParsingContext.nullPrototype); //
		 * System.out.println(expected); //
		 * System.out.println(ldn.unparse(true));
		 * assertTrue(ldn.unparse(true).equals(expected)); } catch
		 * (RecognitionException e) { e.printStackTrace();
		 * fail("Unexpected exception"); } catch (Exception e) {
		 * e.printStackTrace(); fail("Unexpected exception"); }
		 */
	}

	public void testBadLockAAST() {
		String msg = tryLockAAST("", null);
		if (msg != null) {
			fail(msg);
		}

		msg = tryLockAAST("L1 is class:lock protects region", null);
		if (msg != null) {
			fail(msg);
		}

		msg = tryLockAAST("L1 is Type.Type:class protects Class1:region", null);
		if (msg != null) {
			fail(msg);
		}

		msg = tryLockAAST("L1 is class.class protects region", null);
		if (msg != null) {
			fail(msg);
		}

		/*
		 * try { AASTAdaptor.Node root = (AASTAdaptor.Node)
		 * SLParse.prototype.initParser(
		 * "L1 is this.class protects region").lock().getTree();
		 * LockDeclarationNode ldn = (LockDeclarationNode) root
		 * .finalizeAST(IAnnotationParsingContext.nullPrototype);
		 * fail("Should have thrown an exception."); } catch
		 * (RecognitionException e) { } catch (Exception e) {
		 * e.printStackTrace(); fail("Unexpected exception"); }
		 */

		msg = tryLockAAST("is this protects Instance", null);
		if (msg != null) {
			fail(msg);
		}

		msg = tryLockAAST("L is protects Instance", null);
		if (msg != null) {
			fail(msg);
		}

		msg = tryLockAAST("L is this Instance", null);
		if (msg != null) {
			fail(msg);
		}

		msg = tryLockAAST("L this protects Instance", null);
		if (msg != null) {
			fail(msg);
		}

		msg = tryLockAAST("L is this protects", null);
		if (msg != null) {
			fail(msg);
		}

		msg = tryLockAAST("L is this protects", null);
		if (msg != null) {
			fail(msg);
		}
	}

	public void testGoodRegionAAST() {
		String expected;
		try {
			expected = "NewRegionDeclaration\n" + "  modifiers="
					+ (JavaNode.PUBLIC | JavaNode.ABSTRACT) + "\n" + "  id=region1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("public region1").region().getTree();
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
			expected = "NewRegionDeclaration\n" + "  modifiers="
					+ (JavaNode.PROTECTED | JavaNode.ABSTRACT) + "\n" + "  id=region1\n"
					+ "  RegionName\n" + "    id=region2\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("protected region1 extends region2").region()
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
			expected = "NewRegionDeclaration\n" + "  modifiers="
					+ (JavaNode.PRIVATE | JavaNode.STATIC | JavaNode.ABSTRACT) + "\n"
					+ "  id=region1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("private static region1").region().getTree();
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
			expected = "NewRegionDeclaration\n" + "  modifiers="
					+ (JavaNode.STATIC | JavaNode.ABSTRACT) + "\n" + "  id=region1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("static region1").region().getTree();
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
			expected = "NewRegionDeclaration\n" + "  modifiers="
					+ (JavaNode.PUBLIC | JavaNode.STATIC | JavaNode.ABSTRACT) + "\n"
					+ "  id=region1\n" + "  RegionName\n" + "    id=region2\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("public static region1 extends region2")
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
			expected = "NewRegionDeclaration\n" + "  modifiers="
					+ (JavaNode.PUBLIC | JavaNode.STATIC | JavaNode.ABSTRACT) + "\n"
					+ "  id=region1\n" + "  QualifiedRegionName\n"
					+ "    NamedType\n" + "      type=Class2\n"
					+ "    id=region2\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("public static region1 extends Class2:region2")
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
			expected = "NewRegionDeclaration\n" + "  modifiers="
					+ (JavaNode.PUBLIC | JavaNode.STATIC | JavaNode.ABSTRACT) + "\n"
					+ "  id=region1\n" + "  QualifiedRegionName\n"
					+ "    NamedType\n" + "      type=inner.inner2\n"
					+ "    id=region2\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser(
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
	}

	public void testBadRegionAAST() {
		try {
			SLParse.prototype.initParser("").region().getTree();
			fail("Should have thrown an exception.");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have thrown a recognition exception.");
		}
		/*
		 * try { SLParse.prototype.initParser("1region").region().getTree();
		 * fail("Should have thrown an exception."); } catch
		 * (RecognitionException e) { } catch (Exception e) {
		 * e.printStackTrace(); fail("Should have thrown a recognition
		 * exception."); }
		 */
		try {
			SLParse.prototype.initParser("static public region1 extends")
					.region().getTree();
			fail("Should have thrown an exception.");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have thrown a recognition exception.");
		}
		try {
			SLParse.prototype.initParser("private region1 extends this:[]")
					.region().getTree();
			fail("Should have thrown an exception.");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have thrown a recognition exception.");
		}
		try {
			SLParse.prototype.initParser("region1 extends this.region")
					.region().getTree();
			fail("Should have thrown an exception.");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have thrown a recognition exception.");
		}

		try {
			SLParse.prototype
					.initParser("public this.region1 extends this.region")
					.region().getTree();
			fail("Should have thrown an exception.");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have thrown a recognition exception.");
		}
		try {
			SLParse.prototype.initParser("public region1 extends this:region")
					.region().getTree();
			fail("Should have thrown an exception.");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have thrown a recognition exception.");
		}
		try {
			SLParse.prototype.initParser("final region1").region().getTree();
			fail("Should have thrown an exception.");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have thrown a recognition exception.");
		}
	}

	public void testGoodRequiresLockAAST() {
		String expected;
		try {
			expected = "RequiresLockNode\n" + "  SimpleLockName\n"
					+ "    id=lock1\n\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock1").requiresLock().getTree();
			RequiresLockNode ldn = (RequiresLockNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			// System.out.println(expected + "<<<");
			// System.out.println(ldn.unparse(true) + "<<<");
			assertTrue(ldn.unparse(true).equals(expected));
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception.");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception.");
		}
		try {
			expected = "RequiresLockNode\n" + "  SimpleLockName\n"
					+ "    id=lock1\n" + "\n" + "  SimpleLockName\n"
					+ "    id=lock2\n" + "\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock1, lock2").requiresLock().getTree();
			RequiresLockNode ldn = (RequiresLockNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			// System.out.println(expected + "<<<");
			// System.out.println(ldn.unparse(true) + "<<<");
			assertTrue(ldn.unparse(true).equals(expected));
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception.");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception.");
		}
		try {
			expected = "RequiresLockNode\n" + "  SimpleLockName\n"
					+ "    id=lock1\n" + "\n" + "  SimpleLockName\n"
					+ "    id=lock2\n" + "\n" + "  SimpleLockName\n"
					+ "    id=lock3\n" + "\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock1, lock2, lock3").requiresLock().getTree();
			RequiresLockNode ldn = (RequiresLockNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			// System.out.println(expected + "<<<");
			// System.out.println(ldn.unparse(true) + "<<<");
			assertTrue(ldn.unparse(true).equals(expected));
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception.");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception.");
		}

		try {
			expected = "RequiresLockNode\n" + "  SimpleLockName\n"
					+ "    id=lock1\n" + "\n" + "  SimpleLockName\n"
					+ "    id=lock2\n" + "\n" + "  QualifiedLockName\n"
					+ "    VariableUseExpression\n" + "      id=Type\n"
					+ "    id=lock3\n" + "\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock1, lock2, Type:lock3").requiresLock()
					.getTree();
			RequiresLockNode ldn = (RequiresLockNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			// System.out.println(expected + "<<<");
			// System.out.println(ldn.unparse(true) + "<<<");
			assertTrue(ldn.unparse(true).equals(expected));
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception.");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception.");
		}
		try {
			expected = "RequiresLockNode\n" + "  QualifiedLockName\n"
					+ "    QualifiedThisExpression\n" + "      NamedType\n"
					+ "        type=Type\n" + "    id=lock1\n" + "\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("Type.this:lock1").requiresLock().getTree();
			RequiresLockNode ldn = (RequiresLockNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			// System.out.println(expected + "<<<");
			// System.out.println(ldn.unparse(true) + "<<<");
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
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("this.lock1").requiresLock().getTree();
			RequiresLockNode ldn = (RequiresLockNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown an exception.");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception.");
		}

		try {
			SLParse.prototype.initParser("lock1,").requiresLock().getTree();
			fail("Should have thrown an exception.");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have thrown a recognition exception.");
		}

		try {
			SLParse.prototype.initParser(":lock1").requiresLock().getTree();
			fail("Should have thrown an exception.");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have thrown a recognition exception.");
		}

		try {
			SLParse.prototype.initParser("Type:this.lock1").requiresLock()
					.getTree();
			fail("Should have thrown an exception.");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have thrown a recognition exception.");
		}
	}

	public void testGoodReturnsLockAAST() {
		String expected;
		try {
			expected = "ReturnsLockNode\n" + "    SimpleLockName\n"
					+ "    id=lock1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock1").returnsLock().getTree();
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
			expected = "ReturnsLockNode\n" + "    QualifiedLockName\n"
					+ "    QualifiedThisExpression\n" + "      NamedType\n"
					+ "        type=Type\n" + "    id=lock1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("Type.this:lock1").returnsLock().getTree();
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
			SLParse.prototype.initParser("").returnsLock().getTree();
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("this.lock1").returnsLock().getTree();
			ReturnsLockNode ldn = (ReturnsLockNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		/*
		 * try { SLParse.prototype.initParser("1lock").returnsLock().getTree();
		 * fail("Should have thrown a RecognitionException"); } catch
		 * (RecognitionException e) { } catch (Exception e) {
		 * e.printStackTrace(); fail("Unexpected exception"); }
		 */
		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock1, lock2").returnsLock().getTree();
			ReturnsLockNode ldn = (ReturnsLockNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown an exception.");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
	}

	public void testGoodMapIntoAAST() {
		String expected;
		try {
			expected = "InRegionNode\n" + "    RegionName\n"
					+ "    id=region1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("region1").inRegion().getTree();
			InRegionNode ldn = (InRegionNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			System.out.println(expected);
			System.out.println(ldn.unparse());
			assertTrue(ldn.unparse().equals(expected));
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		try {
			expected = "InRegionNode\n" + "    QualifiedRegionName\n"
					+ "    NamedType\n" + "      type=Type\n"
					+ "    id=region1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("Type:region1").inRegion().getTree();
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
			expected = "InRegionNode\n" + "    QualifiedRegionName\n"
					+ "    NamedType\n" + "      type=Type.type2\n"
					+ "    id=region1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("Type.type2:region1").inRegion().getTree();
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
	}

	public void testBadMapIntoAAST() {
		try {
			SLParse.prototype.initParser("").inRegion().getTree();
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		try {
			SLParse.prototype.initParser(":region1").inRegion().getTree();
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}

		try {
			SLParse.prototype.initParser("region1, region2").inRegion()
					.getTree();
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
	}

	/*
	 * public void testGoodMapFields(){ try{ AASTAdaptor.Node root =
	 * (AASTAdaptor.Node)SLParse.prototype.initParser("region1 into
	 * SuperRegion").mapFields().getTree(); MapFieldsNode node =
	 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
	 * } catch(RecognitionException e){ e.printStackTrace(); fail("Unexpected
	 * exception"); } catch(Exception e){ e.printStackTrace(); fail("Unexpected
	 * exception"); }
	 * 
	 * try{ AASTAdaptor.Node root =
	 * (AASTAdaptor.Node)SLParse.prototype.initParser("region1, region2 into
	 * SuperRegion").mapFields().getTree(); MapFieldsNode node =
	 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
	 * } catch(RecognitionException e){ e.printStackTrace(); fail("Unexpected
	 * exception"); } catch(Exception e){ e.printStackTrace(); fail("Unexpected
	 * exception"); }
	 * 
	 * try{ AASTAdaptor.Node root =
	 * (AASTAdaptor.Node)SLParse.prototype.initParser("region1, region2, region3
	 * into SuperRegion").mapFields().getTree(); MapFieldsNode node =
	 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
	 * } catch(RecognitionException e){ e.printStackTrace(); fail("Unexpected
	 * exception"); } catch(Exception e){ e.printStackTrace(); fail("Unexpected
	 * exception"); }
	 * 
	 * try{ AASTAdaptor.Node root =
	 * (AASTAdaptor.Node)SLParse.prototype.initParser("region1 into
	 * []").mapFields().getTree(); MapFieldsNode node =
	 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
	 * } catch(RecognitionException e){ e.printStackTrace(); fail("Unexpected
	 * exception"); } catch(Exception e){ e.printStackTrace(); fail("Unexpected
	 * exception"); }
	 * 
	 * try{ AASTAdaptor.Node root =
	 * (AASTAdaptor.Node)SLParse.prototype.initParser("region1, region2, region3
	 * into []").mapFields().getTree(); MapFieldsNode node =
	 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
	 * } catch(RecognitionException e){ e.printStackTrace(); fail("Unexpected
	 * exception"); } catch(Exception e){ e.printStackTrace(); fail("Unexpected
	 * exception"); }
	 * 
	 * try{ AASTAdaptor.Node root =
	 * (AASTAdaptor.Node)SLParse.prototype.initParser("region1 into
	 * Type:SuperRegion").mapFields().getTree(); MapFieldsNode node =
	 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
	 * } catch(RecognitionException e){ e.printStackTrace(); fail("Unexpected
	 * exception"); } catch(Exception e){ e.printStackTrace(); fail("Unexpected
	 * exception"); }
	 * 
	 * try{ AASTAdaptor.Node root =
	 * (AASTAdaptor.Node)SLParse.prototype.initParser("region1 into
	 * Type.this:SuperRegion").mapFields().getTree(); MapFieldsNode node =
	 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
	 * } catch(RecognitionException e){ e.printStackTrace(); fail("Unexpected
	 * exception"); } catch(Exception e){ e.printStackTrace(); fail("Unexpected
	 * exception"); }
	 * 
	 * try{ AASTAdaptor.Node root =
	 * (AASTAdaptor.Node)SLParse.prototype.initParser("region1 into
	 * Type.type2:SuperRegion").mapFields().getTree(); MapFieldsNode node =
	 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
	 * } catch(RecognitionException e){ e.printStackTrace(); fail("Unexpected
	 * exception"); } catch(Exception e){ e.printStackTrace(); fail("Unexpected
	 * exception"); } }
	 */

	/*
	 * public void testBadMapFields(){ try{ AASTAdaptor.Node root =
	 * (AASTAdaptor.Node)SLParse.prototype.initParser("").mapFields().getTree();
	 * MapFieldsNode node =
	 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
	 * fail("Should have thrown a RecognitionException"); }
	 * catch(RecognitionException e){ } catch(Exception e){ e.printStackTrace();
	 * fail("Unexpected exception"); }
	 * 
	 * try{ AASTAdaptor.Node root =
	 * (AASTAdaptor.Node)SLParse.prototype.initParser("region1, region2 into
	 * ").mapFields().getTree(); MapFieldsNode node =
	 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
	 * fail("Should have thrown a RecognitionException"); }
	 * catch(RecognitionException e){ } catch(Exception e){ e.printStackTrace();
	 * fail("Unexpected exception"); }
	 * 
	 * try{ AASTAdaptor.Node root =
	 * (AASTAdaptor.Node)SLParse.prototype.initParser("region1, region2, region3
	 * into this.SuperRegion").mapFields().getTree(); MapFieldsNode node =
	 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
	 * fail("Should have thrown a RecognitionException"); }
	 * catch(RecognitionException e){ } catch(Exception e){ e.printStackTrace();
	 * fail("Unexpected exception"); }
	 * 
	 * try{ AASTAdaptor.Node root =
	 * (AASTAdaptor.Node)SLParse.prototype.initParser("region1 into
	 * Array[]").mapFields().getTree(); MapFieldsNode node =
	 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
	 * fail("Should have thrown a RecognitionException"); }
	 * catch(RecognitionException e){ } catch(Exception e){ e.printStackTrace();
	 * fail("Unexpected exception"); }
	 * 
	 * try{ AASTAdaptor.Node root =
	 * (AASTAdaptor.Node)SLParse.prototype.initParser("region1, region2, region3
	 * into Type:[]").mapFields().getTree(); MapFieldsNode node =
	 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
	 * fail("Should have thrown a RecognitionException"); }
	 * catch(RecognitionException e){ } catch(Exception e){ e.printStackTrace();
	 * fail("Unexpected exception"); }
	 * 
	 * try{ AASTAdaptor.Node root =
	 * (AASTAdaptor.Node)SLParse.prototype.initParser
	 * ("region1").mapFields().getTree(); MapFieldsNode node =
	 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
	 * fail("Should have thrown a RecognitionException"); }
	 * catch(RecognitionException e){ } catch(Exception e){ e.printStackTrace();
	 * fail("Unexpected exception"); }
	 * 
	 * try{ AASTAdaptor.Node root =
	 * (AASTAdaptor.Node)SLParse.prototype.initParser("region1, region2,
	 * region3").mapFields().getTree(); MapFieldsNode node =
	 * (MapFieldsNode)root.finalizeAST(IAnnotationParsingContext.nullPrototype);
	 * fail("Should have thrown a RecognitionException"); }
	 * catch(RecognitionException e){ } catch(Exception e){ e.printStackTrace();
	 * fail("Unexpected exception"); } }
	 */

	public void testGoodAggregate() {
		String expected;
		try {
			expected = "UniqueMappingNode\n" + "    MappedRegionSpecification\n"
					+ "    RegionMapping\n" + "      RegionName\n"
					+ "        id=region1\n" + "      RegionName\n"
					+ "        id=SuperRegion1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("region1 into SuperRegion1").uniqueInRegion()
					.getTree();
			UniqueMappingNode node = (UniqueMappingNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			//String unparse = node.unparse();
			assertTrue(node.unparse().equals(expected));
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}

		try {
			expected = "UniqueMappingNode\n" + "    MappedRegionSpecification\n"
					+ "    RegionMapping\n" + "      RegionName\n"
					+ "        id=region1\n" + "      RegionName\n"
					+ "        id=SuperRegion1\n" + "    RegionMapping\n"
					+ "      RegionName\n" + "        id=region2\n"
					+ "      RegionName\n" + "        id=SuperRegion1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser(
							"region1 into SuperRegion1, region2 into SuperRegion1")
					.uniqueInRegion().getTree();
			UniqueMappingNode node = (UniqueMappingNode) root
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
			expected = "UniqueMappingNode\n" + "    MappedRegionSpecification\n"
					+ "    RegionMapping\n" + "      RegionName\n"
					+ "        id=region1\n" + "      RegionName\n"
					+ "        id=SuperRegion1\n" + "    RegionMapping\n"
					+ "      RegionName\n" + "        id=region2\n"
					+ "      RegionName\n" + "        id=SuperRegion1\n"
					+ "    RegionMapping\n" + "      RegionName\n"
					+ "        id=region3\n" + "      RegionName\n"
					+ "        id=SuperRegion1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser(
							"region1 into SuperRegion1, region2 into SuperRegion1, region3 into SuperRegion1")
					.uniqueInRegion().getTree();
			UniqueMappingNode node = (UniqueMappingNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			assertTrue(node.unparse().equals(expected));
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		/*
		 * try { expected = "AggregateNode\n"+
		 * "    MappedRegionSpecification\n"+ "    RegionMapping\n" +
		 * "      QualifiedRegionName\n"+ "        NamedType\n" +
		 * "          type=Type\n" + "        id=region1\n" +
		 * "      RegionName\n"+ "        id=SuperRegion1\n" +
		 * "    RegionMapping\n" + "      QualifiedRegionName\n"+
		 * "        NamedType\n" + "          type=Type\n" +
		 * "        id=region2\n" + "      RegionName\n"+
		 * "        id=SuperRegion1\n"; AASTAdaptor.Node root =
		 * (AASTAdaptor.Node) SLParse .initParser(
		 * "Type:region1 into SuperRegion1, Type:region2 into SuperRegion1")
		 * .uniqueInRegion().getTree(); AggregateNode node = (AggregateNode) root
		 * .finalizeAST(IAnnotationParsingContext.nullPrototype);
		 * assertTrue(node.unparse().equals(expected)); } catch
		 * (RecognitionException e) { e.printStackTrace();
		 * fail("Unexpected exception"); } catch (Exception e) {
		 * e.printStackTrace(); fail("Unexpected exception"); }
		 */

		try {
			expected = "UniqueMappingNode\n" + "    MappedRegionSpecification\n"
					+ "    RegionMapping\n" + "      RegionName\n"
					+ "        id=region1\n" + "      QualifiedRegionName\n"
					+ "        NamedType\n" + "          type=Type\n"
					+ "        id=SuperRegion1\n" + "    RegionMapping\n"
					+ "      RegionName\n" + "        id=region2\n"
					+ "      QualifiedRegionName\n" + "        NamedType\n"
					+ "          type=Type\n" + "        id=SuperRegion1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser(
							"region1 into Type:SuperRegion1, region2 into Type:SuperRegion1")
					.uniqueInRegion().getTree();
			UniqueMappingNode node = (UniqueMappingNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			assertTrue(node.unparse().equals(expected));
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}

		/*
		 * try { expected = "AggregateNode\n"+
		 * "    MappedRegionSpecification\n"+ "    RegionMapping\n" +
		 * "      QualifiedRegionName\n"+ "        NamedType\n" +
		 * "          type=Type.Region\n" + "        id=region1\n" +
		 * "      RegionName\n"+ "        id=SuperRegion1\n" +
		 * "    RegionMapping\n" + "      QualifiedRegionName\n"+
		 * "        NamedType\n" + "          type=Type.Region\n" +
		 * "        id=region2\n" + "      RegionName\n"+
		 * "        id=SuperRegion1\n"; AASTAdaptor.Node root =
		 * (AASTAdaptor.Node) SLParse .initParser(
		 * "Type.Region:region1 into SuperRegion1, Type.Region:region2 into SuperRegion1"
		 * ) .uniqueInRegion().getTree(); AggregateNode node = (AggregateNode) root
		 * .finalizeAST(IAnnotationParsingContext.nullPrototype);
		 * assertTrue(node.unparse().equals(expected)); } catch
		 * (RecognitionException e) { e.printStackTrace();
		 * fail("Unexpected exception"); } catch (Exception e) {
		 * e.printStackTrace(); fail("Unexpected exception"); }
		 */
		try {
			expected = "UniqueMappingNode\n" + "    MappedRegionSpecification\n"
					+ "    RegionMapping\n" + "      RegionName\n"
					+ "        id=region1\n" + "      QualifiedRegionName\n"
					+ "        NamedType\n" + "          type=Type.Region\n"
					+ "        id=SuperRegion1\n" + "    RegionMapping\n"
					+ "      RegionName\n" + "        id=region2\n"
					+ "      QualifiedRegionName\n" + "        NamedType\n"
					+ "          type=Type.Region\n"
					+ "        id=SuperRegion1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser(
							"region1 into Type.Region:SuperRegion1, region2 into Type.Region:SuperRegion1")
					.uniqueInRegion().getTree();
			UniqueMappingNode node = (UniqueMappingNode) root
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
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("").uniqueInRegion().getTree();
			UniqueMappingNode node = (UniqueMappingNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}

		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("region1 into SuperRegion1, SuperRegion1")
					.uniqueInRegion().getTree();
			UniqueMappingNode node = (UniqueMappingNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}

		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("region1, region2 into SuperRegion1")
					.uniqueInRegion().getTree();
			UniqueMappingNode node = (UniqueMappingNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}

		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("Type.region1 into SuperRegion1 ").uniqueInRegion()
					.getTree();
			UniqueMappingNode node = (UniqueMappingNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}

		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("region1 into Type:Type:SuperRegion1")
					.uniqueInRegion().getTree();
			UniqueMappingNode node = (UniqueMappingNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}

		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("Type.Region.region1 into this.SuperRegion1")
					.uniqueInRegion().getTree();
			UniqueMappingNode node = (UniqueMappingNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}

		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser(
							"region1 into SuperRegion1 region2 into SuperRegion1")
					.uniqueInRegion().getTree();
			UniqueMappingNode node = (UniqueMappingNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
	}

	public void testGoodPolicyLock() {
		String expected;
		try {
			expected = "PolicyLockDeclaration\n" + "  id=lock\n"
					+ "  ThisExpression\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock is this").policyLock().getTree();
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
			expected = "PolicyLockDeclaration\n" + "  id=lock\n"
					+ "  ImplicitClassLockExpression\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock is class").policyLock().getTree();
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
			expected = "PolicyLockDeclaration\n" + "  id=lock\n"
					+ "  ImplicitClassLockExpression\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock is this.class").policyLock().getTree();
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
			expected = "PolicyLockDeclaration\n" + "  id=lock\n"
					+ "  FieldRef\n" + "    ThisExpression\n" + "    id=Lock\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock is Lock").policyLock().getTree();
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
			expected = "PolicyLockDeclaration\n" + "  id=lock\n"
					+ "  FieldRef\n" + "    TypeExpression\n"
					+ "      NamedType\n" + "        type=Type\n"
					+ "    id=Lock\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock is Type.Lock").policyLock().getTree();
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
			expected = "PolicyLockDeclaration\n" + "  id=lock\n"
					+ "  FieldRef\n" + "    TypeExpression\n"
					+ "      NamedType\n" + "        type=Type.lock\n"
					+ "    id=Lock\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock is Type.lock.Lock").policyLock()
					.getTree();
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
			expected = "PolicyLockDeclaration\n" + "  id=lock\n"
					+ "  QualifiedThisExpression\n" + "    NamedType\n"
					+ "      type=Type\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock is Type.this").policyLock().getTree();
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
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("").policyLock().getTree();
			PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}

		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock").policyLock().getTree();
			PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}

		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock is ").policyLock().getTree();
			PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}

		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser(" is this").policyLock().getTree();
			PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}

		/*
		 * try { AASTAdaptor.Node root = (AASTAdaptor.Node)
		 * SLParse.prototype.initParser(
		 * "lock is this.class").policyLock().getTree();
		 * PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
		 * .finalizeAST(IAnnotationParsingContext.nullPrototype);
		 * fail("Should have thrown a RecognitionException"); } catch
		 * (RecognitionException e) { } catch (Exception e) {
		 * e.printStackTrace(); fail("Unexpected exception"); }
		 */
		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock is Type:this").policyLock().getTree();
			PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
	}

	public void testGoodRegionEffects() {
		String expected;
		try {
			expected = "RegionEffects\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("none").regionEffects().getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
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
			expected = "RegionEffects\n" + "  Reads\n"
					+ "    EffectSpecification\n" + "      isWrite=false\n"
					+ "      ImplicitQualifier\n" + "      RegionName\n"
					+ "        id=region1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("reads region1").regionEffects().getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			String result = node.unparse();
			System.out.println(expected);
			System.out.println(result);
			assertTrue(result.equals(expected));
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		try {
			expected = "RegionEffects\n" + "  Reads\n"
					+ "    EffectSpecification\n" + "      isWrite=false\n"
					+ "      AnyInstanceExpression\n" + "        NamedType\n"
					+ "          type=Type\n" + "      RegionName\n"
					+ "        id=region1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("reads any(Type):region1").regionEffects()
					.getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
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
			expected = "RegionEffects\n" + "  Reads\n"
					+ "    EffectSpecification\n" + "      isWrite=false\n"
					+ "      AnyInstanceExpression\n" + "        NamedType\n"
					+ "          type=Type.type\n" + "      RegionName\n"
					+ "        id=region1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("reads any(Type.type):region1").regionEffects()
					.getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			assertTrue(node.unparse().equals(expected));
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		/*
		 * try { expected = "RegionEffects\n"+ "  Reads\n" +
		 * "    EffectSpecification\n" + "      isWrite=false\n"+
		 * "      SuperExpression\n"+ "      RegionName\n"+
		 * "        id=Region1\n"; AASTAdaptor.Node root = (AASTAdaptor.Node)
		 * SLParse.prototype.initParser(
		 * "reads super:Region1").regionEffects().getTree(); RegionEffectsNode
		 * node = (RegionEffectsNode) root
		 * .finalizeAST(IAnnotationParsingContext.nullPrototype);
		 * assertTrue(node.unparse().equals(expected)); } catch
		 * (RecognitionException e) { e.printStackTrace();
		 * fail("Unexpected exception"); } catch (Exception e) {
		 * e.printStackTrace(); fail("Unexpected exception"); }
		 */
	}

	public void testGoodRegionEffects2() {
		String expected;
		try {
			expected = "RegionEffects\n" + "  Reads\n"
					+ "    EffectSpecification\n" + "      isWrite=false\n"
					+ "      ImplicitQualifier\n" + "      RegionName\n"
					+ "        id=region1\n" + "    EffectSpecification\n"
					+ "      isWrite=false\n" + "      ImplicitQualifier\n"
					+ "      RegionName\n" + "        id=region2\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("reads region1, region2").regionEffects()
					.getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			String result = node.unparse();
			System.out.println(expected);
			System.out.println(result);
			assertTrue(result.equals(expected));
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		try {
			expected = "RegionEffects\n" + "  Reads\n"
					+ "    EffectSpecification\n" + "      isWrite=false\n"
					+ "      QualifiedThisExpression\n" + "        NamedType\n"
					+ "          type=Class\n" + "      RegionName\n"
					+ "        id=region1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("reads Class.this:region1").regionEffects()
					.getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
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
			expected = "RegionEffects\n" + "  Reads\n"
					+ "    EffectSpecification\n" + "      isWrite=false\n"
					+ "      ThisExpression\n" + "      RegionName\n"
					+ "        id=region1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("reads this:region1").regionEffects().getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			assertTrue(node.unparse().equals(expected));
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}

		/* ****************** Test Writes *************** */
		try {
			expected = "RegionEffects\n" + "  Writes\n"
					+ "    EffectSpecification\n" + "      isWrite=false\n"
					+ "      ImplicitQualifier\n" + "      RegionName\n"
					+ "        id=region1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("writes region1").regionEffects().getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
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
			expected = "RegionEffects\n" + "  Writes\n"
					+ "    EffectSpecification\n" + "      isWrite=false\n"
					+ "      AnyInstanceExpression\n" + "        NamedType\n"
					+ "          type=Type\n" + "      RegionName\n"
					+ "        id=region1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("writes any(Type):region1").regionEffects()
					.getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
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
			expected = "RegionEffects\n" + "  Writes\n"
					+ "    EffectSpecification\n" + "      isWrite=false\n"
					+ "      AnyInstanceExpression\n" + "        NamedType\n"
					+ "          type=Type.type\n" + "      RegionName\n"
					+ "        id=region1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("writes any(Type.type):region1")
					.regionEffects().getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			assertTrue(node.unparse().equals(expected));
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		/*
		 * try { expected = "RegionEffects\n"+ "  Writes\n" +
		 * "    EffectSpecification\n" + "      isWrite=false\n"+
		 * "      SuperExpression\n"+ "      RegionName\n"+
		 * "        id=region1\n"; AASTAdaptor.Node root = (AASTAdaptor.Node)
		 * SLParse.prototype.initParser(
		 * "writes super:region1").regionEffects().getTree(); RegionEffectsNode
		 * node = (RegionEffectsNode) root
		 * .finalizeAST(IAnnotationParsingContext.nullPrototype);
		 * assertTrue(node.unparse().equals(expected)); } catch
		 * (RecognitionException e) { e.printStackTrace();
		 * fail("Unexpected exception"); } catch (Exception e) {
		 * e.printStackTrace(); fail("Unexpected exception"); }
		 */
		try {
			expected = "RegionEffects\n" + "  Writes\n"
					+ "    EffectSpecification\n" + "      isWrite=false\n"
					+ "      ImplicitQualifier\n" + "      RegionName\n"
					+ "        id=region1\n" + "    EffectSpecification\n"
					+ "      isWrite=false\n" + "      ImplicitQualifier\n"
					+ "      RegionName\n" + "        id=region2\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("writes region1, region2").regionEffects()
					.getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
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
			expected = "RegionEffects\n" + "  Writes\n"
					+ "    EffectSpecification\n" + "      isWrite=false\n"
					+ "      QualifiedThisExpression\n" + "        NamedType\n"
					+ "          type=Class\n" + "      RegionName\n"
					+ "        id=region1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("writes Class.this:region1").regionEffects()
					.getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
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
			expected = "RegionEffects\n" + "  Writes\n"
					+ "    EffectSpecification\n" + "      isWrite=false\n"
					+ "      ThisExpression\n" + "      RegionName\n"
					+ "        id=region1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("writes this:region1").regionEffects()
					.getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
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
			expected = "RegionEffects\n" + "  Writes\n"
					+ "    EffectSpecification\n" + "      isWrite=false\n"
					+ "      QualifiedThisExpression\n" + "        NamedType\n"
					+ "          type=Class\n" + "      RegionName\n"
					+ "        id=region1\n" + "  Reads\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("writes Class.this:region1; reads nothing")
					.regionEffects().getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
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
			expected = "RegionEffects\n" + "  Reads\n"
					+ "    EffectSpecification\n" + "      isWrite=false\n"
					+ "      ThisExpression\n" + "      RegionName\n"
					+ "        id=region\n" + "    EffectSpecification\n"
					+ "      isWrite=false\n" + "      TypeExpression\n"
					+ "        NamedType\n"
					+ "          type=com.sun.java.Test\n"
					+ "      RegionName\n" + "        id=Region\n"
					+ "  Writes\n" + "    EffectSpecification\n"
					+ "      isWrite=false\n" + "      ThisExpression\n"
					+ "      RegionName\n" + "        id=region1\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser(
							"reads this:region, com.sun.java.Test:Region; writes this:region1")
					.regionEffects().getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			System.out.println(node.unparse());
			assertTrue(node.unparse().equals(expected));
		} catch (RecognitionException e) {
			e.printStackTrace();
			fail("Unexpected exception");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
	}

	public void testBadRegionEffects() {

		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("").regionEffects().getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("reads any(Type).region").regionEffects()
					.getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		/*
		 * nothing is now considered an identifier try { AASTAdaptor.Node root =
		 * (AASTAdaptor.Node) SLParse.prototype.initParser(
		 * "reads region1, nothing").regionEffects().getTree();
		 * RegionEffectsNode node = (RegionEffectsNode) root
		 * .finalizeAST(IAnnotationParsingContext.nullPrototype);
		 * fail("Should have thrown a RecognitionException"); } catch
		 * (RecognitionException e) { } catch (Exception e) {
		 * e.printStackTrace(); fail("Unexpected exception"); }
		 */
		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("reads Type.this.region.[]").regionEffects()
					.getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
	}

	public void testBadWrites() {
		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("").regionEffects().getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("any(Type).region").regionEffects().getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("region1, nothing").regionEffects().getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("Type.this.region.[]").regionEffects()
					.getTree();
			RegionEffectsNode node = (RegionEffectsNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
	}

	public void testGoodIsLock() {
		String expected;

		try {
			expected = "IsLockNode\n" + "    SimpleLockName\n" + "    id=L\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("L").isLock().getTree();
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
			expected = "IsLockNode\n" + "    SimpleLockName\n"
					+ "    id=lock\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock").isLock().getTree();
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
			expected = "IsLockNode\n" + "    SimpleLockName\n"
					+ "    id=myLock\n";
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("myLock").isLock().getTree();
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
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("").isLock().getTree();
			IsLockNode node = (IsLockNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("lock1, lock2").isLock().getTree();
			IsLockNode node = (IsLockNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("this.lock").isLock().getTree();
			IsLockNode node = (IsLockNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("this.class").isLock().getTree();
			IsLockNode node = (IsLockNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype
					.initParser("Type:lock").isLock().getTree();
			IsLockNode node = (IsLockNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			fail("Should have thrown a RecognitionException");
		} catch (RecognitionException e) {
			// OK
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
	}

	public void testGoodLocksRulePlacement() {
		try {
			LockRulesTestHelper lrth = LockRulesTestHelper.getInstance();
		} catch (Error e) {
			e.printStackTrace();
		}

		Lock_ParseRuleHelper lockRulesHelper = new Lock_ParseRuleHelper();

		RequiresLock_ParseRuleHelper requiresLockRulesHelper = new RequiresLock_ParseRuleHelper();

		ReturnsLock_ParseRuleHelper returnsLockRulesHelper = new ReturnsLock_ParseRuleHelper();

		PolicyLock_ParseRuleHelper policyLockRulesHelper = new PolicyLock_ParseRuleHelper();

		ThreadSafe_ParseRuleHelper threadSafeRulesHelper = new ThreadSafe_ParseRuleHelper();

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

		/*
		 * ****************** Test @ThreadSafe
		 * *******************************
		 */
		context.setProperty(AbstractModifiedBooleanNode.APPLIES_TO, Part.InstanceAndStatic.toString());
		context.setOp(ClassDeclaration.prototype);
		threadSafeRulesHelper.parse(context, "");
		assertTrue(context.getError() == null);
		assertTrue(context.getException() == null);
		assertTrue(context.getOffset() == 0);

		context.setOp(InterfaceDeclaration.prototype);
		threadSafeRulesHelper.parse(context, "");
		assertTrue(context.getError() == null);
		assertTrue(context.getException() == null);
		assertTrue(context.getOffset() == 0);

		context.setOp(EnumDeclaration.prototype);
		threadSafeRulesHelper.parse(context, "");
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

		ThreadSafe_ParseRuleHelper threadSafeRulesHelper = new ThreadSafe_ParseRuleHelper();

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

		/*
		 * ****************** Test @ThreadSafe
		 * *******************************
		 */
		context.setOp(MethodDeclaration.prototype);
		threadSafeRulesHelper.parse(context, "");
		assertTrue(context.getError() != null);
		assertTrue(context.getException() == null);
		assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

		context.setOp(PackageDeclaration.prototype);
		threadSafeRulesHelper.parse(context, "");
		assertTrue(context.getError() != null);
		assertTrue(context.getException() == null);
		assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

		context.setOp(ParameterDeclaration.prototype);
		threadSafeRulesHelper.parse(context, "");
		assertTrue(context.getError() != null);
		assertTrue(context.getException() == null);
		assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

		context.setOp(FieldDeclaration.prototype);
		threadSafeRulesHelper.parse(context, "");
		assertTrue(context.getError() != null);
		assertTrue(context.getException() == null);
		assertTrue(context.getOffset() == IAnnotationParsingContext.UNKNOWN);

		context.setOp(ConstructorDeclaration.prototype);
		threadSafeRulesHelper.parse(context, "");
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
		 *            context.setOp(InterfaceDeclaration.prototype);
		 *            mapFieldsHelper.parse(context, "region1 into region2");
		 *            assertTrue(context.getError() == null);
		 *            assertTrue(context.getException() == null);
		 *            assertTrue(context.getOffset() == 0);
		 * 
		 *            context.setOp(EnumDeclaration.prototype);
		 *            mapFieldsHelper.parse(context, "region1 into region2");
		 *            assertTrue(context.getError() == null);
		 *            assertTrue(context.getException() == null);
		 *            assertTrue(context.getOffset() == 0);
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
		RegionEffects_ParseRuleHelper helper = new RegionEffects_ParseRuleHelper();

		TestContext context = new TestContext(null, MethodDeclaration.prototype);

		/* ****************** Test @Reads ****************** */

		helper.parse(context, "reads nothing");
		assertTrue(context.getError() == null);
		assertTrue(context.getException() == null);
		assertTrue(context.getOffset() == 0);

		/* ****************** Test @Writes ****************** */

		helper.parse(context, "writes nothing");
		assertTrue(context.getError() == null);
		assertTrue(context.getException() == null);
		assertTrue(context.getOffset() == 0);
	}

	public void testBadMethodEffectsRulesPlacement() {
		MethodEffectsRulesHelper merth = MethodEffectsRulesHelper.getInstance();
		RegionEffects_ParseRuleHelper helper = new RegionEffects_ParseRuleHelper();

		TestContext context = new TestContext(null,
				PackageDeclaration.prototype);

		/* ****************** Test @Reads ****************** */

		helper.parse(context, "reads nothing");
		assertFalse(context.getError() == null);
		assertTrue(context.getException() == null);
		assertFalse(context.getOffset() == 0);

		context.setOp(ConstructorDeclaration.prototype);
		helper.parse(context, "reads nothing");
		assertFalse(context.getError() == null);
		assertTrue(context.getException() == null);
		assertFalse(context.getOffset() == 0);

		context.setOp(ClassDeclaration.prototype);
		helper.parse(context, "reads nothing");
		assertFalse(context.getError() == null);
		assertTrue(context.getException() == null);
		assertFalse(context.getOffset() == 0);

		context.setOp(FieldDeclaration.prototype);
		helper.parse(context, "reads nothing");
		assertFalse(context.getError() == null);
		assertTrue(context.getException() == null);
		assertFalse(context.getOffset() == 0);

		context.setOp(ParameterDeclaration.prototype);
		helper.parse(context, "reads nothing");
		assertFalse(context.getError() == null);
		assertTrue(context.getException() == null);
		assertFalse(context.getOffset() == 0);

		context.setOp(ClassInitDeclaration.prototype);
		helper.parse(context, "reads nothing");
		assertFalse(context.getError() == null);
		assertTrue(context.getException() == null);
		assertFalse(context.getOffset() == 0);

		/* ****************** Test @Writes ****************** */

		context.setOp(ConstructorDeclaration.prototype);
		helper.parse(context, "writes nothing");
		assertFalse(context.getError() == null);
		assertTrue(context.getException() == null);
		assertFalse(context.getOffset() == 0);

		context.setOp(PackageDeclaration.prototype);
		helper.parse(context, "writes nothing");
		assertFalse(context.getError() == null);
		assertTrue(context.getException() == null);
		assertFalse(context.getOffset() == 0);

		context.setOp(InterfaceDeclaration.prototype);
		helper.parse(context, "writes nothing");
		assertFalse(context.getError() == null);
		assertTrue(context.getException() == null);
		assertFalse(context.getOffset() == 0);

		context.setOp(EnumDeclaration.prototype);
		helper.parse(context, "writes nothing");
		assertFalse(context.getError() == null);
		assertTrue(context.getException() == null);
		assertFalse(context.getOffset() == 0);

		context.setOp(ClassDeclaration.prototype);
		helper.parse(context, "writes nothing");
		assertFalse(context.getError() == null);
		assertTrue(context.getException() == null);
		assertFalse(context.getOffset() == 0);

		context.setOp(ParameterDeclaration.prototype);
		helper.parse(context, "writes nothing");
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
		Map<String,String> props = new HashMap<String, String>();
		
		/**
		 * @param src
		 */
		protected TestContext(AnnotationSource src, Operator op) {
			super(src);
			this.op = op;
		}

		void setProperty(String key, String value) {
			props.put(key, value);
		}

		@Override
		public String getProperty(String key) {
			return props.get(key);
		}
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see com.surelogic.annotation.IAnnotationParsingContext#getOp()
		 */
		@Override
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
		 * @see
		 * com.surelogic.annotation.IAnnotationParsingContext#reportAAST(int,
		 * com.surelogic.annotation.AnnotationLocation, java.lang.Object,
		 * com.surelogic.aast.IAASTRootNode)
		 */
		@Override
    public <T extends IAASTRootNode> void reportAAST(int offset,
				AnnotationLocation loc, Object o, T ast) {
			// Nothing to do
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.surelogic.annotation.IAnnotationParsingContext#reportError(int,
		 * java.lang.String)
		 */
		@Override
    public void reportError(int offset, String msg) {
			errorOffset = offset;
			errorMsg = msg;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.surelogic.annotation.IAnnotationParsingContext#reportException
		 * (int, java.lang.Exception)
		 */
		@Override
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
		protected IRNode getAnnoNode() {
			return null;
		}

	}

}
