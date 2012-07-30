package edu.cmu.cs.fluid.sea;

import com.surelogic.common.xml.XMLCreator;

import edu.cmu.cs.fluid.sea.xml.*;

/**
 * Drops for reporting inferred or information to the user, "i" results.
 */
public class InfoDrop extends IRReferenceDrop implements IResultDrop {
	private final String type;

	public InfoDrop(String t) {
		type = t;
	}
	
	@Override
	public void snapshotAttrs(XMLCreator.Builder s) {
		super.snapshotAttrs(s);
		if (type != null) {
			s.addAttribute("result-type", type);
		} else {
			System.out.println("InfoDrop result-type is null");
		}
	}
	
	public interface Factory {
		InfoDrop create(String type);
	}
	
	public static final Factory factory = new Factory() {
		public InfoDrop create(String type) {
			return new InfoDrop(type);
		}
	};
}