package com.surelogic.test.scripting;

/**
 * A placeholder for future state that might be needed
 * 
 * @author Edwin
 */
public interface ICommandContext {
	Object getArgument(String key);
	
	static ICommandContext nullContext = new ICommandContext() {
		public Object getArgument(String key) {
			return null;
		}
	};
}
