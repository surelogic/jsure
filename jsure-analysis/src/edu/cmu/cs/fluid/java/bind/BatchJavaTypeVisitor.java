/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/BatchJavaTypeVisitor.java,v 1.1 2008/07/25 18:13:47 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.util.Map;
import java.util.logging.Level;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class BatchJavaTypeVisitor extends JavaTypeVisitor {
	private static final BatchJavaTypeVisitor prototype = new BatchJavaTypeVisitor();
	private Map<IRNode, IJavaType> typeMap;
	private static int cached, total;
	
	private BatchJavaTypeVisitor() {
		// Only to enforce who creates these
	}
	
	public static void printStats() {
		System.out.println("cached = "+cached);
		System.out.println("totalT = "+total);
	}
	
	public static void getBindings(final Map<IRNode,IBinding> bMap, 
			                       Map<IRNode, IJavaType> tMap, IBinder binder, IRNode root) {
		BatchJavaTypeVisitor jtv = BatchJavaTypeVisitor.prototype;
		synchronized ( jtv ) {
			final IBinder preBinder = jtv.getBinder();
			jtv.setBinder(binder);
			jtv.typeMap = tMap;
			try {
				for(IRNode node : JJNode.tree.topDown(root)) {
					final Operator op = JJNode.tree.getOperator(node);
					if (op instanceof IHasBinding) {
						bMap.put(node, binder.getIBinding(node));
					}					
					if (op instanceof IHasType) {
						total++;
						if (tMap.containsKey(node)) {
							cached++;
							continue;
						}
						IJavaType result = jtv.doAccept(node, op); 
						if (result == null ) {
							LOG.log( Level.SEVERE, "Cannot get type for " + DebugUnparser.toString(node) );
							jtv.doAccept(node);
						}
					}
				}
			} finally {
				jtv.typeMap = null;
				jtv.setBinder(preBinder);
			}
		}
	}

	public IJavaType doAccept(IRNode node, Operator op) {
		IJavaType t = ((IAcceptor) op).accept(node, this);
		if (t != null) {
			typeMap.put(node, t);
		}
		return t;
	}
	
	@Override
	public IJavaType doAccept(IRNode node) {
		IJavaType t = typeMap.get(node);
		if (t == null) {
			t = super.doAccept(node);
			if (t != null) {
				typeMap.put(node, t);
			}
		} else {
			cached++;
		}
		total++;
		return t;
	}
}
