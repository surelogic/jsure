/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast.layers;

import java.util.*;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.parse.AbstractSingleNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.Declaration;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * A list of various layers
 * 
 * @author Edwin
 */
public class TargetListNode extends AbstractLayerMatchTarget {	
	private final List<UnidentifiedTargetNode> union;
	
	public static final AbstractSingleNodeFactory factory =
		new AbstractSingleNodeFactory("TargetList") {
		@Override
		public AASTNode create(String _token, int _start, int _stop,
				int _mods, String _id, int _dims, List<AASTNode> _kids) {			
			List<UnidentifiedTargetNode> union = new ArrayList<UnidentifiedTargetNode>();
			for(AASTNode n : _kids) {
				union.add((UnidentifiedTargetNode) n);
			}
			return new TargetListNode(_start, union);
		}
	};
	
	TargetListNode(int offset, List<UnidentifiedTargetNode> set) {
		super(offset);
		if (set == null || set.isEmpty()) {
			throw new IllegalArgumentException("Bad set: "+set);
		}		
		union = set;
		for(UnidentifiedTargetNode ut : union) {
			ut.setParent(this);
		}
	}
	
	public Iterable<UnidentifiedTargetNode> getUnion() {
		return union;
	}
	
	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public IAASTNode cloneTree() {
		List<UnidentifiedTargetNode> clonedUnion = new ArrayList<UnidentifiedTargetNode>();
		for(UnidentifiedTargetNode ut : union) {
			clonedUnion.add((UnidentifiedTargetNode) ut.cloneTree());
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
		    for(UnidentifiedTargetNode ut : union) {
		    	indent(sb, indent + 2);
			    sb.append(ut.unparse(debug, indent+2));		    
			    sb.append("\n");
		    }
		} else {
			if (union.size() > 1) {
				boolean first = true;
				for(UnidentifiedTargetNode ut : union) {
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
		for(UnidentifiedTargetNode n : union) {
			if (n.matches(type)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterable<String> getNames() {
		List<String> names = new ArrayList<String>();
		for(UnidentifiedTargetNode t : union) {
			for(String s : t.getNames()) {
				names.add(s);
			}
		}
		return names;
	}
}
