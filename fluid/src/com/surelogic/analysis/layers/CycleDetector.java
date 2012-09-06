/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.layers;

import java.util.*;

// From A to B,C,D
public class CycleDetector extends HashMap<String, Set<String>> {
	private static final long serialVersionUID = 1L;

	public void addRef(String here, String qname) {
		Set<String> references = this.get(here);
		if (references == null) {
			references = new HashSet<String>();
			this.put(here, references);
		}
		references.add(qname);
	}
	
	public void addRefs(String key, List<String> inLayers) {
		Set<String> refs = this.get(key);
		if (refs == null) {
			refs = new HashSet<String>();
			this.put(key, refs);
		}
		refs.addAll(inLayers);
	}
	
	public void checkAll() {
		// Removed references to itself
		for(Map.Entry<String, Set<String>> e : this.entrySet()) {
			e.getValue().remove(e.getKey());
			//System.out.println(e.getKey()+" -> "+e.getValue());
		}
		
		// Check for cycles
		final Set<String> seen = new HashSet<String>();			
		for(Map.Entry<String, Set<String>> e : this.entrySet()) {
			//System.out.println("Starting from "+e.getKey());
			seen.clear();
			checkForCycles(seen, e.getKey(), null, false);
		}
	}
	
	public boolean checkOne(String qname) {
		Set<String> seen = new HashSet<String>();			
		//System.out.println("Starting from "+computeCurrentName());
		return checkForCycles(seen, qname, null, true);
	}
	
	private boolean checkForCycles(Set<String> seen, String here, String last, boolean failFast) {
		if (seen.contains(here)) {
			//System.out.println("FAIL: "+here+" already seen");
			reportFailure(here, last);
			return false; // Cycle detected
		}
		seen.add(here);
		
		Set<String> references = this.get(here);
		//System.out.println(here+" -> "+references);
		//System.out.println("\tSeen: "+seen+"\n");
		if (references == null) {
			seen.remove(here);
			return true; // No refs, so no cycle here
		}
		for(String ref : references) {
			if (!checkForCycles(seen, ref, here, failFast) && failFast) {
				return false;
			}
		}
		seen.remove(here);
		return true;
	}
	
	protected void reportFailure(String backedge, String last) {
		// Nothing to do so far
	}
}
