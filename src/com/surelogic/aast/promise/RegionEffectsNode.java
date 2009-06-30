/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/RegionEffectsNode.java,v 1.3 2007/10/22 18:55:23 ethan Exp $*/
package com.surelogic.aast.promise;

import java.util.*;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

/**
 * TODO Fill in purpose.
 * @author ethan
 */
public class RegionEffectsNode extends AASTRootNode {
	private List<EffectsSpecificationNode> effects;
	
  public static final AbstractSingleNodeFactory factory =
  	new AbstractSingleNodeFactory("RegionEffects"){
  	
  	@Override
  	@SuppressWarnings("unchecked")
  	public AASTNode create(String _token, int _start, int _stop, int _mods, String _id, int _dims, List<AASTNode> _kids){
  		List<EffectsSpecificationNode> effects = new ArrayList<EffectsSpecificationNode>(_kids.size());
  		for(AASTNode effect : _kids){
  			effects.add((EffectsSpecificationNode)effect);
  		}
  		return new RegionEffectsNode(_start, effects);
  	}
  };
  
  /**
   * Constructor
   * @param offset The offset of this AAST in the application
   * @param effects A list of {@link EffectsSpecificationNodes}, currently only {@link ReadsNodes} and {@link WriteNodes}
   */
  public RegionEffectsNode(int offset, List<EffectsSpecificationNode> effects){
  	super(offset);
  	if(effects == null){
  		throw new IllegalArgumentException("Effects is null");
  	}
  	for(EffectsSpecificationNode effect : effects){
  		((AASTNode)effect).setParent(this);
  	}
  	this.effects = Collections.unmodifiableList(effects);
  }
  
  /**
   * Creates a copy of this AAST and all sub-AASTs
   */
  @Override
  public IAASTNode cloneTree(){
  	List<EffectsSpecificationNode> effectCopy = new ArrayList<EffectsSpecificationNode>(effects.size());
  	for (EffectsSpecificationNode effectSpecificationNode : effects) {
			effectCopy.add((EffectsSpecificationNode)effectSpecificationNode.cloneTree());
		}
  	return new RegionEffectsNode(getOffset(), effectCopy);
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
    String name = this.getClass().getSimpleName();
    name = name.substring(0, name.length()-4);
    
    StringBuilder sb = new StringBuilder();
    if (debug) {
      indent(sb, indent); 
    }
    sb.append(name);
    if (debug) {
      sb.append('\n');
      for(AASTNode _n : getEffectsList()) {
        sb.append(_n.unparse(debug, indent+2));
      }    
    } else {
      sb.append(' ');
      if (getEffectsList().isEmpty()) {
        sb.append("nothing");
      }
      
      boolean first = true;
      for(AASTNode _n : getEffectsList()) {
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
	 * Returns the list of {@link EffectsSpecificationNode}. This list should never
	 * exceed the number of possible effects. Currently, this is 2, for {@link ReadsNode}
	 * and {@link WritesNodes}.
	 * @return A List of {@link EffectsSpecificationNode}
	 */
	public List<EffectsSpecificationNode> getEffectsList(){
		return effects;
	}
}
