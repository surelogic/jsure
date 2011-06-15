/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.proxy;

import com.surelogic.analysis.IIRAnalysis;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.InfoDrop.Factory;

/**
 * Temporary builder to help analyses be parallelized
 * for both Info/WarningDrop
 * 
 * @author Edwin
 */
public class InfoDropBuilder extends AbstractDropBuilder {
	private final Factory factory;
	
	private InfoDropBuilder(String type, Factory f) {
		super(type);
		factory = f;
	}
	
	public static InfoDropBuilder create(IIRAnalysis a, String type, Factory f) {
		InfoDropBuilder rv = new InfoDropBuilder(type, f);
		a.handleBuilder(rv);
		return rv;
	}
	
	@Override
	public int build() {
		if (!isValid()) {
			return 0;
		}
		InfoDrop rd = factory.create(type);
		return buildDrop(rd);
	}
}
