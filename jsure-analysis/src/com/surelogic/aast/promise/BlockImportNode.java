/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/BlockImportNode.java,v 1.1 2007/10/27 17:11:10 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.ArrayList;
import java.util.List;

import com.surelogic.aast.*;

public class BlockImportNode extends ModuleAnnotationNode {
  private final OfNamesClauseNode ofNamesClause;
  private final List<String> fromModuleNames;
  
  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("BlockImport") {
      @SuppressWarnings("unchecked")
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
//        String id = _id;
        List<String> names;
        OfNamesClauseNode ofNames;
        if (_kids.get(0) instanceof OfNamesClauseNode) {
          ofNames = (OfNamesClauseNode) _kids.get(0);
          names = (List<String>) _kids.get(1);
        } else {
          names = (List<String>) _kids.get(0);
          ofNames = null;
        }
        return new BlockImportNode (_start,ofNames, names);
      }
    };
    
    BlockImportNode(int offset, OfNamesClauseNode ofNamesCl, List<String> fromModNames) {
      super(offset);
      ofNamesClause = ofNamesCl;
      fromModuleNames = fromModNames;
    }
    
    

    /* (non-Javadoc)
     * @see com.surelogic.aast.AASTRootNode#cloneTree()
     */
    @Override
    protected IAASTNode internalClone(final INodeModifier mod) {
      List<String> fromModuleNamesCopy = new ArrayList<String>(fromModuleNames.size());
      fromModuleNamesCopy.addAll(fromModuleNames);
      OfNamesClauseNode ofNamesCopy = 
        ofNamesClause == null ? null : (OfNamesClauseNode) ofNamesClause.cloneOrModifyTree(mod);
      
      return new BlockImportNode(getOffset(), ofNamesCopy, fromModuleNamesCopy );
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
      sb.append("BlockImport ");
      if (ofNamesClause != null) {
        sb.append(ofNamesClause.unparse(debug, indent+2));
      }
      sb.append(" from [");
      for (String fromModuleName : fromModuleNames) {
        if (debug) { indent(sb, indent+2); }
        sb.append("\n");
        sb.append(fromModuleName);
      }
      if (debug) { indent(sb, indent); }
      sb.append("]\n");
      return sb.toString();
    }



    /**
     * @return Returns the ofNamesClause.
     */
    public OfNamesClauseNode getOfNamesClause() {
      return ofNamesClause;
    }

    /**
     * @return Returns the fromModuleNames.
     */
    public List<String> getFromModuleNames() {
      return fromModuleNames;
    }
    
    
}
