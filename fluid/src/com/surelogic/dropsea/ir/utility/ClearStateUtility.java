package com.surelogic.dropsea.ir.utility;

import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.ir.drops.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.JavaPromise;

public class ClearStateUtility {
	public static void clearAllState() {
		for(CUDrop d : Sea.getDefault().getDropsOfType(CUDrop.class)) {
			invalidate(d.getCompilationUnitIRNode());
		}
		SlotInfo.gc();
		// TODO other state to clean out?
		
		Sea.getDefault().invalidateAll();
	}

	private static void invalidate(IRNode root) {
		for(IRNode n : JavaPromise.bottomUp(root)) {
			n.destroy();
		}
	}
}
