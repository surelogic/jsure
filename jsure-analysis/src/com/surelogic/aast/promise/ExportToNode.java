/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ExportToNode.java,v 1.1 2007/10/27 17:11:10 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeModifier;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ExportToNode extends ModuleAnnotationNode {

  final private List<String> exportNames;
  final private List<String> toModuleNames;
  public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
      "ExportTo") {
    @Override
    public AASTNode create(String _token, int _start, int _stop, int _mods,
                           String _id, int _dims, List<AASTNode> _kids) {
      
      // ExportTo has two children, both List<String>
      @SuppressWarnings("unchecked")
      List<String> names = (List<String>) _kids.get(0);
      @SuppressWarnings("unchecked")
      List<String> toNames = (List<String>) _kids.get(1);

      return new ExportToNode(_start, names, toNames);
    }
  };
  
  public ExportToNode (int offset, List<String> names, List<String> toNames) {
    super(offset);
    exportNames = Collections.unmodifiableList(names);
    toModuleNames = Collections.unmodifiableList(toNames);
  }

  
  /**
   * @return Returns the exportNames.
   */
  public List<String> getExportNames() {
    return exportNames;
  }


  /**
   * @return Returns the toModuleName.
   */
  public List<String> getToModuleName() {
    return toModuleNames;
  }


  /* (non-Javadoc)
   * @see com.surelogic.aast.AASTRootNode#cloneTree()
   */
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
    List<String> exportNamesCopy = new ArrayList<String>(exportNames.size());
    List<String> toModuleNamesCopy = new ArrayList<String>(toModuleNames.size());
    exportNamesCopy.addAll(exportNames);
    toModuleNamesCopy.addAll(toModuleNames);
    return new ExportToNode(getOffset(), exportNamesCopy, toModuleNames);
  }

  /* (non-Javadoc)
   * @see com.surelogic.aast.AASTNode#accept(com.surelogic.aast.INodeVisitor)
   */
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    // TODO Auto-generated method stub
    return visitor.visit(this);
  }

  /* (non-Javadoc)
   * @see com.surelogic.aast.AASTNode#unparse(boolean, int)
   */
  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("ExportTo\n");
    indent(sb, indent+2);
    sb.append("exportNames=[");
    boolean first = true;
    for (String s: exportNames) {
      if (!first) { sb.append(','); first = false; }
      sb.append(s);
    }
    sb.append("]\n");
    indent(sb,indent+2);
    sb.append("toModuleNames=[");
    first=true;
    for (String s: exportNames) {
      if (!first) { sb.append(','); first = false; }
      sb.append(s);
    }
    sb.append("]\n");
    return sb.toString();
  }
  
  
}
