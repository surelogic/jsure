/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast.layers;

import java.util.*;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeModifier;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.Declaration;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * A list of various layers
 * 
 * @author Edwin
 */
public class TargetListNode extends AbstractLayerMatchTarget {	
	private final List<AbstractLayerMatchTarget> union;
	
	public static final AbstractAASTNodeFactory factory =
		new AbstractAASTNodeFactory("TargetList") {
		@Override
		public AASTNode create(String _token, int _start, int _stop,
				int _mods, String _id, int _dims, List<AASTNode> _kids) {			
			List<AbstractLayerMatchTarget> union = new ArrayList<AbstractLayerMatchTarget>();
			for(AASTNode n : _kids) {
				union.add((AbstractLayerMatchTarget) n);
			}
			return new TargetListNode(_start, union);
		}
	};
	
	TargetListNode(int offset, List<AbstractLayerMatchTarget> set) {
		super(offset);
		if (set == null || set.isEmpty()) {
			throw new IllegalArgumentException("Bad set: "+set);
		}		
		union = set;
		for(AbstractLayerMatchTarget ut : union) {
			ut.setParent(this);
		}
	}
	
	public Iterable<AbstractLayerMatchTarget> getUnion() {
		return union;
	}
	
	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	protected IAASTNode internalClone(final INodeModifier mod) {
		List<AbstractLayerMatchTarget> clonedUnion = new ArrayList<AbstractLayerMatchTarget>();
		for(AbstractLayerMatchTarget ut : union) {
			clonedUnion.add((UnidentifiedTargetNode) ut.cloneOrModifyTree(mod));
		}
		return new TargetListNode(offset, clonedUnion);
	}

	@Override
	public String unparse(boolean debug, int indent) {
	    StringBuilder sb = new StringBuilder();
		if (debug) {
		    indent(sb, indent);		    
		    sb.append("TargetListNode\n");
		    indent(sb, indent + 2);
		    for(AbstractLayerMatchTarget ut : union) {
		    	indent(sb, indent + 2);
			    sb.append(ut.unparse(debug, indent+2));		    
			    sb.append("\n");
		    }
		} else {
			if (union.size() > 1) {
				boolean first = true;
				for(AbstractLayerMatchTarget ut : union) {
					if (first) {
						first = false;
					} else {
						sb.append(", ");
					}
					sb.append(ut.unparse(debug, indent));
				}
			} else {
				sb.append(union.get(0).unparse(debug, indent));
			}
		}
	    return sb.toString();
	}
	
	@Override
	public Operator appliesTo() {
		return Declaration.prototype;
	}

	@Override
	public boolean matches(IRNode type) {
		for(AbstractLayerMatchTarget n : union) {
			if (n.matches(type)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterable<String> getNames() {
		List<String> names = new ArrayList<String>();
		for(AbstractLayerMatchTarget t : union) {
			for(String s : t.getNames()) {
				names.add(s);
			}
		}
		return names;
	}
}
