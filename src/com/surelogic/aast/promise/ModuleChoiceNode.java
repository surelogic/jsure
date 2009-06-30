/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ModuleChoiceNode.java,v 1.1 2007/10/27 17:11:10 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

public class ModuleChoiceNode extends AASTRootNode {
  final private ModuleWrapperNode modWrapper;
  final private ModulePromiseNode modPromise;
  
  public static final AbstractSingleNodeFactory factory = new AbstractSingleNodeFactory(
      "ModuleChoice") {
    @Override
    @SuppressWarnings("unchecked")
    public AASTNode create(String _token, int _start, int _stop, int _mods,
                           String _id, int _dims, List<AASTNode> _kids) {
      AASTNode kid = _kids.get(0);
      if (kid instanceof ModuleWrapperNode) {
        return new ModuleChoiceNode(_start, (ModuleWrapperNode) kid);
      } else {
        return new ModuleChoiceNode(_start, (ModulePromiseNode) kid);
      }  
    }
  };
  
  public ModuleChoiceNode(int offset, ModuleWrapperNode mwn) {
    super(offset);
    modWrapper = mwn;
    modPromise = null;
  }

  public ModuleChoiceNode(int offset, ModulePromiseNode mpn) {
    super(offset);
    modWrapper = null;
    modPromise = mpn;
  }
  
  @Override
  public IAASTNode cloneTree() {
    if (modWrapper != null) {
      ModuleWrapperNode modWrapperCopy = (ModuleWrapperNode) modWrapper.cloneTree();
      return new ModuleChoiceNode(getOffset(), modWrapperCopy);
    } else {
      ModulePromiseNode modPromiseCopy = (ModulePromiseNode) modPromise.cloneTree();
      return new ModuleChoiceNode(getOffset(), modPromiseCopy);
    }
   }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("ModuleChoice\n");
    if (modWrapper != null) {
      sb.append(modWrapper.unparse(debug, indent+2));
    } else {
      sb.append(modPromise.unparse(debug, indent+2));
    }
    return sb.toString();
  }

  public ModuleWrapperNode getModWrapper() {
    return modWrapper;
  }

  public ModulePromiseNode getModPromise() {
    return modPromise;
  }

  
}
