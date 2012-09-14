/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.promise;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.ir.*;

public final class PromiseDropStorage {
	private static final Logger LOG = SLLogger.getLogger();
	
	private PromiseDropStorage() {
		// Nothing to do, since never created
	}
	
	private static List<BooleanPromiseDropStorage<? extends BooleanPromiseDrop<?>>> booleans = 
		new ArrayList<BooleanPromiseDropStorage<? extends BooleanPromiseDrop<?>>>();
	private static List<SinglePromiseDropStorage<? extends PromiseDrop<?>>> nodes = 
		new ArrayList<SinglePromiseDropStorage<? extends PromiseDrop<?>>>();
	private static List<PromiseDropSeqStorage<? extends PromiseDrop<?>>> sequences = 
		new ArrayList<PromiseDropSeqStorage<? extends PromiseDrop<?>>>();
	
	static void register(BooleanPromiseDropStorage<? extends BooleanPromiseDrop<?>> s) {
		booleans.add(s);
	}
	static void register(SinglePromiseDropStorage<? extends PromiseDrop<?>> s) {
		nodes.add(s);
	}
	static void register(PromiseDropSeqStorage<? extends PromiseDrop<?>> s) {
		//System.out.println("Registered "+s.name());
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
			BooleanPromiseDrop<?> o = n.getSlotValue(b.getSlotInfo());
			if (o != null) {
				//System.out.println("Cleared: "+b.name()+" on "+n);
				o.invalidate();
				n.setSlotValue(b.getSlotInfo(), null);			
			}
		}
		for(SinglePromiseDropStorage<?> s : nodes) {
			if (s.getSlotInfo() == null) {
				continue;
			}
			PromiseDrop<?> o = n.getSlotValue(s.getSlotInfo());
			if (o != null) {
				//System.out.println("Cleared: "+s.name()+" on "+n);
				o.invalidate();
				n.setSlotValue(s.getSlotInfo(), null);			
			}		
		}
		for(PromiseDropSeqStorage<? extends PromiseDrop<?>> s : sequences) {
			if (s.getSeqSlotInfo() == null) {
				continue;
			}
			List<? extends PromiseDrop<?>> o = n.getSlotValue(s.getSeqSlotInfo());
			if (o != null) {
				//System.out.println("Cleared: "+s.name()+" on "+n);
				for(PromiseDrop<?> d : o) {
					d.invalidate();
				}
				n.setSlotValue(s.getSeqSlotInfo(), null);			
			}
		}
	}
	
	public static List<PromiseDrop<?>> getAllDrops(IRNode n) {
		// TODO cache these instead? 
		List<PromiseDrop<?>> drops = new ArrayList<PromiseDrop<?>>();
		for(BooleanPromiseDropStorage<? extends BooleanPromiseDrop<?>> b : booleans) {
			if (b.getSlotInfo() == null) {
				continue;
			}
			BooleanPromiseDrop<?> o = n.getSlotValue(b.getSlotInfo());
			if (o != null) {
				drops.add(o);		
			}
		}
		for(SinglePromiseDropStorage<? extends PromiseDrop<?>> s : nodes) {
			if (s.getSlotInfo() == null) {
				continue;
			}
			PromiseDrop<?> o = n.getSlotValue(s.getSlotInfo());
			if (o != null) {
				drops.add(o);		
			}		
		}
		for(PromiseDropSeqStorage<? extends PromiseDrop<?>> s : sequences) {
			if (s.getSeqSlotInfo() == null) {
				continue;
			}
			List<? extends PromiseDrop<?>> o = n.getSlotValue(s.getSeqSlotInfo());
			if (o != null) {
				for(PromiseDrop<?> d : o) {
					drops.add(d);
				}	
			}
		}
		if (drops.isEmpty()) {
			return Collections.emptyList();
		}
		return drops;
	}
}
