package edu.cmu.cs.fluid.control;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.ConnectedNodes;

public class DumpCFG {

	public DumpCFG() { }

	public DumpCFG(Component c) {
		this();
		addRoot(c);
	}
	
	public void addRoot(Component c) {
		if (allNodes == null) {
			allNodes = new Nodes(c.getEntryPort());
		} else {
			allNodes.add(c.getEntryPort());
		}
		allNodes.add(c.getNormalExitPort());
		allNodes.add(c.getAbruptExitPort());
	}
	
	public void dump() {
		dump(new PrintWriter(System.out));
	}
	
	private String toString(Map<IRNode,Integer> index, IRNode n) {
		Integer i = index.get(n);
		if (i == null) return n.toString();
		else return i.toString();
	}
	
 	public void dump(PrintWriter pw) {
 		List<IRNode> nodes = new ArrayList<IRNode>();
 		Map<IRNode,Integer> index = new HashMap<IRNode,Integer>();
 		while (allNodes.hasNext()) {
 			int i = nodes.size();
 			IRNode n = allNodes.next();
 			nodes.add(n);
 			index.put(n, i);
 		}
 		int n = nodes.size();
 		for (int i=0; i < n; ++i) {
 			IRNode node = nodes.get(i);
 			pw.println(i + ": " + node.toString());
 			pw.print("  before: ");
 			for (IRNode p : ControlFlowGraph.prototype.parents(node)) {
 				pw.print(toString(index,p));
 				pw.print(" ");
 			}
 			pw.print("\n  after: ");
 			pw.flush();
 			for (IRNode p : ControlFlowGraph.prototype.children(node)) {
 				pw.print(toString(index,p));
 				pw.print(" ");
 			}
 			pw.println();
 			pw.flush();
 		}
	}
	
	private Nodes allNodes;
	
	private class Nodes extends ConnectedNodes {

		private Queue<IRNode> extraRoots = new LinkedList<IRNode>();
		
		public Nodes(IRNode root) {
			super(ControlFlowGraph.prototype, root);
		}
		
		public void add(IRNode root) {
			extraRoots.add(root);
		}
		
		public boolean additionalChildren(IRNode n) {
			if (super.additionalChildren(n)) return true;
			while (!extraRoots.isEmpty()) {
				IRNode extra = extraRoots.remove();
				if (extra == null) continue;
				if (!mark(extra)) continue;
				visit(extra);
				return true;
			}
			return false;
		}
		
	}
}
