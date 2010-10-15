/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.proxy;

import com.surelogic.analysis.IIRAnalysis;
import edu.cmu.cs.fluid.sea.*;

/**
 * Temporary builder to help analyses be parallelized
 * for both Info/WarningDrop
 * 
 * @author Edwin
 */
public class InfoDropBuilder extends AbstractDropBuilder {
	private final boolean isWarning;
	
	private InfoDropBuilder(String type, boolean warn) {
		super(type);
		isWarning = warn;
	}
	
	public static InfoDropBuilder create(IIRAnalysis a, String type, boolean warn) {
		InfoDropBuilder rv = new InfoDropBuilder(type, warn);
		a.handleBuilder(rv);
		return rv;
	}
	
	@Override
	public int build() {
		if (!isValid()) {
			return 0;
		}
		InfoDrop rd = isWarning ? new WarningDrop(type) : new InfoDrop(type);				
		return buildDrop(rd);
	}
}
