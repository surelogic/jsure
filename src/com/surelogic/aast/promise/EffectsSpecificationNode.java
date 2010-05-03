
package com.surelogic.aast.promise;


import java.util.*;



import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class EffectsSpecificationNode extends AASTRootNode 
{  
  // Fields
  private final List<EffectSpecificationNode> effect;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("EffectsSpecification") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        @SuppressWarnings("unchecked")
        List<EffectSpecificationNode> effect = ((List) _kids);
        return new EffectsSpecificationNode (_start,
          effect        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public EffectsSpecificationNode(int offset,
                                  List<EffectSpecificationNode> effect) {
    super(offset);
    if (effect == null) { throw new IllegalArgumentException("effect is null"); }
    for (EffectSpecificationNode _c : effect) {
      ((AASTNode) _c).setParent(this);
    }
    this.effect = Collections.unmodifiableList(effect);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    String name = this.getClass().getSimpleName();
    name = name.substring(0, name.length()-4);
    
    StringBuilder sb = new StringBuilder();
    if (debug) {
      indent(sb, indent); 
    }
    sb.append(name);
    if (debug) {
      sb.append('\n');
      for(AASTNode _n : getEffectList()) {
        sb.append(_n.unparse(debug, indent+2));
      }    
    } else {
      sb.append(' ');
      if (getEffectList().isEmpty()) {
        sb.append("nothing");
      }
      
      boolean first = true;
      for(AASTNode _n : getEffectList()) {
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
  public List<EffectSpecificationNode> getEffectList() {
    return effect;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	List<EffectSpecificationNode> effectsCopy = new ArrayList<EffectSpecificationNode>(effect.size());
  	for (EffectSpecificationNode effectSpecificationNode : effect) {
			effectsCopy.add((EffectSpecificationNode)effectSpecificationNode.cloneTree());
		}
  	return new EffectsSpecificationNode(getOffset(), effectsCopy);
  }
}

