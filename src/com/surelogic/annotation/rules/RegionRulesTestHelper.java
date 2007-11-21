/**
 * 
 */
package com.surelogic.annotation.rules;


/**
 * @author ethan
 *
 */
public class RegionRulesTestHelper extends RegionRules {
	private static final RegionRulesTestHelper instance = new RegionRulesTestHelper();
	
	public static RegionRulesTestHelper getInstance(){
		return instance;
	}
	
	public static class Aggregate_ParseRuleHelper extends Aggregate_ParseRule{
		public Aggregate_ParseRuleHelper(){
			super();
		}
	}

	public static class InRegion_ParseRuleHelper extends InRegion_ParseRule{
		
		public InRegion_ParseRuleHelper(){
			super();
		}
	}
	public static class MapFields_ParseRuleHelper extends MapFields_ParseRule{
		
		public MapFields_ParseRuleHelper(){
			super();
		}
	}
	public static class Region_ParseRuleHelper extends Region_ParseRule{
	  public Region_ParseRuleHelper() {
			super(null);
		}
	}
}
