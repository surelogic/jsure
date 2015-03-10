/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/OfNamesClauseNode.java,v 1.1 2007/10/27 17:11:10 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.Collections;
import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class OfNamesClauseNode extends ModuleAnnotationNode {
  final List<String> exportNames;

  public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
      "OfNamesClause") {
    @Override
    public AASTNode create(String _token, int _start, int _stop, int _mods,
                           String _id, int _dims, List<AASTNode> _kids) {
      @SuppressWarnings({ "unchecked", "rawtypes" })
      List<String> tlist = (List) _kids;
      List<String> names = Collections.unmodifiableList(tlist);
      

      return new OfNamesClauseNode(_start, names);
    }
  };

  OfNamesClauseNode(int offset, List<String> expNames) {
    super(offset);
    exportNames = expNames;
  }

  @Override
  public IAASTNode cloneTree() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    // TODO Auto-generated method stub
    return null;
  }

}
