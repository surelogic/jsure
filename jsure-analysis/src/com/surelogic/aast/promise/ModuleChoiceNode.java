/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ModuleChoiceNode.java,v 1.1 2007/10/27 17:11:10 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.AASTRootNode;
import com.surelogic.aast.AbstractAASTNodeFactory;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeModifier;
import com.surelogic.aast.INodeVisitor;

public class ModuleChoiceNode extends AASTRootNode {
  final private ModuleWrapperNode modWrapper;
  final private ModulePromiseNode modPromise;
  final private ModuleScopeNode modScope;
  
  public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
      "ModuleChoice") {
    @Override
    public AASTNode create(String _token, int _start, int _stop, int _mods,
                           String _id, int _dims, List<AASTNode> _kids) {
      AASTNode kid = _kids.get(0);
      if (kid instanceof ModuleWrapperNode) {
        return new ModuleChoiceNode(_start, (ModuleWrapperNode) kid);
      } else if (kid instanceof ModuleScopeNode) {
    	  return new ModuleChoiceNode(_start, (ModuleScopeNode) kid);
      } else {
        return new ModuleChoiceNode(_start, (ModulePromiseNode) kid);
      }  
    }
  };
  
  public ModuleChoiceNode(int offset, ModuleWrapperNode mwn) {
    super(offset);
    modWrapper = mwn;
    modPromise = null;
    modScope = null;
  }

  public ModuleChoiceNode(int offset, ModulePromiseNode mpn) {
    super(offset);
    modWrapper = null;
    modPromise = mpn;
    modScope = null;
  }
  
  public ModuleChoiceNode(int offset, ModuleScopeNode msn) {
	  super(offset);
	  modWrapper = null;
	  modPromise = null;
	  modScope = msn;
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
    if (modWrapper != null) {
      ModuleWrapperNode modWrapperCopy = (ModuleWrapperNode) modWrapper.cloneOrModifyTree(mod);
      return new ModuleChoiceNode(getOffset(), modWrapperCopy);
    } else if (modScope != null) {
    	ModuleScopeNode modScopeCopy = (ModuleScopeNode) modScope.cloneOrModifyTree(mod);
    	return new ModuleChoiceNode(getOffset(), modScopeCopy);
    } else {
      ModulePromiseNode modPromiseCopy = (ModulePromiseNode) modPromise.cloneOrModifyTree(mod);
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
    } else if (modScope != null) {
    	sb.append(modScope.unparse(debug, indent+2));
    } else {
      sb.append(modPromise.unparse(debug, indent+2));
    }
    return sb.toString();
  }

  @Override
  public final String unparseForPromise() {
	  throw new UnsupportedOperationException();
  }  
  
  public ModuleWrapperNode getModWrapper() {
    return modWrapper;
  }

  public ModulePromiseNode getModPromise() {
    return modPromise;
  }

  public ModuleScopeNode getModScope() {
	  return modScope;
  }
  
}
