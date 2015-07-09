package com.surelogic.analysis.utility;

import java.util.*;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.ConcurrencyType;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.granules.IAnalysisGranulator;
import com.surelogic.analysis.visitors.TopLevelAnalysisVisitor;
import com.surelogic.analysis.visitors.TypeImplementationProcessor;
import com.surelogic.analysis.visitors.TopLevelAnalysisVisitor.TypeBodyPair;
import com.surelogic.annotation.rules.UtilityRules;
import com.surelogic.common.concurrent.Procedure;
import com.surelogic.common.util.*;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.UtilityPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.BlockStatement;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodBody;
import edu.cmu.cs.fluid.java.operator.NestedClassDeclaration;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.ThrowStatement;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.Visibility;

public final class UtilityAnalysis extends AbstractWholeIRAnalysis<UtilityAnalysis.UtilityVisitorFactory, TypeBodyPair> {	
  private static final int CLASS_IS_PUBLIC = 600;
  private static final int CLASS_IS_NOT_PUBLIC = 601;
  private static final int FIELD_IS_STATIC = 606;
  private static final int FIELD_IS_NOT_STATIC = 607;
  private static final int METHOD_IS_STATIC = 608;
  private static final int METHOD_IS_NOT_STATIC = 609;
  private static final int NO_CONSTRUCTOR = 610;
  private static final int TOO_MANY_CONSTRUCTORS = 611;
  private static final int CONSTRUCTOR_NOT_PRIVATE = 612;
  private static final int CONSTRUCTOR_BAD_ARGS = 613;
  private static final int PRIVATE_NO_ARG_CONSTRUCTOR = 614;
  private static final int CONSTRUCTOR_DOES_TOO_MUCH = 615;
  private static final int CONSTRUCTOR_OKAY = 616;
  private static final int CONSTRUCTOR_THROWS_ASSERTION_ERROR = 617;
  private static final int INSTANCE_CREATED = 618;
  private static final int SUBCLASSED = 619;
  private static final int CONSIDER_FINAL = 620;
  private static final int CONSTRUCTOR_COMPILED = 621;

  
  
  /** Should we try to run things in parallel */
  private static boolean wantToRunInParallel = false;
  
  /**
   * Are we actually going to run things in parallel?  Not all JRE have the
   * libraries we need to actually run in parallel.
   */
  private static boolean willRunInParallel = wantToRunInParallel;
  
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
	
	
	public UtilityAnalysis() {
		super(willRunInParallel, "UtilityAssurance");
		if (runInParallel() == ConcurrencyType.INTERNALLY) {
			setWorkProcedure(new Procedure<TypeBodyPair>() {
				@Override
        public void op(final TypeBodyPair n) {
					if (byCompUnit) {
					  TopLevelAnalysisVisitor.processCompilationUnit(
		            // actually n.typeDecl is a CompilationUnit here!
					      new ClassProcessor(getAnalysis()), n.getType());
					} else {
					  actuallyAnalyzeClassBody(getAnalysis(), n.getType(), n.getClassBody());
					}
				}
			});
		}      
	}
	
	
	private static void actuallyAnalyzeClassBody(final UtilityVisitorFactory f,
	    final IRNode classDecl, final IRNode classBody) {
	  final UtilityPromiseDrop uDrop = UtilityRules.getUtilityDrop(classDecl);
	  if (uDrop != null) {
	    final UtilityVisitor uv = f.getVisitor(uDrop, classDecl, classBody);
	    uv.processType();
	  }
	}

	@Override
	protected boolean flushAnalysis() {
		return true;
	}
	
	@Override
	protected UtilityVisitorFactory constructIRAnalysis(final IBinder binder) {		
	  if (binder == null || binder.getTypeEnvironment() == null) {
		  return null;
	  }
	  return new UtilityVisitorFactory(binder);
	}
	
	@Override
	protected void clearCaches() {
		if (runInParallel() != ConcurrencyType.INTERNALLY) {
			final UtilityVisitorFactory lv = getAnalysis();
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
	public IAnalysisGranulator<TypeBodyPair> getGranulator() {
		return TopLevelAnalysisVisitor.granulator;
	}
	
	@Override
	protected boolean doAnalysisOnGranule_wrapped(IIRAnalysisEnvironment env, TypeBodyPair n) {
		actuallyAnalyzeClassBody(getAnalysis(), n.getType(), n.getClassBody());
		return true; 
	}
	
	@Override
	public Iterable<TypeBodyPair> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
		finishBuild();
		return super.analyzeEnd(env, p);
	}
	
	
	
	private final class ClassProcessor extends TopLevelAnalysisVisitor.SimpleClassProcessor {
    private final UtilityVisitorFactory factory;
    private final List<TypeBodyPair> types = new ArrayList<TypeBodyPair>();
    
    public ClassProcessor(final UtilityVisitorFactory f) {
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



  public static final class UtilityVisitor extends TypeImplementationProcessor {
    /**
     * Visits the entire subtree (including nested types and anonymous classes)
     * to search for new expressions and class declarations that may
     * create instances of the utility class.
     */
    private final class BodyVisitor extends VoidTreeWalkVisitor {
      public BodyVisitor() {
        super();
      }
      
      
      
      @Override
      public Void visitNewExpression(final IRNode newExpr) {
        final IRNode clazz = binder.getBinding(NewExpression.getType(newExpr));
        if (clazz.equals(typeDecl)) {
          builder.createRootResult(false, newExpr, INSTANCE_CREATED);
        }
        doAcceptForChildren(newExpr);
        return null;
      }
      
      @Override
      public Void visitNestedClassDeclaration(final IRNode nestedClass) {
        final IRNode extendz = binder.getBinding(NestedClassDeclaration.getExtension(nestedClass));
        if (extendz.equals(typeDecl)) {
          builder.createRootResult(false, nestedClass, SUBCLASSED);
        }
        doAcceptForChildren(nestedClass);
        return null;
      }
      
      @Override
      public Void visitAnonClassExpression(final IRNode anonClass) {
        final IRNode extendz = binder.getBinding(AnonClassExpression.getType(anonClass));
        if (extendz.equals(typeDecl)) {
          builder.createRootResult(false, anonClass, INSTANCE_CREATED);
        }
        doAcceptForChildren(anonClass);
        return null;
      }
    }
    

    
    private final ResultsBuilder builder;
    private IRNode constructorDecl;
    private int numConstructors;


    
    protected UtilityVisitor(
        final UtilityPromiseDrop uDrop, final IBinder b,
        final IRNode classDecl, final IRNode classBody) {
      super(b, classDecl, classBody);
      builder = new ResultsBuilder(uDrop);
    }
   
    
    @Override
    protected void preProcess() {
      /* We already know that it must be a class declaration because scrubbing
       * does not allow the annotation to appear on interfaces.
       */
      
      // Prefer the class to be final
      if ((ClassDeclaration.getMods(typeDecl) & JavaNode.FINAL) == 0) {
        final HintDrop db = HintDrop.newWarning(typeDecl);
        db.setMessage(CONSIDER_FINAL);
      }
      
      // Class must be public
      final boolean isPublic = (ClassDeclaration.getMods(typeDecl) & JavaNode.PUBLIC) != 0;
      builder.createRootResult(typeDecl, isPublic,
          CLASS_IS_PUBLIC, CLASS_IS_NOT_PUBLIC);

      constructorDecl = null;
      numConstructors = 0;
    }
    
    @Override
    protected void processVariableDeclarator(
        final IRNode fieldDecl, final IRNode varDecl, final boolean isStatic) {
      builder.createRootResult(varDecl, isStatic, FIELD_IS_STATIC,
          FIELD_IS_NOT_STATIC, VariableDeclarator.getId(varDecl));
    }
    
    @Override
    protected void processMethodDeclaration(final IRNode mdecl) {
      builder.createRootResult(mdecl, TypeUtil.isStatic(mdecl),
          METHOD_IS_STATIC, METHOD_IS_NOT_STATIC,
          JavaNames.genRelativeFunctionName(mdecl));
    }
    
    @Override
    protected void processConstructorDeclaration(final IRNode cdecl) {
      // ignore the implicit constructor
      if (!JavaNode.wasImplicit(cdecl)) {
        constructorDecl = cdecl;
        numConstructors += 1;
      }
    }
    
    @Override
    protected void postProcess() {
      if (numConstructors == 0) {
        builder.createRootResult(false, typeDecl, NO_CONSTRUCTOR);
      } else if (numConstructors > 1) {
        builder.createRootResult(false, typeDecl, TOO_MANY_CONSTRUCTORS);
      } else {
        boolean good = true;
        if (Visibility.getVisibilityOf(constructorDecl) != Visibility.PRIVATE) {
          builder.createRootResult(false, constructorDecl, CONSTRUCTOR_NOT_PRIVATE);
          good = false;
        }
        if (Parameters.getFormalIterator(ConstructorDeclaration.getParams(constructorDecl)).hasNext()) {
          builder.createRootResult(false, constructorDecl, CONSTRUCTOR_BAD_ARGS);
          good = false;
        }
        if (good) {
          builder.createRootResult(true, constructorDecl, PRIVATE_NO_ARG_CONSTRUCTOR);
        }
        
        /* Constructor must be one of 
         * 
         *   private C() {
         *     super();
         *   }
         * 
         * or
         * 
         *   private C() {
         *     super();
         *     throw new AssertionError();
         *   }
         */
        final IRNode body = ConstructorDeclaration.getBody(constructorDecl);
        if (!MethodBody.prototype.includes(body)) {
          builder.createRootResult(false, constructorDecl, CONSTRUCTOR_COMPILED);
        } else {
          final Iteratable<IRNode> stmts =
              BlockStatement.getStmtIterator(MethodBody.getBlock(body));
          /* First statement must be "super(...)".  Cannot be "this(...)" because
           * we know there is exactly one constructor.  Grab it and skip it.
           */
          @SuppressWarnings("unused")
          final IRNode superCall = stmts.next(); // Eat the super call
          if (stmts.hasNext()) {
            final IRNode stmt = stmts.next();
            if (stmts.hasNext()) {
              // Has more than 2 statements, definitely bad
              builder.createRootResult(false, constructorDecl, CONSTRUCTOR_DOES_TOO_MUCH);            
            } else {
              boolean bad = true;
              // Check for a Throws statement
              if (ThrowStatement.prototype.includes(stmt)) {
                final IRNode thrown = ThrowStatement.getValue(stmt);
                if (NewExpression.prototype.includes(thrown)) {
                  final IRNode type = NewExpression.getType(thrown);
                  if (JavaNames.getFullTypeName(binder.getBinding(type)).equals("java.lang.AssertionError")) {
                    bad = false;
                  }
                }
              }
              builder.createRootResult(constructorDecl, !bad,
                  CONSTRUCTOR_THROWS_ASSERTION_ERROR, CONSTRUCTOR_DOES_TOO_MUCH);
            }
          } else {
            builder.createRootResult(true, constructorDecl, CONSTRUCTOR_OKAY);
          }
        }
      }
      
      // Check for class instantiation and extension
      new BodyVisitor().doAccept(typeBody);
    }
    
    public IBinder getBinder() {
      return binder;
    }

    public void clearCaches() {
      // do nothing
    }
    
  }


  
  public final class UtilityVisitorFactory implements IBinderClient {
    private final IBinder binder;
    
    public UtilityVisitorFactory(final IBinder b) {
      binder = b;
    }
    
    
    
    @Override
    public IBinder getBinder() {
      return binder;
    }

    @Override
    public void clearCaches() {
      // do nothing
    }
    
    public UtilityVisitor getVisitor(final UtilityPromiseDrop uDrop,
        final IRNode classDecl, final IRNode classBody) {
      return new UtilityVisitor(uDrop, binder, classDecl, classBody);
    }
  }
}
