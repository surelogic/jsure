/*
 * $Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/WritesNode.java,v
 * 1.1 2007/06/27 16:27:17 chance Exp $
 */
package com.surelogic.aast.promise;

import java.util.ArrayList;
import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.parse.AbstractSingleNodeFactory;

public class WritesNode extends EffectsSpecificationNode {
	public static final AbstractSingleNodeFactory factory =
		new AbstractSingleNodeFactory("Writes") {

			@Override
			@SuppressWarnings("unchecked")
			public AASTNode create(String _token, int _start, int _stop,
				int _mods, String _id, int _dims, List<AASTNode> _kids) {
				String id = _id;
				List<EffectSpecificationNode> effects =
					new ArrayList<EffectSpecificationNode>(_kids.size());
				for (AASTNode effect : _kids) {
					effects.add((EffectSpecificationNode) effect);
				}

				return new WritesNode(_start, effects);
			}

		};

	public WritesNode(int offset, List<EffectSpecificationNode> effect) {
		super(offset, effect);
	}
	
  @Override
  public IAASTNode cloneTree(){
  	List<EffectSpecificationNode> effectCopy = new ArrayList<EffectSpecificationNode>(getEffectList().size());
  	for (EffectSpecificationNode effectSpecificationNode : getEffectList()) {
			effectCopy.add((EffectSpecificationNode)effectSpecificationNode.cloneTree());
		}
  	return new WritesNode(getOffset(), effectCopy);
  }
}
