/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.promise;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;

public final class PromiseDropStorage {
	private static final Logger LOG = SLLogger.getLogger();
	
	private PromiseDropStorage() {
		// Nothing to do, since never created
	}
	
	private static List<BooleanPromiseDropStorage<?>> booleans = 
		new ArrayList<BooleanPromiseDropStorage<?>>();
	private static List<SinglePromiseDropStorage<?>> nodes = 
		new ArrayList<SinglePromiseDropStorage<?>>();
	private static List<PromiseDropSeqStorage<?>> sequences = 
		new ArrayList<PromiseDropSeqStorage<?>>();
	
	static void register(BooleanPromiseDropStorage<?> s) {
		booleans.add(s);
	}
	static void register(SinglePromiseDropStorage<?> s) {
		nodes.add(s);
	}
	static void register(PromiseDropSeqStorage<?> s) {

		sequences.add(s);
	}
	
	public static void init() {
		// Check if all initialized
		for(BooleanPromiseDropStorage<?> b : 
			new ArrayList<BooleanPromiseDropStorage<?>>(booleans)) {
			if (b.getSlotInfo() == null) {
				LOG.fine("No SlotInfo for "+b.name());
				booleans.remove(b);
			}
		}
		for(SinglePromiseDropStorage<?> s : nodes) {
			if (s.getSlotInfo() == null) {
				LOG.warning("No SlotInfo for "+s.name());
			}	
		}
		for(PromiseDropSeqStorage<?> s : sequences) {
			if (s.getSeqSlotInfo() == null) {
				LOG.warning("No SlotInfo for "+s.name());
			}
		}
	}
	
	public static void clearDrops(IRNode n) {
		for(BooleanPromiseDropStorage<?> b : booleans) {
			if (b.getSlotInfo() == null) {
				continue;
			}
			Object o = n.getSlotValue(b.getSlotInfo());
			if (o != null) {
				//System.out.println("Cleared: "+b.name()+" on "+n);
				n.setSlotValue(b.getSlotInfo(), null);			
			}
		}
		for(SinglePromiseDropStorage<?> s : nodes) {
			if (s.getSlotInfo() == null) {
				continue;
			}
			Object o = n.getSlotValue(s.getSlotInfo());
			if (o != null) {
				//System.out.println("Cleared: "+s.name()+" on "+n);
				n.setSlotValue(s.getSlotInfo(), null);			
			}		
		}
		for(PromiseDropSeqStorage<?> s : sequences) {
			if (s.getSeqSlotInfo() == null) {
				continue;
			}
			Object o = n.getSlotValue(s.getSeqSlotInfo());
			if (o != null) {
				//System.out.println("Cleared: "+s.name()+" on "+n);
				n.setSlotValue(s.getSeqSlotInfo(), null);			
			}
		}
	}
}
