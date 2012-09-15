package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.analysis.threadroles.TRExpr;
import com.surelogic.analysis.threadroles.TRLeafExpr;
import com.surelogic.aast.AbstractAASTNodeFactory;
import com.surelogic.dropsea.ir.drops.threadroles.TRoleNameModel;

import edu.cmu.cs.fluid.ir.IRNode;



public class ThreadRoleNameNode extends AASTNode implements TRoleLit, ThreadRoleOrElem {
  // Fields
  private final String id;

  public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
      "ThreadRoleName") {
    @Override
    public AASTNode create(String _token, int _start, int _stop, int _mods,
        String _id, int _dims, List<AASTNode> _kids) {
      String id = _id;
      return new ThreadRoleNameNode(_start, id);
    }
  };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ThreadRoleNameNode(int offset, String id) {
    super(offset);
    if (id == null) {
      throw new IllegalArgumentException("id is null");
    }
    this.id = id;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    if (debug) {
      StringBuilder sb = new StringBuilder();
      indent(sb, indent);    
      sb.append("ThreadRoleName\n");
      indent(sb, indent + 2);
      sb.append("id=").append(getId());
      sb.append("\n");
      return sb.toString();
    }
    return getId();
  }

  /**
   * @return A non-null String
   */
  public String getId() {
    return id;
  }


  public TRExpr buildTRExpr(IRNode where) {
    TRExpr res = new TRLeafExpr(TRoleNameModel.getInstance(id, where));
    return res;
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {

    return visitor.visit(this);
  }

  /* (non-Javadoc)
   * @see com.surelogic.aast.AASTNode#cloneTree()
   */
  @Override
  public IAASTNode cloneTree() {
    return new ThreadRoleNameNode(getOffset(), new String(getId()));
  }
}
