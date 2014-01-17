package com.surelogic.analysis;

import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public final class TopLevelType extends GranuleInType {
	public TopLevelType(IRNode type) {
		super(type);
	}

	public IRNode getNode() {
		return typeDecl;
	}

	public String getLabel() {
		return JavaNames.getFullTypeName(typeDecl);
	}
	
	public static final IAnalysisGranulator<TopLevelType> granulator = new AbstractGranulator<TopLevelType>(TopLevelType.class) {
		@Override
		protected void extractGranules(List<TopLevelType> granules, IRNode cu) {
			for(final IRNode type : VisitUtil.getTypeDecls(cu)) {
				granules.add(new TopLevelType(type));
			}			
		}
	};
}
