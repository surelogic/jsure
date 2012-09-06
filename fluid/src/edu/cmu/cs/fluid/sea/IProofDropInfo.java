/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea;

import java.util.*;

public interface IProofDropInfo extends IDrop {
	boolean provedConsistent();
	boolean proofUsesRedDot();

	/**
	 * Returns true if this result depends on something from source
	 */
	boolean derivedFromSrc();
	boolean isFromSrc();
	boolean isIntendedToBeCheckedByAnalysis();
	boolean isConsistent();
	boolean isCheckedByAnalysis();
	boolean isAssumed();
	boolean isVirtual();
	
	boolean isVouched();
	boolean isTimeout();
	Collection<? extends IProofDropInfo> getTrusts();
	Collection<? extends IProofDropInfo> getChecks();
	boolean hasOrLogic();
	Collection<String> get_or_TrustLabelSet();
	boolean get_or_proofUsesRedDot();
	boolean get_or_provedConsistent();
	Collection<? extends IProofDropInfo> get_or_Trusts(String key);
	Collection<? extends IProofDropInfo> getCheckedBy();
	Collection<? extends IProofDropInfo> getTrustsComplete();
}
