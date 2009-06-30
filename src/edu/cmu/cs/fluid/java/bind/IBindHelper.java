package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Interface for a class that helps to bind names within a given type
 * (esp. for imports, ...)
 */
public interface IBindHelper {
	/** 
	 * @param type An AST for the name of a type
	 * @return The declaration for that type
	 */
	IRNode getNamedTypeBinding(IRNode type);
	
	/** 
	 * @param type A name to be resolved
	 * @return A IBindHelper specialized to the imports (if any) in the code
	 */
  IBindHelper getHelper(IRNode type);

	/** 
	 */
  IRNode findFieldInBody(IRNode body, String name);
}
