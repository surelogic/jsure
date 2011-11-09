/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea;

public abstract class AbstractDropPredicate implements DropPredicate {
	public boolean match(IDropInfo d) {
		throw new UnsupportedOperationException();
	}
}
