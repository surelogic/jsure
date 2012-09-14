// $Header$
package com.surelogic.ast;

import com.surelogic.dropsea.ir.IRReferenceDrop;

import edu.cmu.cs.fluid.sea.*;

public interface IPromiseBinding extends IBinding {
//  IPromiseDeclarationNode getNode();
  IRReferenceDrop getDrop();
}
