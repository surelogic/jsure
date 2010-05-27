/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ModulePromiseNode.java,v 1.2 2007/10/28 18:17:07 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.Collections;
import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ModulePromiseNode extends ModuleAnnotationNode  {
  // Fields
  private final String id;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ModulePromise") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        String id = _id;
        return new ModulePromiseNode (_start,
          id        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ModulePromiseNode(int offset,
                    String id) {
    super(offset);
    if (id == null) { throw new IllegalArgumentException("id is null"); }
    this.id = id;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("Module\n");
    indent(sb, indent+2);
    sb.append("id=").append(id);
    sb.append("\n");
    return sb.toString();
  }

  /**
   * @return A non-null String
   */
  public String getModuleName() {
    return id;
  }
  
  
  public ModulePromiseNode getAnno() {
    return this;
  }

  public List<String> getWrappedModuleName() {
    return Collections.emptyList();
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
    return new ModulePromiseNode(getOffset(), new String(id));
  }
}
