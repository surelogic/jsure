/*
 * $Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/WritesNode.java,v
 * 1.1 2007/06/27 16:27:17 chance Exp $
 */
package com.surelogic.aast.promise;

import java.util.ArrayList;
import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.AbstractAASTNodeFactory;
import com.surelogic.aast.INodeModifier;

public class WritesNode extends EffectsSpecificationNode {
	public static final AbstractAASTNodeFactory factory =
		new AbstractAASTNodeFactory("Writes") {

			@Override
			public AASTNode create(String _token, int _start, int _stop,
				int _mods, String _id, int _dims, List<AASTNode> _kids) {
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
  protected IAASTNode internalClone(final INodeModifier mod) {
  	List<EffectSpecificationNode> effectCopy = new ArrayList<EffectSpecificationNode>(getEffectList().size());
  	for (EffectSpecificationNode effectSpecificationNode : getEffectList()) {
			effectCopy.add((EffectSpecificationNode)effectSpecificationNode.cloneOrModifyTree(mod));
		}
  	return new WritesNode(getOffset(), effectCopy);
  }
  
  @Override
  WritesNode finishCloneForProposal(final List<EffectSpecificationNode> effects) {
    return new WritesNode(getOffset(), effects);
  }
  
  @Override
  public String getLabel() {
    return "writes";
  }
}
