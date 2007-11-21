/**
 * 
 */
package com.surelogic.jsure.tests;

import com.surelogic.aast.promise.NewRegionDeclarationNode;

import junit.framework.TestCase;

/**
 * @author ethan
 *
 */
public class TestScrubber extends TestCase {

		/**
		 * @param name
		 */
		public TestScrubber(String name) {
				super(name);
		}

		/* (non-Javadoc)
		 * @see junit.framework.TestCase#setUp()
		 */
		protected void setUp() throws Exception {
				super.setUp();
		}

		/* (non-Javadoc)
		 * @see junit.framework.TestCase#tearDown()
		 */
		protected void tearDown() throws Exception {
				super.tearDown();
		}
		
		/********************************************
		 * Region scrubbing
		 ********************************************/
		
		public void testAggregateScrubbing(){
				fail("Unimplemented");
		}
		
		public void testRegionScrubbing(){
				fail("Unimplemented");
				NewRegionDeclarationNode regionDecl = new NewRegionDeclarationNode(0, 0, "region1", null);
		}
		
		public void testMapIntoScrubbing(){
				fail("Unimplemented");
		}
		
		public void testMapFieldsScrubbing(){
				fail("Unimplemented");
		}

		/********************************************
		 * Lock scrubbing
		 ********************************************/
		
		public void testLockScrubbing(){
				
				fail("Unimplemented");
		}
		
		public void testPolicyLockScrubbing(){
				
				fail("Unimplemented");
		}
		
		public void testRequiresLockScrubbing(){
				
				fail("Unimplemented");
		}
		
		public void testReturnsLockScrubbing(){
				
				fail("Unimplemented");
		}
		
		public void testIsLocScrubbing(){
				
				fail("Unimplemented");
		}
		
		
		/********************************************
		 * MethodEffects scrubbing
		 ********************************************/
		
		public void testMethodEffectsScrubbing(){
				fail("Unimplemented");
		}
}
