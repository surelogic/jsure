
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.java.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public final class MethodGuardedByNode extends GuardedByNode 
{ 
  // Fields
  public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
  "GuardedBy") {
	  @Override
	  public AASTNode create(String _token, int _start, int _stop, int _mods,
			  String _id, int _dims, List<AASTNode> _kids) {
		  ExpressionNode field = (ExpressionNode) _kids.get(0);
		  return new MethodGuardedByNode(_start, field);
	  }
  };
  
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public MethodGuardedByNode(int offset, ExpressionNode lock) {
    super(offset, lock);
  }

  @Override
  public IAASTNode cloneTree() {
	  return new MethodGuardedByNode(offset, (ExpressionNode) getLock().cloneTree());
  }
}

