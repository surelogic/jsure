package edu.cmu.cs.fluid.java.bind;

import java.io.*;
import java.util.*;

import com.surelogic.common.concurrent.*;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.*;
import edu.cmu.cs.fluid.tree.*;
import edu.cmu.cs.fluid.parse.JJNode;

public class DebugUtil {
	private static final Set<IRNode> dumped = new ConcurrentHashSet<IRNode>();
	
	private static DebugUnparser unparser = new DebugUnparser(-1, JJNode.tree) {
		@Override
		public void unparse(IRNode node) {
			if (node != null) {
				final Operator op = JJNode.tree.getOperator(node);
				if (Statement.prototype.includes(op) || Initializer.prototype.includes(op)) {			
					deepToken.emit(this,node);
					return;
				}
			}
			super.unparse(node);
		}
	};
	
	/**
	 * Output the interface outline of the given type	 
	 */
	public static void dumpType(IRNode tdecl) {
		// Check if already dumped
		if (!dumped.add(tdecl)) {
			return;
		}
		//System.err.println(unparser.unparseString(tdecl));
		final String unparse = unparser.unparseString(tdecl).replace(" = #;", "\n\t").replace(";", "\n\t").replace(" #", "\n\t");
		final StringReader r = new StringReader(unparse);
		final BufferedReader br = new BufferedReader(r);
		String line;
		try {
			while ((line = br.readLine()) != null) {
				System.err.println(line);
			}
		} catch (IOException e) {
			throw new IllegalStateException();
		}
	}
	
	public static void dumpClosestType(IRNode n) {
		dumpType(VisitUtil.getClosestType(n));
	}
}
