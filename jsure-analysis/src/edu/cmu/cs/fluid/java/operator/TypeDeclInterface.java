// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/operator/TypeDeclInterface.java,v 1.5 2007/09/20 13:56:42 chance Exp $
package edu.cmu.cs.fluid.java.operator;

import com.surelogic.common.ref.IDecl;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.promise.IHasReceiverDecl;
 
public interface TypeDeclInterface extends ReferenceTypeInterface, IHasReceiverDecl { 
  IRNode get_Body(IRNode n);
  IDecl.Kind getKind();
}
