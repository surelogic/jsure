/**
 * 
 */
package com.surelogic.jsure.tests;

import java.util.List;

import junit.framework.TestCase;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.java.TypeNode;
import com.surelogic.aast.promise.*;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.parse.*;


/**
 * @author ethan
 * 
 */
public class TestAASTCloning extends TestCase {

	/**
	 * @param name
	 */
	public TestAASTCloning(String name) {
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

	public void testLockCloning() {
		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype.initParser(
					"L1 is lock protects region").lock().getTree();

			LockDeclarationNode node = (LockDeclarationNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);

			LockDeclarationNode clone = (LockDeclarationNode) node.cloneTree();
			assertFalse(node == clone);
			assertFalse(node.getId() == clone.getId());
			assertTrue(node.getId().equals(clone.getId()));
			assertTrue(node.getOffset() == clone.getOffset());
			
			assertFalse(node.getField() == clone.getField());
			assertFalse(node.getRegion() == clone.getRegion());
			
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
	}

	public void testAggregateCloning() {
		try {
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype.initParser("region1 into SuperRegion")
					.uniqueInRegion().getTree();
			UniqueMappingNode anode = (UniqueMappingNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);

			UniqueMappingNode aClone = (UniqueMappingNode) anode.cloneTree();
			assertFalse(anode == aClone);
			MappedRegionSpecificationNode anodeMRS = anode.getSpec();
			MappedRegionSpecificationNode aCloneMRS = aClone.getSpec();

			assertFalse(anodeMRS == aCloneMRS);
			List<RegionMappingNode> aNodeML = anodeMRS.getMappingList();
			List<RegionMappingNode> aCloneML = aCloneMRS.getMappingList();

			assertFalse(aNodeML == aCloneML);
			assertTrue(aNodeML.size() == aCloneML.size());
			for (int i = 0, len = aNodeML.size(); i < len; i++) {
				RegionMappingNode n1 = aNodeML.get(i);
				RegionMappingNode n2 = aCloneML.get(i);

				assertFalse(n1 == n2);

				assertFalse(n1.getFrom() == n2.getFrom());
				assertFalse(n1.getFrom().getId() == n2.getFrom().getId());
				assertTrue(n1.getFrom().getId().equals(n2.getFrom().getId()));

				assertFalse(n1.getTo() == n2.getTo());
				assertFalse(n1.getTo().getId() == n2.getTo().getId());
				assertTrue(n1.getTo().getId().equals(n2.getTo().getId()));

			}

		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
	}

	public void testPolicyLockCloning(){
		try{
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype.initParser("lock is this.class")
					.policyLock().getTree();
			PolicyLockDeclarationNode node = (PolicyLockDeclarationNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			
			PolicyLockDeclarationNode clone = (PolicyLockDeclarationNode) node.cloneTree();
			assertFalse(node == clone);
			assertFalse(node.getId() == clone.getId());
			assertTrue(node.getId().equals(clone.getId()));
			assertTrue(node.getOffset() == clone.getOffset());
			assertFalse(node.getField() == clone.getField());
			
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
	}
	
	public void testRegionCloning(){
		try{
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype.initParser("protected region1 extends region2")
					.region().getTree();
			RegionDeclarationNode node = (RegionDeclarationNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			
			RegionDeclarationNode clone = (RegionDeclarationNode) node.cloneTree();
			assertFalse(node == clone);
			assertFalse(node.getId() == clone.getId());
			assertTrue(node.getId().equals(clone.getId()));
			assertTrue(node.getOffset() == clone.getOffset());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
	}
	
	public void testRequiresLock(){
		try{
			AASTAdaptor.Node root = (AASTAdaptor.Node) SLParse.prototype.initParser("lock1, lock2, lock3, lock4")
					.requiresLock().getTree();
			RequiresLockNode node = (RequiresLockNode) root
					.finalizeAST(IAnnotationParsingContext.nullPrototype);
			
			RequiresLockNode clone = (RequiresLockNode) node.cloneTree();
			assertFalse(node == clone);
			
			assertTrue(node.getOffset() == clone.getOffset());
			
			List<LockSpecificationNode> list1 = node.getLockList();
			List<LockSpecificationNode> list2 = clone.getLockList();
			assertTrue(list1.size() == list2.size());
			for(int i= 0, len = list1.size(); i < len; i++){
				LockSpecificationNode l1 = list1.get(i);
				LockSpecificationNode l2 = list2.get(i);
				assertFalse(l1 == l2);
				assertFalse(l1.getLock() == l2.getLock());
				assertFalse(l1.getLock().getId() == l2.getLock().getId());
				assertTrue(l1.getLock().getId().equals(l2.getLock().getId()));
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
	}
	
	
	public void testScopedPromiseCloning(){
		try{
			ConstructorDeclPatternNode node = (ConstructorDeclPatternNode)createScopedPromise("@Reads(Instance) for public new(int, String, char) in Foo");
			ConstructorDeclPatternNode clone = (ConstructorDeclPatternNode)node.cloneTree();
			
			assertFalse(node == clone);
			assertFalse(node.getInPattern() == clone.getInPattern());
			assertTrue(node.getMods() == clone.getMods());
			
			List<TypeNode> sig1 = node.getSigList();
			List<TypeNode> sig2 = clone.getSigList();
			
			assertFalse(sig1 == sig2);
			assertTrue(sig1.size() == sig2.size());
			for(int i = 0, len = sig1.size(); i < len; i++){
				TypeNode n1 = sig1.get(i);
				TypeNode n2 = sig2.get(i);
				
				assertFalse(n1 == n2);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception");
		}
	}
	
	
	private PromiseTargetNode createScopedPromise(String annoText)
			throws RecognitionException, Exception {
		ScopedPromiseAdaptor.Node root = (ScopedPromiseAdaptor.Node) ScopedPromiseParse.prototype
				.initParser(annoText).scopedPromise().getTree();

		// ScopedPromiseParse.printAST(root);

		ScopedPromiseNode node = (ScopedPromiseNode) root
				.finalizeAST(IAnnotationParsingContext.nullPrototype);

		return node.getTargets();
	}
}
