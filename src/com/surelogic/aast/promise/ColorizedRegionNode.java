
package com.surelogic.aast.promise;


import java.util.*;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;
import com.surelogic.parse.TempListNode;

public class ColorizedRegionNode extends DataColoringAnnotationNode { 
  // Fields
  private final List<RegionSpecificationNode> cRegions;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ColorizedRegion") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        List<RegionSpecificationNode> cRegions =  ((TempListNode) _kids.get(0)).toList();
        return new ColorizedRegionNode (_start,
          cRegions        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ColorizedRegionNode(int offset,
                             List<RegionSpecificationNode> cRegions) {
    super(offset);
    if (cRegions == null) { throw new IllegalArgumentException("cRegions is null"); }
    for (RegionSpecificationNode _c : cRegions) {
      ((AASTNode) _c).setParent(this);
    }
    this.cRegions = Collections.unmodifiableList(cRegions);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("ColorizedRegion\n");
    for(AASTNode _n : getCRegionsList()) {
      sb.append(_n.unparse(debug, indent+2));
    }
    return sb.toString();
  }

  /**
   * @return A non-null, but possibly empty list of nodes
   */
  public List<RegionSpecificationNode> getCRegionsList() {
    return cRegions;
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
		List<RegionSpecificationNode> cRegionsCopy = new ArrayList<RegionSpecificationNode>(cRegions.size());
		for (RegionSpecificationNode regionSpecificationNode : cRegions) {
			cRegionsCopy.add((RegionSpecificationNode)regionSpecificationNode.cloneTree());
		}	
		return new ColorizedRegionNode(getOffset(), cRegionsCopy);
	}
}

