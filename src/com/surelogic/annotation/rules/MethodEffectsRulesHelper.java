/**
 * 
 */
package com.surelogic.annotation.rules;

/**
 * @author ethan
 *
 */
public class MethodEffectsRulesHelper extends MethodEffectsRules {
		private static final MethodEffectsRulesHelper instance = new MethodEffectsRulesHelper();
		
		public static MethodEffectsRulesHelper getInstance(){
				return instance;
		}

		public static class Reads_ParseRuleHelper extends Reads_ParseRule{
				public Reads_ParseRuleHelper(){
						super();
				}
		}
		
		public static class Writes_ParseRuleHelper extends Writes_ParseRule{
				public Writes_ParseRuleHelper(){
						super();
				}
		}
}
