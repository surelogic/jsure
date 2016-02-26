/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ModuleWrapperNode.java,v 1.1 2007/10/27 17:11:10 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.*;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeModifier;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ModuleWrapperNode extends ModuleAnnotationNode {
  private final String outerModuleName;
  private final List<String> innerModuleNames;
  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ModuleWrapper") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        String id = _id;
        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<String> names = ((List) _kids);
        return new ModuleWrapperNode (_start, id,
          names        );
      }
    };
    
    protected ModuleWrapperNode(int offset, String modName, List<String> names) {
      super(offset);
      if (names==null) {
        throw new IllegalArgumentException("names is null");
      }
      
      outerModuleName = modName;
      innerModuleNames = Collections.unmodifiableList(names);
    }

    public String getModuleName() {
      return outerModuleName;
    }

    public List<String> getWrappedModuleNames() {
      return innerModuleNames;
    }

    public ModuleWrapperNode getAnno() {
      return this;
    }

    /* (non-Javadoc)
     * @see com.surelogic.aast.AASTRootNode#cloneTree()
     */
    @Override
    protected IAASTNode internalClone(final INodeModifier mod) {
      return new ModuleWrapperNode(getOffset(), outerModuleName, innerModuleNames);
    }

    /* (non-Javadoc)
     * @see com.surelogic.aast.AASTNode#accept(com.surelogic.aast.INodeVisitor)
     */
    @Override
    public <T> T accept(INodeVisitor<T> visitor) {
      return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see com.surelogic.aast.AASTNode#unparse(boolean, int)
     */
    @Override
    public String unparse(boolean debug, int indent) {
      StringBuilder sb = new StringBuilder();
      if (debug) { indent(sb, indent); }
      sb.append("Module ");
      sb.append(outerModuleName);
      sb.append(" contains ");
      sb.append(innerModuleNames.toString());
      return sb.toString();
    }
    
    
}
