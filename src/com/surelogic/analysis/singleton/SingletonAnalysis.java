package com.surelogic.analysis.singleton;

import java.util.*;

import jsr166y.forkjoin.Ops.Procedure;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.ConcurrencyType;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.TopLevelAnalysisVisitor;
import com.surelogic.analysis.TopLevelAnalysisVisitor.TypeBodyPair;
import com.surelogic.analysis.TypeImplementationProcessor;
import com.surelogic.annotation.rules.UtilityRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumDeclaration;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.promises.SingletonPromiseDrop;

public final class SingletonAnalysis extends AbstractWholeIRAnalysis<SingletonAnalysis.SingletonVerifier, TypeBodyPair> {	
  /** Should we try to run things in parallel */
  private static boolean wantToRunInParallel = false;
  
  /**
   * Are we actually going to run things in parallel?  Not all JRE have the
   * libraries we need to actually run in parallel.
   */
  private static boolean willRunInParallel = wantToRunInParallel && !singleThreaded;
  
  /**
   * Use a work queue?  Only relevant if {@link #willRunInParallel} is 
   * <code>true</code>.  Otherwise it is <code>false</code>.
   */
	private static boolean queueWork = willRunInParallel && true;

  /**
   * Analyze compilation units in parallel?  Only relevant if {@link #willRunInParallel} is 
   * <code>true</code> and {@link #queueWork} is <code>true</code>.  Otherwise it is <code>false</code>.
   * When relevant, a <code>false</code> value means analyze by types, a
   * smaller granularity than compilation units.
   */
	private static boolean byCompUnit = queueWork && true; // otherwise by type
	
	
	public SingletonAnalysis() {
		super(willRunInParallel, queueWork ? TypeBodyPair.class : null, "SingletonAssurance");
		if (runInParallel() == ConcurrencyType.INTERNALLY) {
			setWorkProcedure(new Procedure<TypeBodyPair>() {
				public void op(TypeBodyPair n) {
					if (byCompUnit) {
					  TopLevelAnalysisVisitor.processCompilationUnit(
		            // actually n.typeDecl is a CompilationUnit here!
					      new ClassProcessor(getAnalysis()), n.typeDecl());
					} else {
					  actuallyAnalyzeClassBody(getAnalysis(), n.typeDecl(), n.classBody());
					}
				}
			});
		}      
	}
	
	
	private static void actuallyAnalyzeClassBody(final SingletonVerifier sv,
	    final IRNode classDecl, final IRNode classBody) {
	  final SingletonPromiseDrop sDrop = UtilityRules.getSingletonDrop(classDecl);
	  if (sDrop != null) {
	    if (EnumDeclaration.prototype.includes(classDecl)) {
	      sv.verifyEnum(sDrop, classDecl, classBody);
	    }
//	    final UtilityVisitor uv = f.getVisitor(sDrop, classDecl, classBody);
//	    uv.processType();
	  }
	}

	@Override
	protected boolean flushAnalysis() {
		return true;
	}
	
	@Override
	protected SingletonVerifier constructIRAnalysis(final IBinder binder) {		
	  if (binder == null || binder.getTypeEnvironment() == null) {
		  return null;
	  }
	  return new SingletonVerifier(binder);
	}
	
	@Override
	protected void clearCaches() {
		if (runInParallel() != ConcurrencyType.INTERNALLY) {
			final SingletonVerifier lv = getAnalysis();
			if (lv != null) {
				lv.clearCaches();
			}
		} else {
			analyses.clearCaches();
		}
	}
	
	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode compUnit) {
		if (byCompUnit) {
			queueWork(new TypeBodyPair(compUnit, null));
			return true;
		}
		// FIX factor out?
		final ClassProcessor cp = new ClassProcessor(getAnalysis());
		TopLevelAnalysisVisitor.processCompilationUnit(cp, compUnit);
		if (runInParallel() == ConcurrencyType.INTERNALLY) {
			if (queueWork) {
        queueWork(cp.getTypeBodies());
			} else {
        runInParallel(TypeBodyPair.class, cp.getTypeBodies(), getWorkProcedure());
			}
		}
		return true;
	}
	
	@Override
	public Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
		finishBuild();
		return super.analyzeEnd(env, p);
	}
	
	
	
	private final class ClassProcessor extends TopLevelAnalysisVisitor.SimpleClassProcessor {
    private final SingletonVerifier factory;
    private final List<TypeBodyPair> types = new ArrayList<TypeBodyPair>();
    
    public ClassProcessor(final SingletonVerifier f) {
      factory = f;
    }

    public Collection<TypeBodyPair> getTypeBodies() {
      return types;
    }
    
    @Override
    protected void visitTypeDecl(final IRNode typeDecl, final IRNode classBody) {
      if (runInParallel() == ConcurrencyType.INTERNALLY && !byCompUnit) {
        types.add(new TypeBodyPair(typeDecl, classBody));
      } else {
        actuallyAnalyzeClassBody(factory, typeDecl, classBody);
      }
    }
	}



  public final class SingletonVerifier implements IBinderClient {
    private final IBinder binder;
    
    public SingletonVerifier(final IBinder b) {
      binder = b;
    }
    
    
    
    public IBinder getBinder() {
      return binder;
    }

    public void clearCaches() {
      // do nothing
    }
    
    
    public void verifyEnum(final SingletonPromiseDrop sDrop,
        final IRNode enumDecl, final IRNode enumBody) {
      new EnumVerifier(
          SingletonAnalysis.this, sDrop, enumDecl, enumBody).processType();
    }
    
//    
//    
//    public UtilityVisitor getVisitor(final UtilityPromiseDrop uDrop,
//        final IRNode classDecl, final IRNode classBody) {
//      return new UtilityVisitor(
//          SingletonAnalysis.this, uDrop, binder, classDecl, classBody);
//    }
  }
  
  
  
  private static final class EnumVerifier extends TypeImplementationProcessor {
    private int numElements;
    private IRNode element;
    
    
    
    public EnumVerifier(
        AbstractWholeIRAnalysis<? extends IBinderClient, ?> a,
        SingletonPromiseDrop pd, IRNode enumDecl, IRNode enumBody) {
      super(a, pd, enumDecl, enumBody);
      numElements = 0;
      element = null;
    }

    
    
    @Override
    protected String message2string(final int msg) {
      return Messages.toString(msg);
    }
    
    
    
    @Override
    protected void processEnumConstantDeclaration(final IRNode decl) {
      numElements += 1;
      element = decl;
    }
    
    @Override
    protected void postProcess() {
      if (numElements == 0) {
        createResult(typeDecl, false, Messages.ENUM_NO_ELEMENTS);
      } else if (numElements == 1) {
        createResult(element, true, Messages.ENUM_ONE_ELEMENT,
            EnumConstantDeclaration.getId(element));
      } else { // numElements > 1
        createResult(typeDecl, false, Messages.ENUM_TOO_MANY_ELEMENTS);
      }
    }
  }
}
