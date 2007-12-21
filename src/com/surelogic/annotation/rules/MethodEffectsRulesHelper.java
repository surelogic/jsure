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
		
		public static class RegionEffects_ParseRuleHelper extends RegionEffects_ParseRule{
				public RegionEffects_ParseRuleHelper(){
						super();
				}
		}
}
