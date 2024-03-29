package com.surelogic.analysis.granules;

import java.util.List;

import com.surelogic.analysis.visitors.FlowUnitFinder;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;

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
      super(SkipAnnotations.YES);
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
}
