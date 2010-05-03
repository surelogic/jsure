
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.AASTBinder;
import com.surelogic.aast.bind.ILockBinding;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class SimpleLockNameNode extends LockNameNode { 
  // Fields

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("SimpleLockName") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        String id = _id;
        return new SimpleLockNameNode (_start,
          id        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public SimpleLockNameNode(int offset,
                            String id) {
    super(offset, id);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    if (!debug) {
      return getId();
    }
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("SimpleLockName\n");
    indent(sb, indent+2);
    sb.append("id=").append(getId());
    sb.append("\n");
    return sb.toString();
  }
  
  public boolean bindingExists() {
    return AASTBinder.getInstance().isResolvable(this);
  }

  public ILockBinding resolveBinding() {
    return AASTBinder.getInstance().resolve(this);
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new SimpleLockNameNode(getOffset(), new String(getId()));
  }
}

