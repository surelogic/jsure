package com.surelogic.analysis.utility;

import java.util.*;

import jsr166y.forkjoin.Ops.Procedure;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.TopLevelAnalysisVisitor;
import com.surelogic.annotation.rules.UtilityRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.BlockStatement;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.Implements;
import edu.cmu.cs.fluid.java.operator.MethodBody;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.ThrowStatement;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.Visibility;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.promises.UtilityPromiseDrop;
import edu.cmu.cs.fluid.sea.proxy.InfoDropBuilder;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Iteratable;

public final class UtilityAnalysis extends AbstractWholeIRAnalysis<UtilityAnalysis.UtilityVisitor, UtilityAnalysis.Pair> {	
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
		super(willRunInParallel, queueWork ? Pair.class : null, "UtilityAssurance");
		if (runInParallel()) {
			setWorkProcedure(new Procedure<Pair>() {
				public void op(Pair n) {
					if (byCompUnit) {
					  final TopLevelAnalysisVisitor topLevel = 
					    new TopLevelAnalysisVisitor(
					        new ClassProcessor(getAnalysis(), getResultDependUponDrop()));
					  // actually n.typeDecl is a CompilationUnit here!
						topLevel.doAccept(n.typeDecl);	
					} else {
					  actuallyAnalyzeClassBody(
					      getAnalysis(),getResultDependUponDrop(),
					      n.typeDecl, n.classBody);
					}
				}
			});
		}      
	}
	
	
	private final void actuallyAnalyzeClassBody(
	    final UtilityVisitor uv, final Drop rd, 
	    final IRNode typeDecl, final IRNode classBody) {
	  final UtilityPromiseDrop uDrop = UtilityRules.getUtilityDrop(typeDecl);
	  if (uDrop != null) {
	    uv.assureClass(typeDecl, classBody, uDrop);
	  }
	}

	@Override
	protected boolean flushAnalysis() {
		return true;
	}
	
	@Override
	protected UtilityVisitor constructIRAnalysis(final IBinder binder) {		
	  if (binder == null || binder.getTypeEnvironment() == null) {
		  return null;
	  }
	  return new UtilityVisitor(this, binder);
	}
	
	@Override
	protected void clearCaches() {
		if (!runInParallel()) {
			final UtilityVisitor lv = getAnalysis();
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
			queueWork(new Pair(compUnit, null));
			return true;
		}
		// FIX factor out?
		final ClassProcessor cp = new ClassProcessor(getAnalysis(), getResultDependUponDrop());
		new TopLevelAnalysisVisitor(cp).doAccept(compUnit);
		if (runInParallel()) {
			if (queueWork) {
        queueWork(cp.getTypeBodies());
			} else {
        runInParallel(Pair.class, cp.getTypeBodies(), getWorkProcedure());
			}
		}
		return true;
	}
	
	@Override
	public Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
		finishBuild();
		return super.analyzeEnd(env, p);
	}
	
	
	
	protected final class Pair {
	  public final IRNode typeDecl;
	  public final IRNode classBody;
	  
	  public Pair(final IRNode td, final IRNode cb) {
	    typeDecl = td;
	    classBody = cb;
	  }
	}
	
	
	
	private final class ClassProcessor extends TopLevelAnalysisVisitor.SimpleClassProcessor {
    private final UtilityVisitor utilityVisitor;
    private final Drop resultsDependUpon;
    private final List<Pair> types = new ArrayList<Pair>();
    
    public ClassProcessor(final UtilityVisitor uv, final Drop rd) {
      utilityVisitor = uv;
      resultsDependUpon = rd;
    }

    public Collection<Pair> getTypeBodies() {
      return types;
    }
    
    @Override
    protected void visitTypeDecl(final IRNode typeDecl, final IRNode classBody) {
      if (runInParallel() && !byCompUnit) {
        types.add(new Pair(typeDecl, classBody));
      } else {
        actuallyAnalyzeClassBody(
            utilityVisitor, resultsDependUpon, typeDecl, classBody);
      }
    }
	}



  public static final class UtilityVisitor implements IBinderClient {
    private final UtilityAnalysis analysis;
    private final IBinder binder;


    
    protected UtilityVisitor(final UtilityAnalysis a, final IBinder b) {
      analysis = a;
      binder = b;
    }

    
    
    private final void createResult(
        final UtilityPromiseDrop uDrop,
        final IRNode decl, final boolean isConsistent, 
        final int msg, final Object... args) {
      final ResultDropBuilder result =
        ResultDropBuilder.create(analysis, Messages.toString(msg));
      analysis.setResultDependUponDrop(result, decl);
      result.addCheckedPromise(uDrop);
      result.setConsistent(isConsistent);
      result.setResultMessage(msg, args);
    }

    
    
    public void assureClass(
        final IRNode classDecl, final IRNode classBody, final UtilityPromiseDrop drop) {
      /* We already know that it must be a class declaration because scrubbing
       * does not allow the annotation to appear on interfaces.
       */
      
      // Prefer the class to be final
      if ((ClassDeclaration.getMods(classDecl) & JavaNode.FINAL) == 0) {
        final InfoDropBuilder db =
          InfoDropBuilder.create(analysis, Messages.toString(Messages.CONSIDER_FINAL), true);
        analysis.setResultDependUponDrop(db, classDecl);
        db.setResultMessage(Messages.CONSIDER_FINAL);
      }
      
      // Class must be public
      if ((ClassDeclaration.getMods(classDecl) & JavaNode.PUBLIC) != 0) {
        createResult(drop, classDecl, true, Messages.CLASS_IS_PUBLIC);
      } else {
        createResult(drop, classDecl, false, Messages.CLASS_IS_NOT_PUBLIC);
      }
      
      // Class must extend java.lang.Object
      final IRNode superDecl =
        binder.getBinding(ClassDeclaration.getExtension(classDecl));
      if (JavaNames.getQualifiedTypeName(superDecl).equals("java.lang.Object")) {
        createResult(drop, classDecl, true, Messages.CLASS_EXTENDS_OBJECT);
      } else {
        createResult(drop, classDecl, false, Messages.CLASS_DOES_NOT_EXTEND_OBJECT);
      }
      
      // Class must not implement any interfaces
      final Iterator<IRNode> interfaces =
        Implements.getIntfIterator(ClassDeclaration.getImpls(classDecl));
      if (!interfaces.hasNext()) {
        createResult(drop, classDecl, true, Messages.CLASS_IMPLEMENTS_NOTHING);
      } else {
        createResult(drop, classDecl, false, Messages.CLASS_IMPLEMENTS_SOMETHING);
      }
      
      IRNode constructorDecl = null;
      int numConstructors = 0;
      for (final IRNode bodyDecl : ClassBody.getDeclIterator(classBody)) {
        final Operator op = JJNode.tree.getOperator(bodyDecl);
        if (FieldDeclaration.prototype.includes(op)) {
          if (TypeUtil.isStatic(bodyDecl)) {
            for (final IRNode field : VariableDeclarators.getVarIterator(FieldDeclaration.getVars(bodyDecl))) {
              createResult(drop, field, true, Messages.FIELD_IS_STATIC, 
                  VariableDeclarator.getId(field));
            }
          } else {
            for (final IRNode field : VariableDeclarators.getVarIterator(FieldDeclaration.getVars(bodyDecl))) {
              createResult(drop, field, false, Messages.FIELD_IS_NOT_STATIC, 
                  VariableDeclarator.getId(field));
            }
          }
        } else if (MethodDeclaration.prototype.includes(op)) {
          if (TypeUtil.isStatic(bodyDecl)) {
            createResult(drop, bodyDecl, true, Messages.METHOD_IS_STATIC,
                JavaNames.genMethodConstructorName(bodyDecl));
          } else {
            createResult(drop, bodyDecl, false, Messages.METHOD_IS_NOT_STATIC,
                JavaNames.genMethodConstructorName(bodyDecl));
          }
        } else if (ConstructorDeclaration.prototype.includes(op)) {
          // ignore the implicit constructor
          if (!JavaNode.wasImplicit(bodyDecl)) {
            constructorDecl = bodyDecl;
            numConstructors += 1;
          }
        }
      }
      
      if (numConstructors == 0) {
        createResult(drop, classDecl, false, Messages.NO_CONSTRUCTOR);
      } else if (numConstructors > 1) {
        createResult(drop, classDecl, false, Messages.TOO_MANY_CONSTRUCTORS);
      } else {
        boolean good = true;
        if (Visibility.getVisibilityOf(constructorDecl) != Visibility.PRIVATE) {
          createResult(drop, constructorDecl, false, Messages.CONSTRUCTOR_NOT_PRIVATE);
          good = false;
        }
        if (Parameters.getFormalIterator(ConstructorDeclaration.getParams(constructorDecl)).hasNext()) {
          createResult(drop, constructorDecl, false, Messages.CONSTRUCTOR_BAD_ARGS);
          good = false;
        }
        if (good) {
          createResult(drop, constructorDecl, true, Messages.PRIVATE_NO_ARG_CONSTRUCTOR);
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
        final Iteratable<IRNode> stmts = BlockStatement.getStmtIterator(
            MethodBody.getBlock(
                ConstructorDeclaration.getBody(constructorDecl)));
        /* First statement must be "super(...)".  Cannot be "this(...)" because
         * we know there is exactly one constructor.  Grab it and skip it.
         */
        @SuppressWarnings("unused")
        final IRNode superCall = stmts.next();
        if (stmts.hasNext()) {
          final IRNode stmt = stmts.next();
          if (stmts.hasNext()) {
            // Has more than 2 statements, definitely bad
            createResult(drop, constructorDecl, false, Messages.CONSTRUCTOR_DOES_TOO_MUCH);            
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
            if (bad) {
              createResult(drop, constructorDecl, false, Messages.CONSTRUCTOR_DOES_TOO_MUCH);
            } else {
              createResult(drop, constructorDecl, true, Messages.CONSTRUCTOR_THROWS_ASSERTION_ERROR);
            }
          }
        } else {
          createResult(drop, constructorDecl, true, Messages.CONSTRUCTOR_OKAY);
        }
      }
    }    
    
    public IBinder getBinder() {
      return binder;
    }

    public void clearCaches() {
      // do nothing
    }
    
  }
}
