package com.surelogic.dropsea.irfree;

import java.util.*;

import com.surelogic.dropsea.*;

/**
 * Encapsulating a drop 
 * 
 * @author Edwin
 */
public class DiffNode extends AbstractDiffNode {
	final IDrop drop;
	int cachedHash; // To store for each pass
	
	DiffNode(IDrop d) {
		if (d == null) {
			throw new IllegalArgumentException();
		}
		drop = d;
	}

	@Override
  public IDrop getDrop() {
		return drop;
	}
	
//	@Override
	@Override
  public String getText() {
		return drop.getMessage();
	}
	
	@Override
	public boolean hasChildren() {
		if (getDiffStatus() != Status.OLD) {
			return false;			
		}
		if (!drop.getHints().isEmpty() || !drop.getProposals().isEmpty()) {
			return true;
		}
		if (drop instanceof IAnalysisResultDrop) {
			IAnalysisResultDrop rd = (IAnalysisResultDrop) drop;
			if (!rd.getChecked().isEmpty() || !rd.getTrusted().isEmpty()) {
				return true;
			}			
		}
		return false;
	}
	
	@Override
	public Object[] getChildren() {
		List<DiffNode> children = new ArrayList<DiffNode>();
		wrap(children, "hint", drop.getHints());
		wrap(children, "proposal", drop.getProposals());
		if (drop instanceof IAnalysisResultDrop) {
			IAnalysisResultDrop rd = (IAnalysisResultDrop) drop;
			wrap(children, "checked", rd.getChecked());
			wrap(children, "trusted", rd.getTrusted());
		}
		return children.toArray();
	}

	private void wrap(List<DiffNode> children, final String label, Iterable<? extends IDrop> drops) {
		for(final IDrop d : drops) {
			children.add(new DiffNode(d) {
				@Override
				public String getText() {
					return label+": "+d.getMessage();
				}
			});
		}
	}
}
