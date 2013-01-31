// $Header$	
package edu.cmu.cs.fluid.java.operator;

import edu.cmu.cs.fluid.ir.IRNode;
 
/**
 * Marks NestedClass/InterfaceDeclarations
 * 
 * @author Edwin.Chan
 */
public interface NestedTypeDeclInterface extends ClassBodyDeclInterface, TypeDeclInterface, NestedDeclInterface { 
  IRNode get_Annos(IRNode n);
  IRNode get_Types(IRNode n);
  @Override
  IRNode get_Body(IRNode n);    
}
