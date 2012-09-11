/**
 * 
 */
package com.surelogic.annotation.rules;

/**
 * @author ethan
 *
 */
public class LockRulesTestHelper extends LockRules {
	
	private static final LockRulesTestHelper instance = new LockRulesTestHelper();
	
	public static LockRulesTestHelper getInstance(){
		return instance;
	}
	
	public static class Lock_ParseRuleHelper extends Lock_ParseRule{
		public Lock_ParseRuleHelper(){
			super(null);
		}
	}
	public static class PolicyLock_ParseRuleHelper extends PolicyLock_ParseRule{
		
		public PolicyLock_ParseRuleHelper(){
			super();
		}
	}
	public static class RequiresLock_ParseRuleHelper extends RequiresLock_ParseRule{

		public RequiresLock_ParseRuleHelper(){
			super();
		}
	}
	public static class ReturnsLock_ParseRuleHelper extends ReturnsLock_ParseRule{
		
		public ReturnsLock_ParseRuleHelper(){
			super();
		}
	}
	public static class ThreadSafe_ParseRuleHelper extends ThreadSafe_ParseRule{
		
		public ThreadSafe_ParseRuleHelper(){
			super();
		}
	}

}
