/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.promise;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;

public final class PromiseDropStorage {
	private PromiseDropStorage() {
		// Nothing to do, since never created
	}
	
	private static List<BooleanPromiseDropStorage<?>> booleans = 
		new ArrayList<BooleanPromiseDropStorage<?>>();
	private static List<SinglePromiseDropStorage<?>> nodes = 
		new ArrayList<SinglePromiseDropStorage<?>>();
	private static List<PromiseDropSeqStorage<?>> sequences = 
		new ArrayList<PromiseDropSeqStorage<?>>();
	
	public static void clearDrops(IRNode n) {
		for(BooleanPromiseDropStorage<?> b : booleans) {
			if (n.valueExists(b.getSlotInfo())) {
				n.setSlotValue(b.getSlotInfo(), null);			
			}
		}
		for(SinglePromiseDropStorage<?> s : nodes) {
			if (n.valueExists(s.getSlotInfo())) {
				n.setSlotValue(s.getSlotInfo(), null);			
			}		
		}
		for(PromiseDropSeqStorage<?> s : sequences) {
			if (n.valueExists(s.getSlotInfo())) {
				n.setSlotValue(s.getSeqSlotInfo(), null);			
			}
		}
	}
}
