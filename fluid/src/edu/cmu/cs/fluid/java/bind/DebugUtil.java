package edu.cmu.cs.fluid.java.bind;

import java.util.*;

import com.surelogic.common.concurrent.*;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public class DebugUtil {
	private static final Set<IRNode> dumped = new ConcurrentHashSet<IRNode>();
	
	/**
	 * Output the interface outline of the given type	 
	 */
	public static void dumpType(IRNode tdecl) {
		// Check if already dumped
		if (!dumped.add(tdecl)) {
			return;
		}
		// TODO
	}
	
	public static void dumpClosestType(IRNode n) {
		dumpType(VisitUtil.getClosestType(n));
	}
}
