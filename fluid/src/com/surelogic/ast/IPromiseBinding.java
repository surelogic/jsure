// $Header$
package com.surelogic.ast;

import com.surelogic.dropsea.ir.IRReferenceDrop;

public interface IPromiseBinding extends IBinding {
//  IPromiseDeclarationNode getNode();
  IRReferenceDrop getDrop();
}
