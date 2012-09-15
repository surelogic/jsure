
package com.surelogic.aast.promise;


import java.util.*;



import com.surelogic.aast.*;
import com.surelogic.parse.TempListNode;

public class EnclosingModuleNode extends ModuleNode { 
  // Fields
  private final List<ModuleNode> modules;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("EnclosingModule") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        String id = _id;
        @SuppressWarnings("unchecked")
        List<ModuleNode> modules =  ((TempListNode) _kids.get(0)).toList();
        return new EnclosingModuleNode (_start,
          id,
          modules        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public EnclosingModuleNode(int offset,
                             String id,
                             List<ModuleNode> modules) {
    super(offset, id);
    if (modules == null) { throw new IllegalArgumentException("modules is null"); }
    for (ModuleNode _c : modules) {
      ((AASTNode) _c).setParent(this);
    }
    this.modules = Collections.unmodifiableList(modules);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("EnclosingModule\n");
    indent(sb, indent+2);
    sb.append("id=").append(getId());
    sb.append("\n");
    for(AASTNode _n : getModulesList()) {
      sb.append(_n.unparse(debug, indent+2));
    }
    return sb.toString();
  }

  /**
   * @return A non-null, but possibly empty list of nodes
   */
  public List<ModuleNode> getModulesList() {
    return modules;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	List<ModuleNode> modulesCopy = new ArrayList<ModuleNode>(modules.size());
  	for (ModuleNode moduleNode : modules) {
			modules.add((ModuleNode)moduleNode.cloneTree());
		}
  	
  	return new EnclosingModuleNode(getOffset(), new String(getId()), modulesCopy);
  }
}

