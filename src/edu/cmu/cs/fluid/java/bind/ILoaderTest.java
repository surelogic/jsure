package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * 
 */
public interface ILoaderTest {
	/**
	 * @param qname The name of a type or class
	 */
	void load(String qname);
	
	/**
	 * @param name The name of a type or class
	 * @return The IR corresponding to the name
	 */
  IRNode findAST(String name);
  
  IBinder getBinder();
}
