package com.surelogic.analysis.granules;

import java.util.List;

import com.surelogic.analysis.visitors.FlowUnitFinder;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import extra166y.Ops.Procedure;

public final class FlowUnitGranulator extends AbstractGranulator<FlowUnitGranule> {
  public static final FlowUnitGranulator prototype = new FlowUnitGranulator();
  
  
  
  private FlowUnitGranulator() {
    super(FlowUnitGranule.class);
  }

  
  
  @Override
  protected void extractGranules(
      final List<FlowUnitGranule> granules,
      final ITypeEnvironment tEnv,
      final IRNode cu) {
    final GranuleFinder granuleFinder = new GranuleFinder(granules);
    granuleFinder.doAccept(cu);
  }

  
  
  private static final class GranuleFinder extends FlowUnitFinder {
    private final List<FlowUnitGranule> granules;
    
    public GranuleFinder(final List<FlowUnitGranule> list) {
      super(true);
      granules = list;
    }

    @Override
    protected Callback createCallback() {
      return new Callback() {
        @Override
        public void foundMethodDeclaration(final IRNode mdecl) {
          granules.add(FlowUnitGranule.newMethod(getEnclosingType(), mdecl));
        }
        
        @Override
        public void foundConstructorDeclaration(final IRNode cdecl) {
          granules.add(FlowUnitGranule.newConstructor(getEnclosingType(), cdecl));
        }
        
        @Override
        public void foundClassInitializer(final IRNode classInit) {
          granules.add(FlowUnitGranule.newClassInitializer(getEnclosingType(), classInit));
        }
      };
    }
  }
  
  @Override
  public Procedure<FlowUnitGranule> wrapAnalysis(final Procedure<FlowUnitGranule> proc) {
	  return new Procedure<FlowUnitGranule>() {
		@Override
		public void op(FlowUnitGranule g) {
			final JavaComponentFactory jcf = JavaComponentFactory.startUse();
		    try {
		    	proc.op(g);		    
		    } finally {
		      JavaComponentFactory.finishUse(jcf);
		    }
		}
	  };
  }
}
