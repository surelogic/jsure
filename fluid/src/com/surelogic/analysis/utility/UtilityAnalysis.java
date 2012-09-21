package com.surelogic.analysis.utility;

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
import com.surelogic.dropsea.ir.AnalysisHintDrop;
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
import edu.cmu.cs.fluid.util.Iteratable;

public final class UtilityAnalysis extends AbstractWholeIRAnalysis<UtilityAnalysis.UtilityVisitorFactory, TypeBodyPair> {	
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
	
	
	public UtilityAnalysis() {
		super(willRunInParallel, queueWork ? TypeBodyPair.class : null, "UtilityAssurance");
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
	public Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
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



  public static final class UtilityVisitor extends TypeImplementationProcessor<UtilityPromiseDrop> {
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
          createRootResult(false, newExpr, Messages.INSTANCE_CREATED);
        }
        doAcceptForChildren(newExpr);
        return null;
      }
      
      @Override
      public Void visitNestedClassDeclaration(final IRNode nestedClass) {
        final IRNode extendz = binder.getBinding(NestedClassDeclaration.getExtension(nestedClass));
        if (extendz.equals(typeDecl)) {
          createRootResult(false, nestedClass, Messages.SUBCLASSED);
        }
        doAcceptForChildren(nestedClass);
        return null;
      }
      
      @Override
      public Void visitAnonClassExpression(final IRNode anonClass) {
        final IRNode extendz = binder.getBinding(AnonClassExpression.getType(anonClass));
        if (extendz.equals(typeDecl)) {
          createRootResult(false, anonClass, Messages.INSTANCE_CREATED);
        }
        doAcceptForChildren(anonClass);
        return null;
      }
    }
    

    
    private final IBinder binder;
    private IRNode constructorDecl;
    private int numConstructors;


    
    protected UtilityVisitor(final UtilityAnalysis a,
        final UtilityPromiseDrop uDrop, final IBinder b,
        final IRNode classDecl, final IRNode classBody) {
      super(a, uDrop, classDecl, classBody);
      binder = b;
    }
   
    
    @Override
    protected void preProcess() {
      /* We already know that it must be a class declaration because scrubbing
       * does not allow the annotation to appear on interfaces.
       */
      
      // Prefer the class to be final
      if ((ClassDeclaration.getMods(typeDecl) & JavaNode.FINAL) == 0) {
        final AnalysisHintDrop db = AnalysisHintDrop.newWarning(typeDecl);
        db.setMessage(Messages.CONSIDER_FINAL);
      }
      
      // Class must be public
      final boolean isPublic = (ClassDeclaration.getMods(typeDecl) & JavaNode.PUBLIC) != 0;
      createRootResult(typeDecl, isPublic,
          Messages.CLASS_IS_PUBLIC, Messages.CLASS_IS_NOT_PUBLIC);

      constructorDecl = null;
      numConstructors = 0;
    }
    
    @Override
    protected void processVariableDeclarator(
        final IRNode fieldDecl, final IRNode varDecl, final boolean isStatic) {
      createRootResult(varDecl, isStatic, Messages.FIELD_IS_STATIC,
          Messages.FIELD_IS_NOT_STATIC, VariableDeclarator.getId(varDecl));
    }
    
    @Override
    protected void processMethodDeclaration(final IRNode mdecl) {
      createRootResult(mdecl, TypeUtil.isStatic(mdecl),
          Messages.METHOD_IS_STATIC, Messages.METHOD_IS_NOT_STATIC,
          JavaNames.genMethodConstructorName(mdecl));
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
        createRootResult(false, typeDecl, Messages.NO_CONSTRUCTOR);
      } else if (numConstructors > 1) {
        createRootResult(false, typeDecl, Messages.TOO_MANY_CONSTRUCTORS);
      } else {
        boolean good = true;
        if (Visibility.getVisibilityOf(constructorDecl) != Visibility.PRIVATE) {
          createRootResult(false, constructorDecl, Messages.CONSTRUCTOR_NOT_PRIVATE);
          good = false;
        }
        if (Parameters.getFormalIterator(ConstructorDeclaration.getParams(constructorDecl)).hasNext()) {
          createRootResult(false, constructorDecl, Messages.CONSTRUCTOR_BAD_ARGS);
          good = false;
        }
        if (good) {
          createRootResult(true, constructorDecl, Messages.PRIVATE_NO_ARG_CONSTRUCTOR);
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
          createRootResult(false, constructorDecl, Messages.CONSTRUCTOR_COMPILED);
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
              createRootResult(false, constructorDecl, Messages.CONSTRUCTOR_DOES_TOO_MUCH);            
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
              createRootResult(constructorDecl, !bad,
                  Messages.CONSTRUCTOR_THROWS_ASSERTION_ERROR, Messages.CONSTRUCTOR_DOES_TOO_MUCH);
            }
          } else {
            createRootResult(true, constructorDecl, Messages.CONSTRUCTOR_OKAY);
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
    
    
    
    public IBinder getBinder() {
      return binder;
    }

    public void clearCaches() {
      // do nothing
    }
    
    public UtilityVisitor getVisitor(final UtilityPromiseDrop uDrop,
        final IRNode classDecl, final IRNode classBody) {
      return new UtilityVisitor(
          UtilityAnalysis.this, uDrop, binder, classDecl, classBody);
    }
  }
}
