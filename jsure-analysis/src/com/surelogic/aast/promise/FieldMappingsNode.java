
package com.surelogic.aast.promise;


import java.util.*;

import com.surelogic.aast.*;
import com.surelogic.annotation.rules.RegionRules;
import com.surelogic.parse.TempListNode;

public class FieldMappingsNode extends AASTRootNode { 
  // Fields
  private final List<RegionSpecificationNode> fields;
  private final RegionSpecificationNode to;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("FieldMappings") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        @SuppressWarnings("unchecked")
        List<RegionSpecificationNode> fields =  ((TempListNode) _kids.get(0)).toList();
        RegionSpecificationNode to =  (RegionSpecificationNode) _kids.get(1);
        return new FieldMappingsNode (_start,
          fields,
          to        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public FieldMappingsNode(int offset,
                           List<RegionSpecificationNode> fields,
                           RegionSpecificationNode to) {
    super(offset);
    if (fields == null) { throw new IllegalArgumentException("fields is null"); }
    for (RegionSpecificationNode _c : fields) {
      ((AASTNode) _c).setParent(this);
    }
    this.fields = Collections.unmodifiableList(fields);
    if (to == null) { throw new IllegalArgumentException("to is null"); }
    ((AASTNode) to).setParent(this);
    this.to = to;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
    	indent(sb, indent); 
        sb.append("FieldMappings\n");
        for(AASTNode _n : getFieldsList()) {
        	sb.append(_n.unparse(debug, indent+2));
        }
        sb.append(getTo().unparse(debug, indent+2));
    } else {
    	boolean first = true;
        for(AASTNode _n : getFieldsList()) {
        	sb.append(_n.unparse(debug, indent));
        	if (first) {
        		first = false;
        	} else {
        		sb.append(", ");
        	}
        }
        sb.append(" into ");
        sb.append(getTo().unparse(debug, indent));
    }
    return sb.toString();
  }

  @Override
  public String unparseForPromise() {
	  // TODO check why the code above doesn't do this
	  return RegionRules.MAP_FIELDS+"(\""+unparse(false)+"\")";
  }
  
  /**
   * @return A non-null, but possibly empty list of nodes
   */
  public List<RegionSpecificationNode> getFieldsList() {
    return fields;
  }
  /**
   * @return A non-null node
   */
  public RegionSpecificationNode getTo() {
    return to;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	List<RegionSpecificationNode> fieldsCopy = new ArrayList<RegionSpecificationNode>(fields.size());
  	for (RegionSpecificationNode regionSpecificationNode : fields) {
			fieldsCopy.add((RegionSpecificationNode)regionSpecificationNode.cloneOrModifyTree(mod));
		}
  	return new FieldMappingsNode(getOffset(), fieldsCopy, (RegionSpecificationNode)getTo().cloneOrModifyTree(mod));
  }
}

