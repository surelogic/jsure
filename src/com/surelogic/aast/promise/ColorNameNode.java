package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.analysis.colors.CExpr;
import com.surelogic.analysis.colors.CLeafExpr;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.colors.ColorNameModel;



public class ColorNameNode extends AASTNode implements ColorLit, ColorOrElem {
  // Fields
  private final String id;

  public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
      "ColorName") {
    @Override
    public AASTNode create(String _token, int _start, int _stop, int _mods,
        String _id, int _dims, List<AASTNode> _kids) {
      String id = _id;
      return new ColorNameNode(_start, id);
    }
  };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ColorNameNode(int offset, String id) {
    super(offset);
    if (id == null) {
      throw new IllegalArgumentException("id is null");
    }
    this.id = id;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) {
      indent(sb, indent);
    }
    sb.append("ColorName\n");
    indent(sb, indent + 2);
    sb.append("id=").append(getId());
    sb.append("\n");
    return sb.toString();
  }

  /**
   * @return A non-null String
   */
  public String getId() {
    return id;
  }


  public CExpr buildCExpr(IRNode where) {
    CExpr res = new CLeafExpr(ColorNameModel.getInstance(id, where));
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
    return new ColorNameNode(getOffset(), new String(getId()));
  }
}
