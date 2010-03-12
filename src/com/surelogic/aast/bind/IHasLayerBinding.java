/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast.bind;

import com.surelogic.ast.Resolvable;

public interface IHasLayerBinding extends Resolvable {
	ILayerBinding resolveBinding();
}
