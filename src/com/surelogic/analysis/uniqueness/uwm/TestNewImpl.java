package com.surelogic.analysis.uniqueness.uwm;

import java.util.HashMap;
import java.util.Map;

import com.surelogic.analysis.*;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.uniqueness.cmu.UniqueAnalysis;
import com.surelogic.analysis.uniqueness.uwm.UniquenessAnalysis;
import com.surelogic.analysis.uniqueness.uwm.UniquenessAnalysis.RawQuery;
import com.surelogic.analysis.uniqueness.uwm.store.FieldTriple;
import com.surelogic.analysis.uniqueness.uwm.store.State;
import com.surelogic.analysis.uniqueness.uwm.store.Store;
import com.surelogic.analysis.uniqueness.uwm.store.StoreLattice;

import edu.cmu.cs.fluid.control.Component.WhichPort;
import edu.cmu.cs.fluid.control.FlowAnalysis;
import edu.cmu.cs.fluid.control.FlowAnalysis.AnalysisGaveUp;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.cmu.cs.fluid.util.Triple;
import edu.cmu.cs.fluid.util.UnionLattice;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;


public class TestNewImpl extends AbstractWholeIRAnalysis<TestNewImpl.Visitor, Void> {
  private static final Category TEST_CATEGORY = Category.getInstance("TEST"); 
  
  private Effects e = null;
  private UniquenessAnalysis n = null;
  private UniqueAnalysis o = null;

  public TestNewImpl() {
    super("Test New Uniqueness");
  }

  @Override
  protected Visitor constructIRAnalysis(final IBinder binder) {
    e = new Effects(binder);
    n = new UniquenessAnalysis(binder, e);
    o = new UniqueAnalysis(binder, e, 0);
    return new Visitor(binder);
  }

  @Override
  protected boolean doAnalysisOnAFile(
      final CUDrop cud, final IRNode compUnit, final IAnalysisMonitor monitor) {
    runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
      public void run() {
        runOverFile(compUnit, monitor);
      }
    });
    return true;
  }

  @Override
  protected void clearCaches() {
    // Nothing to do
  }



  protected void runOverFile(final IRNode compUnit, final IAnalysisMonitor monitor) {
    getAnalysis().setMonitor(monitor);
    getAnalysis().doAccept(compUnit);
    ImmutableHashOrderSet.clearCaches();
    JavaComponentFactory.clearCache();
  }
  
  
  private static class Record {
    public final StoreLattice newLattice;
    public final RawQuery newUniqueness;
    @SuppressWarnings("rawtypes")
    public final FlowAnalysis oldUniqueness;
    
    public Record(final StoreLattice l, final RawQuery newU, @SuppressWarnings("rawtypes") final FlowAnalysis oldU) {
      newLattice = l;
      newUniqueness = newU;
      oldUniqueness = oldU;
    }
  }
  

  public final class Visitor extends AbstractJavaAnalysisDriver<Record> implements IBinderClient {
    private final IBinder binder;
    private volatile IAnalysisMonitor monitor;
    
    
    public Visitor(final IBinder b) {
      binder = b;
      monitor = null;
    }
    
    public void setMonitor(final IAnalysisMonitor m) {
      monitor = m;
    }
    
    
    
    @Override
    protected Record createNewQuery(final IRNode decl) {
      final String methodName;
      if (ClassInitializer.prototype.includes(decl)) {
        methodName = "initializer";
      } else {
        methodName = JavaNames.genQualifiedMethodConstructorName(decl);
      }
      System.out.println("Checking " + methodName);
      if (monitor != null) {
        monitor.subTask("Checking [ Uniqueness Test ] " + methodName);
      }

      
      
      JavaForwardAnalysis<Store, StoreLattice> newAnalysis = null;
      StoreLattice newLattice = null;
      RawQuery newRawQuery = null;
      try {
        newAnalysis = n.getAnalysis(decl);
        newLattice = newAnalysis.getLattice();
        newRawQuery = n.getRaw(decl);
      } catch(final edu.uwm.cs.fluid.control.FlowAnalysis.AnalysisGaveUp e) {
        System.out.println("XXXXX New analysis gave up on " + DebugUnparser.toString(decl));
        newAnalysis = null;
        newLattice = null;
        newRawQuery = null;
      }
      
      FlowAnalysis<Object> oldAnalysis = null;
      try {
        oldAnalysis = o.getAnalysis(decl);
      } catch(final AnalysisGaveUp e) {
        System.out.println("XXXXX Old analysis gave up on " + DebugUnparser.toString(decl));
        oldAnalysis = null;
      }
      return new Record(newLattice, newRawQuery, oldAnalysis);
    }

    @Override
    protected Record createSubQuery(final IRNode caller) {
      final StoreLattice newLattice = currentQuery().newLattice;
      final RawQuery subAnalysisQuery = currentQuery().newUniqueness.getSubAnalysisQuery(caller);
      FlowAnalysis<Object> oldAnalysis = null;
      try {
        oldAnalysis = (caller instanceof AnonClassExpression) ? o.getAnalysis(caller) : null;
      } catch(final AnalysisGaveUp e) {
        System.out.println("XXXXX Old analysis gave up on " + DebugUnparser.toString(caller));
        oldAnalysis = null;
      }
      return new Record(newLattice, subAnalysisQuery, oldAnalysis);
    }
  
    
    
    @Override
    public final Void visitStringLiteral(final IRNode node) {
      return null;
    }
    
    @Override
    public final Void visitExpression(final IRNode node) {
      compareResults(node);
      return super.visitExpression(node);
    }
    
    @Override
    public final Void visitStatement(final IRNode node) {
      compareResults(node);
      return super.visitStatement(node);
    }
    
    
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void compareResults(final IRNode node) {
      final Record currentQuery = currentQuery();
      if (currentQuery != null && currentQuery.newLattice != null && currentQuery.oldUniqueness != null) {
        final Store newStore = currentQuery.newUniqueness.getResultFor(node);
        final com.surelogic.analysis.uniqueness.cmu.Store oldStore = 
          (com.surelogic.analysis.uniqueness.cmu.Store) currentQuery.oldUniqueness.getAfter(node, WhichPort.ENTRY);

        /* Compare the stores: TOP and BOTTOM are swapped!! */
        boolean equalStores = true;
        if (currentQuery.newLattice.equals(newStore, currentQuery.newLattice.bottom())) {
          // Check BOTTOM
          equalStores = oldStore.equals(oldStore.top());
        } else if (currentQuery.newLattice.equals(newStore, currentQuery.newLattice.top())) {
          // Check TOP
          equalStores = oldStore.equals(oldStore.bottom());
        } else if (currentQuery.newLattice.lattice1.equals(newStore.first(), currentQuery.newLattice.lattice1.top())) {
          // Check error messages
          equalStores = currentQuery.newLattice.toString(newStore).equals(oldStore.toString());
        } else if (oldStore.getStackSize().equals(oldStore.getStackSize().bottom())) {
          // Check error messages
          equalStores = currentQuery.newLattice.toString(newStore).equals(oldStore.toString());
        } else if (newStore.getStackSize().intValue() != oldStore.getStackSize().intValue()) {
          // Check if they have the same stack size
          equalStores = false;
        } else {
          // Check sets of objects
          final ImmutableSet<Object> oldObjects = oldStore.getObjects();
          if (oldObjects.size() != newStore.getObjects().size()) {
            equalStores = false;
          } else {
            final Map<ImmutableSet<Object>, ImmutableSet<Object>> map = 
              new HashMap<ImmutableSet<Object>, ImmutableSet<Object>>();
            
            for (final ImmutableSet<Object> newObj : newStore.getObjects()) {
              UnionLattice<Object> mappedObj = new UnionLattice<Object>();
              for (final Object o : newObj) {
                Object addMe = o;
                if (o == State.UNDEFINED) addMe = com.surelogic.analysis.uniqueness.cmu.Store.undefinedVariable;
                else if (o == State.BORROWED) addMe = com.surelogic.analysis.uniqueness.cmu.Store.borrowedVariable;
                else if (o == State.SHARED) addMe = com.surelogic.analysis.uniqueness.cmu.Store.sharedVariable;
                mappedObj = (UnionLattice) mappedObj.addElement(addMe);
              }
              map.put(newObj, mappedObj);
              if (!oldObjects.contains(mappedObj)) {
                equalStores = false;
                break;
              }
            }
            
            if (equalStores) {
              // test field store
              final UnionLattice oldFS = oldStore.getFieldStore();
              if (oldFS.size() != newStore.getFieldStore().size()) {
                equalStores = false;
              } else {
                for (final FieldTriple t : newStore.getFieldStore()) {
                  final Triple tt = new Triple(map.get(t.first()), t.second(), map.get(t.third()));
                  if (!oldFS.contains(tt)) {
                    equalStores = false;
                    break;
                  }
                }
              }
            }
          }
        }
        
        final InfoDrop drop = new InfoDrop();
        setResultDependUponDrop(drop, node);
        drop.setCategory(TEST_CATEGORY);
        drop.setMessage(
            (equalStores ? "EQUAL" : "NOT EQUAL") + 
            (isInsideConstructor() ? " (inside constructor)" : ""));
        
        if (!equalStores) {
          System.out.println("*************************************************");
          final ISrcRef srcRef = JavaNode.getSrcRef(node);
          System.out.println("WHERE: " + ((srcRef == null) ? "UNKNOWN" : (srcRef.getCUName() + "@" + srcRef.getLineNumber())));
          System.out.println("NODE: " + DebugUnparser.toString(node));
          System.out.println("OP: " + JJNode.tree.getOperator(node).name());
          System.out.println("OLD:");
          System.out.println(oldStore.toString());
          System.out.println();
          System.out.println("NEW:");
          System.out.println(currentQuery.newLattice.toString(newStore));
       }
      }
    }

    
    
    public IBinder getBinder() {
      return binder;
    }

    public void clearCaches() {
      // nothing to do
    }
  }
}