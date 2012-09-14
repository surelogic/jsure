package com.surelogic.jsure.core.scripting;

/**
 * A placeholder for future state that might be needed
 * 
 * @author Edwin
 */
public interface ICommandContext {
	Object getArgument(String key);
	
	static ICommandContext nullContext = new ICommandContext() {
		@Override
		public Object getArgument(String key) {
			return null;
		}
	};
}
