
package com.surelogic.aast.promise;


import java.util.*;

import com.surelogic.aast.*;
import com.surelogic.annotation.rules.AnnotationRules.ParameterMap;

public class EffectsSpecificationNode extends AASTNode 
{  
  // Fields
  private final List<EffectSpecificationNode> effect;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("EffectsSpecification") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
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
    name = name.substring(0, name.length()-4);//.toLowerCase();
    
    StringBuilder sb = new StringBuilder();
    if (debug) {
      indent(sb, indent); 
    }
    sb.append(name);
    if (debug) {
      sb.append('\n');
      for(EffectSpecificationNode _n : getEffectList()) {
        sb.append(_n.unparse(debug, indent+2));
      }    
    } else {
      sb.append(' ');
      if (getEffectList().isEmpty()) {
        sb.append("nothing");
      }
      
      boolean first = true;
      for(final EffectSpecificationNode _n : getEffectList()) {
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
  protected IAASTNode internalClone(final INodeModifier mod) {
  	final List<EffectSpecificationNode> effectsCopy = new ArrayList<EffectSpecificationNode>(effect.size());
  	for (EffectSpecificationNode effectSpecificationNode : effect) {
			effectsCopy.add((EffectSpecificationNode)effectSpecificationNode.cloneOrModifyTree(mod));
		}
  	return new EffectsSpecificationNode(getOffset(), effectsCopy);
  }
  
  public final EffectsSpecificationNode cloneForProposal(final ParameterMap pm) {
    final List<EffectSpecificationNode> effectsCopy = new ArrayList<EffectSpecificationNode>(effect.size());
    for (EffectSpecificationNode effectSpecificationNode : effect) {
      effectsCopy.add(effectSpecificationNode.cloneForProposal(pm));
    }
    return finishCloneForProposal(effectsCopy);
  }
  
  EffectsSpecificationNode finishCloneForProposal(final List<EffectSpecificationNode> effects) {
    return new EffectsSpecificationNode(getOffset(), effects);
  }
  
  public final String unparseForPromise() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getLabel());
    sb.append(' ');
    if (effect.isEmpty()) {
      sb.append("nothing");
    } else {
      boolean first = true;
      for(final EffectSpecificationNode _n : getEffectList()) {
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
  
  protected String getLabel() {
    return "???";
  }
}

