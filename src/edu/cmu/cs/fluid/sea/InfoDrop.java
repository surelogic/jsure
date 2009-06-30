package edu.cmu.cs.fluid.sea;

import edu.cmu.cs.fluid.sea.xml.SeaSnapshot;

/**
 * Drops for reporting inferred or information to the user, "i" results.
 */
public class InfoDrop extends IRReferenceDrop {
	private final String type;

	public InfoDrop(String t) {
		type = t;
	}
	
	public InfoDrop() {
		this(null);
	}
	
	@Override
	public void snapshotAttrs(SeaSnapshot s) {
		super.snapshotAttrs(s);
		if (type != null) {
			s.addAttribute("result-type", type);
		} else {
			System.out.println("result-type is null");
		}
	}
}