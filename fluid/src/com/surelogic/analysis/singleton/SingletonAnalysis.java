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
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.typeAnnos.SingletonPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.BlockStatement;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.Implements;
import edu.cmu.cs.fluid.java.operator.MethodBody;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.ReturnStatement;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.util.Iteratable;

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
	    sv.verifyDeclaration(sDrop, classDecl, classBody);
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
    
    
    
    public void verifyDeclaration(final SingletonPromiseDrop sDrop,
        final IRNode typeDecl, final IRNode typeBody) {
      if (EnumDeclaration.prototype.includes(typeDecl)) {
        new EnumVerifier(
            SingletonAnalysis.this, sDrop, typeDecl, typeBody).processType();
      } else {
        new ClassVerifier(binder,
            SingletonAnalysis.this, sDrop, typeDecl, typeBody).processType();
      }
    }
  }
  
  
  
  private static final class EnumVerifier extends TypeImplementationProcessor<SingletonPromiseDrop> {
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
  
  
  
  private static final class ClassVerifier extends TypeImplementationProcessor<SingletonPromiseDrop> {
    private final class BodyVisitor extends VoidTreeWalkVisitor {
      private final IRNode fieldDeclToCheck;
      
      
      
      public BodyVisitor(final boolean publicPattern, final boolean privatePattern) { 
        // Only check the field if we have exactly one public static final field
        fieldDeclToCheck = 
            publicPattern ? publicStaticFinalField : 
              (privatePattern ? privateStaticFinalField : null);
      }
      
      
      
      @Override
      public Void visitNewExpression(final IRNode newExpr) {
        final IRNode clazz = binder.getBinding(NewExpression.getType(newExpr));
        if (clazz.equals(typeDecl)) {
          /* Found a new C(), where C() is the annotated class.  Need to check
           * that we are in the initializer of the one and only INSTANCE field.  
           */
          if (JJNode.tree.getParentOrNull(
              JJNode.tree.getParentOrNull(newExpr)) == fieldDeclToCheck) {
            createResult(newExpr, true, Messages.GOOD_CREATION);
          } else {
            createResult(newExpr, false, Messages.EXTRA_CREATION);
          }
        }
        doAcceptForChildren(newExpr);
        return null;
      }
    }

    
    
    private final IBinder binder;
    private final IJavaType javaType; 
    
    private boolean isSerializable = false;
    private boolean hasReadResolve = false;
    private IRNode readResolveReturnExpr = null;
    
    private int numPublicStaticFinalFields = 0;
    private int numPrivateStaticFinalFields = 0;
    private int numFields = 0;
    private IRNode publicStaticFinalField = null;
    private IRNode privateStaticFinalField = null;
    private int numGetters = 0;
    
    
    
    public ClassVerifier(final IBinder b,
        AbstractWholeIRAnalysis<? extends IBinderClient, ?> a,
        SingletonPromiseDrop pd, IRNode classDecl, IRNode classBody) {
      super(a, pd, classDecl, classBody);
      binder = b;
      javaType = binder.getTypeEnvironment().getMyThisType(classDecl);
    }

    
    
   
    private boolean isPublicStatic(final IRNode decl) {
      return JavaNode.getModifier(decl, JavaNode.STATIC) &&
          JavaNode.getModifier(decl, JavaNode.PUBLIC);
    }
    
    private IRNode getGrandParent(final IRNode n) {
      return JJNode.tree.getParentOrNull(JJNode.tree.getParentOrNull(n));
    }
    
    private IRNode implementsSerializable() {
      for (final IRNode i : Implements.getIntfIterator(ClassDeclaration.getImpls(typeDecl))) {
        final IRNode iDecl = binder.getBinding(i);
        if (JavaNames.getFullTypeName(iDecl).equals("java.io.Serializable")) {
          return i;
        }
      }
      return null;
    }
    
    
    @Override
    protected void preProcess() {
      if (JavaNode.getModifier(typeDecl, JavaNode.FINAL)) {
        createResult(typeDecl, true, Messages.CLASS_IS_FINAL);
      } else {
        createResult(typeDecl, false, Messages.CLASS_NOT_FINAL);        
      }
      
      isSerializable = implementsSerializable() != null;
    }
    
    @Override
    protected void processConstructorDeclaration(final IRNode cdecl) {
      final String id = JavaNames.genSimpleMethodConstructorName(cdecl);
      if (JavaNode.getModifier(cdecl, JavaNode.PRIVATE)) {
        createResult(cdecl, true, Messages.CONSTRUCTOR_IS_PRIVATE, id);
      } else {
        createResult(cdecl, false, Messages.CONSTRUCTOR_NOT_PRIVATE, id);
      }
    }
    
    @Override
    protected void processMethodDeclaration(final IRNode mdecl) {
      final IRNode body = MethodDeclaration.getBody(mdecl);
      final IRNode rtBound = binder.getBinding(MethodDeclaration.getReturnType(mdecl));
      
      if (!MethodBody.prototype.includes(body)) {
        createResult(mdecl, false, Messages.CLASS_METHOD_COMPILED);
      } else { 
        // Are we the readResolve() method?
        if (!JavaNode.getModifier(mdecl, JavaNode.STATIC) &&
            MethodDeclaration.getId(mdecl).equals("readResolve") &&
            rtBound != null && JavaNames.getQualifiedTypeName(rtBound).equals("java.lang.Object") &&
            !Parameters.getFormalIterator(MethodDeclaration.getParams(mdecl)).hasNext()) {
          hasReadResolve = true;
          final Iteratable<IRNode> stmts =
              BlockStatement.getStmtIterator(MethodBody.getBlock(body));
          // Method has a return type, so it must have at least one (return) statement.
          final IRNode firstStmt = stmts.next();
          if (!stmts.hasNext()) { // has exactly 1 stmt
            // check for return of static field
            if (ReturnStatement.prototype.includes(firstStmt)) {
              final IRNode expr = ReturnStatement.getValue(firstStmt);
              readResolveReturnExpr = expr;
            } else {
              // No return
              createResult(body, false, Messages.BAD_READ_RESOLVE_BODY);
            }
          } else {
            // > 1 statement
            createResult(body, false, Messages.BAD_READ_RESOLVE_BODY);
          }
        }
        
        if (isPublicStatic(mdecl) && typeDecl.equals(rtBound)) {
          final Iteratable<IRNode> stmts =
              BlockStatement.getStmtIterator(MethodBody.getBlock(body));
          if (stmts.hasNext()) { // has at least 1 stmt
            final IRNode firstStmt = stmts.next();
            if (!stmts.hasNext()) { // has exactly 1 stmt
              // check for return of static field
              if (ReturnStatement.prototype.includes(firstStmt)) {
                final IRNode expr = ReturnStatement.getValue(firstStmt);
                if (FieldRef.prototype.includes(expr)) {
                  final IRNode fdecl = getGrandParent(binder.getBinding(expr));
                  if (JavaNode.getModifier(fdecl, JavaNode.STATIC)) {
                    // Returns a static field, but from what class?
                    if (typeDecl.equals(getGrandParent(fdecl))) {
                      numGetters += 1;
                      createResult(mdecl, true, Messages.CLASS_FOUND_GETTER,
                          JavaNames.genSimpleMethodConstructorName(mdecl));
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    
    @Override
    protected void processVariableDeclarator(
        final IRNode fieldDecl, final IRNode varDecl, final boolean isStatic) {
      if (javaType.equals(binder.getJavaType(varDecl))) {
        numFields += 1;
        if (isStatic && JavaNode.getModifier(fieldDecl, JavaNode.FINAL)) {
          if (JavaNode.getModifier(fieldDecl, JavaNode.PUBLIC)) {
            numPublicStaticFinalFields += 1;
            publicStaticFinalField = varDecl;
          } else if (JavaNode.getModifier(fieldDecl, JavaNode.PRIVATE)) {
            numPrivateStaticFinalFields += 1;
            privateStaticFinalField = varDecl;
          }
        }
      }
      
      if (isSerializable && !isStatic) {
        if (JavaNode.getModifier(fieldDecl, JavaNode.TRANSIENT)) {
          createResult(varDecl, true, Messages.FIELD_IS_TRANSIENT);
        } else {
          createResult(varDecl, false, Messages.FIELD_NOT_TRANSIENT);
        }
      }
    }
    
    @Override
    protected void postProcess() {
      // Sort out the field declarations here
      
      /* Must have exactly one field declaration of type C, where C is the 
       * annotated type.  That declaration must be either "public static final"
       * or "private static final".  If we have any other fields of type C
       * it is an error.
       */
      final boolean publicFieldPattern =
          numFields == 1 && numPublicStaticFinalFields == 1;
      final boolean privateFieldPattern =
          numFields == 1 && numPrivateStaticFinalFields == 1;
      final String typeString = javaType.toString();
      
      final IRNode singletonField;
      if (publicFieldPattern) {
        singletonField = publicStaticFinalField;
        createResult(publicStaticFinalField, true,
            Messages.CLASS_ONE_PUBLIC_FIELD, typeString,
            VariableDeclarator.getId(publicStaticFinalField));
      } else if (privateFieldPattern) {
        singletonField = privateStaticFinalField;
        createResult(privateStaticFinalField, true,
            Messages.CLASS_ONE_PRIVATE_FIELD, typeString,
            VariableDeclarator.getId(privateStaticFinalField));
      } else {
        singletonField = null;
        if (numPublicStaticFinalFields == 0 && numPrivateStaticFinalFields == 0) {
          createResult(typeDecl, false, Messages.CLASS_NO_PUBLIC_FIELD, typeString);
          createResult(typeDecl, false, Messages.CLASS_NO_PRIVATE_FIELD, typeString);
        }
        if (numFields > 1) {
          createResult(typeDecl, false, Messages.CLASS_TOO_MANY, typeString);
        }
      }
      
      // Check for new expressions
      new BodyVisitor(publicFieldPattern, privateFieldPattern).doAccept(typeBody);
      
      // Check for getter methods
      if (privateFieldPattern && numGetters == 0) {
        createResult(typeDecl, false, Messages.CLASS_NO_GETTER);
      }
      
      // Check the return value of the readResolve() method, if any
      if (isSerializable) {
        if (!hasReadResolve) {
          createResult(typeDecl, false, Messages.NO_READ_RESOLVE);
        }
        if (readResolveReturnExpr != null) {
          if (FieldRef.prototype.includes(readResolveReturnExpr)) {
            final IRNode fdecl = binder.getBinding(readResolveReturnExpr);
            if (fdecl.equals(singletonField)) {
              createResult(readResolveReturnExpr, true, Messages.READ_RESOLVE_GOOD,
                  VariableDeclarator.getId(singletonField));
            } else {
              // Wrong field
              createResult(readResolveReturnExpr, false, Messages.READ_RESOLVE_BAD,
                  VariableDeclarator.getId(singletonField));
            }
          } else {
            // Not a field value at all
            createResult(readResolveReturnExpr, false, Messages.READ_RESOLVE_BAD,
                VariableDeclarator.getId(singletonField));
          }
        }
      }
    }
  }
}
