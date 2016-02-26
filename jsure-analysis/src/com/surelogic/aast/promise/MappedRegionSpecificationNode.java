
package com.surelogic.aast.promise;


import java.util.*;

import com.surelogic.aast.*;

public class MappedRegionSpecificationNode extends FieldRegionSpecificationNode { 
  // Fields
  private final List<RegionMappingNode> mapping;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("MappedRegionSpecification") {
      @SuppressWarnings({ "rawtypes", "unchecked" })
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        List<RegionMappingNode> mapping = ((List) _kids);
        return new MappedRegionSpecificationNode (_start,
          mapping        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public MappedRegionSpecificationNode(int offset,
                                       List<RegionMappingNode> mapping) {
    super(offset);
    if (mapping == null) { throw new IllegalArgumentException("mapping is null"); }
    for (RegionMappingNode _c : mapping) {
      ((AASTNode) _c).setParent(this);
    }
    this.mapping = Collections.unmodifiableList(mapping);
  }
  
  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) {
      if (debug) { indent(sb, indent); }
      sb.append("MappedRegionSpecification\n");
      for(AASTNode _n : getMappingList()) {
        sb.append(_n.unparse(debug, indent+2));
      }
    } else {
      boolean first = true;
      for(RegionMappingNode _n : getMappingList()) {
        if (first) {
          first = false;
        } else {
          sb.append(", ");
        }
        sb.append(_n.unparse(false));
      }
    }
    return sb.toString();
  }

  /**
   * @return A non-null, but possibly empty list of nodes
   */
  public List<RegionMappingNode> getMappingList() {
    return mapping;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	List<RegionMappingNode> mappingCopy = new ArrayList<RegionMappingNode>(mapping.size());
  	for (RegionMappingNode regionMappingNode : mapping) {
			mappingCopy.add((RegionMappingNode)regionMappingNode.cloneOrModifyTree(mod));
		}
  	return new MappedRegionSpecificationNode(getOffset(), mappingCopy);
  }
}

