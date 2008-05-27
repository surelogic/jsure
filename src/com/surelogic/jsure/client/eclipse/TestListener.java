package com.surelogic.jsure.client.eclipse;

import java.util.*;

import com.surelogic.jsure.xml.*;

public class TestListener implements XMLResultListener {	
	static class Ref {
		final String from;
		final String to;
		final String name;
		
		public Ref(String from, Entity ref) {
			this.from = from;
			this.to   = ref.getId();
			this.name = ref.toString();
		}
	}
	private static final List<Ref> DEFINED = Collections.<Ref>emptyList();
	private final Map<String,List<Ref>> references = new HashMap<String,List<Ref>>();
	
	private void define(Entity e) {
		final List<Ref> refs = references.get(e.getId());
		// TODO define
		references.put(e.getId(), DEFINED);

		if (refs != null) {
			// Handle dangling refs
			for(Ref r : refs) {
				// TODO handle
				System.out.println("Handled "+r.name+" ref from "+r.from+" to "+r.to);
			}
		}
	}
	
	private void processRef(String from, Entity to) {
		List<Ref> refs = references.get(to.getId());
		if (refs == null) {
			refs = new ArrayList<Ref>();
			references.put(to.getId(), refs);
		}
		else if (refs == DEFINED) {
			// TODO handle
			System.out.println("Handled "+to+" ref from "+from+" to "+to.getId());
			return;
		}
		refs.add(new Ref(from, to));
	}
	
	public void notify(Entity e) {
		System.out.println("Got "+e);
		define(e);
		
		for(Entity ref : e.getReferences()) {
			processRef(e.getId(), ref);
		}
	}

	public void done() {
		for(Map.Entry<String,List<Ref>> e : references.entrySet()) {		
			if (e.getValue() != DEFINED) {
				System.out.println("Dangling references to id "+e.getKey());
			}
		}
	}	
}
