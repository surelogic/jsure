/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ExportNode.java,v 1.1 2007/10/27 17:11:10 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeModifier;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ExportNode extends ModuleAnnotationNode {

  final private List<String> exportNames;
  final private String toModuleName;
  public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
      "OfNamesClause") {
    @Override
    public AASTNode create(String _token, int _start, int _stop, int _mods,
                           String _id, int _dims, List<AASTNode> _kids) {
      
      // Export has two children: first a List<String>, second a String
      @SuppressWarnings("unchecked")
      List<String> names = (List<String>) _kids.get(0);

      return new ExportNode(_start, names, _id);
    }
  };
  
  public ExportNode (int offset, List<String> names, String id) {
    super(offset);
    exportNames = Collections.unmodifiableList(names);
    toModuleName = id;
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
  public String getToModuleName() {
    return toModuleName;
  }


  /* (non-Javadoc)
   * @see com.surelogic.aast.AASTRootNode#cloneTree()
   */
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
    List<String> exportNamesCopy = new ArrayList<String>(exportNames.size());
    exportNamesCopy.addAll(exportNames);
    return new ExportNode(getOffset(), exportNamesCopy, toModuleName);
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
    sb.append("Export\n");
    indent(sb, indent+2);
    sb.append("exportNames=[");
    boolean first = true;
    for (String s: exportNames) {
      if (!first) { sb.append(','); }
      sb.append(s);
    }
    sb.append("]\n");
    indent(sb,indent+2);
    sb.append("to=").append(toModuleName);
    sb.append('\n');
    return sb.toString();
  }
  
  
}
