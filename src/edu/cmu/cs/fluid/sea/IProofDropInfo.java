/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea;

import java.util.*;

public interface IProofDropInfo extends IDropInfo {
	boolean provedConsistent();
	boolean proofUsesRedDot();
	boolean isConsistent();
	Collection<? extends IProofDropInfo> getTrusts();
	Collection<? extends IProofDropInfo> getChecks();
	boolean isFromSrc();
}
