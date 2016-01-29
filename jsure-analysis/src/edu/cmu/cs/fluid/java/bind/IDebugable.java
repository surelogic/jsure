package edu.cmu.cs.fluid.java.bind;

/**
 * A interface for providing useful info in the debugger
 * 
 * @author edwin
 */
public interface IDebugable {
	/**
	 * Get an unparse similar to what would appear in source code
	 * (e.g., relative names omitting package names)
	 */
	public String toSourceText();

	/**
	 * Get a unparse that uses fully qualified type names
	 */
	public String toFullyQualifiedText();	
}
