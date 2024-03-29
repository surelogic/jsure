package edu.cmu.cs.fluid.java.bind;

import java.util.*;

import com.surelogic.common.ref.IJavaRef;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;

/**
 * Encodes part of the AST as part of the stack trace
 * @author Edwin
 */
public class BindingException extends RuntimeException {
	private static final long serialVersionUID = -2323274122832014430L;

	public BindingException(IRNode context, boolean showParents) {
		this(context, showParents, JJNode.tree.getOperator(context).name()+" - "+findEnclosingType(context), null);
	}
	public BindingException(IRNode context, boolean showParents, String msg, RuntimeException cause) {
		super(msg, cause);
		
		List<StackTraceElement> trace = new ArrayList<StackTraceElement>();
		if (showParents) {
			for(IRNode ancestor : VisitUtil.rootWalk(context)) {
				String name = JJNode.tree.getOperator(ancestor).name();
				StackTraceElement e = new StackTraceElement(name, "op", name, 1);
				trace.add(e);
			}
		} else {
			flattenAST(trace, context, "root", 1);
		}
		for(StackTraceElement e : getStackTrace()) {
			trace.add(e);
		}
		setStackTrace(trace.toArray(new StackTraceElement[trace.size()]));
	}

	private static String findEnclosingType(IRNode context) {
		for(IRNode ancestor : VisitUtil.rootWalk(context)) {
			IJavaRef ref = JavaNode.getJavaRef(ancestor);
			if (ref != null) {
				return ref.getTypeNameFullyQualified();
			}
		}
		return "unknown";
	}

	private void flattenAST(List<StackTraceElement> trace, IRNode n, String parent, int i) {
		String name = JJNode.tree.getOperator(n).name();
		String info = JJNode.getInfoOrNull(n);
		StackTraceElement e = new StackTraceElement(name, info == null ? "na" : info, parent, 1);
		trace.add(e);
		
		for(IRNode c : JJNode.tree.children(n)) {
			flattenAST(trace, c, name, i+1);
		}
	}
}
