/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ReadsNode.java,v 1.3 2007/09/24 21:09:55 ethan Exp $*/
package com.surelogic.aast.promise;

import java.util.ArrayList;
import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.parse.AbstractSingleNodeFactory;

public class ReadsNode extends EffectsSpecificationNode {
	public static final AbstractSingleNodeFactory factory =
		new AbstractSingleNodeFactory("Reads"){
		
		@Override
		@SuppressWarnings("unchecked")
		public AASTNode create(String _token, int _start, int _stop,
			int _mods, String _id, int _dims, List<AASTNode> _kids){
			String id = _id;
			List<EffectSpecificationNode> effects = new ArrayList<EffectSpecificationNode>(_kids.size());
			for (AASTNode effect : _kids) {
				effects.add((EffectSpecificationNode)effect);
			}
			
			return new ReadsNode (_start, effects);
		}
		
	};
  public ReadsNode(int offset, List<EffectSpecificationNode> effect) {
    super(offset, effect);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	List<EffectSpecificationNode> effectsCopy = new ArrayList<EffectSpecificationNode>(getEffectList().size());
  	for (EffectSpecificationNode effectSpecificationNode : getEffectList()) {
			effectsCopy.add((EffectSpecificationNode)effectSpecificationNode.cloneTree());
		}
  	return new ReadsNode(getOffset(), effectsCopy);
  }
}
