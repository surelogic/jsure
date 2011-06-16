package com.surelogic.analysis.testing;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.JavaSemanticsVisitor;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaCaptureType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaIntersectionType;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.bind.IJavaWildcardType;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public final class TypesModule extends AbstractWholeIRAnalysis<TypesModule.TypesVisitor, Void> {
	public TypesModule() {
		super("BCACategory");
	}

	@Override
	protected TypesVisitor constructIRAnalysis(final IBinder binder) {
	  return new TypesVisitor(binder, this);
	}
  
  @Override
  protected void clearCaches() {
    // Nothing to do
  }

	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode compUnit) {
		runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
      public void run() {
				runOverFile(compUnit);
			}
		});
		return true;
	}

	protected void runOverFile(final IRNode compUnit) {
    getAnalysis().doAccept(compUnit);
	}	
	
  static final class TypesVisitor extends JavaSemanticsVisitor implements IBinderClient {
    private final IBinder binder;
//    private final TypesModule tm;
    
    public TypesVisitor(final IBinder b, final TypesModule tm) {
      super(true);
      this.binder = b;
//      this.tm = tm;
    }

    private void showType(final IRNode e) {
      final ISrcRef srcRef = JavaNode.getSrcRef(e);
      final IJavaType type = binder.getJavaType(e);
      System.out.printf("Expression '%s' at line %5d has type '%s', a %s%n",
          DebugUnparser.toString(e),
          (srcRef == null) ? 0 : srcRef.getLineNumber(),
          type.toString(), getClassName(type));
      showType("  ", type);
      System.out.println("--------------------------------------------------");
    }
    
    private void showType(final String prefix, final IJavaType type) {
      final String nextPrefix = prefix + "  ";
      if (type instanceof IJavaDeclaredType) {
        final IJavaDeclaredType declaredType = (IJavaDeclaredType) type;
        for (final IJavaType t2 : declaredType.getTypeParameters()) {
          System.out.printf("%sActual type parameter '%s', a %s%n",
              prefix, t2, getClassName(t2));
          showType(nextPrefix, t2);
        }
      }
      if (type instanceof IJavaTypeFormal) {
        final IJavaTypeFormal typeFormal = (IJavaTypeFormal) type;
        final IJavaType zuper = type.getSuperclass(binder.getTypeEnvironment());
        System.out.printf("%sSuperclass is '%s', a %s%n",
            prefix, zuper, getClassName(zuper));
        showType(nextPrefix, zuper);
        
        for (final IJavaType t2 : typeFormal.getSupertypes(binder.getTypeEnvironment())) {
          System.out.printf("%sSupertype (from iterator) '%s', a %s%n",
              prefix, t2, getClassName(t2));
          showType(nextPrefix, t2);
        }
      } else if (type instanceof IJavaCaptureType) {
        final IJavaCaptureType captureType = (IJavaCaptureType) type;
        final IJavaWildcardType wildcard = captureType.getWildcard();
        System.out.printf("%sFor wildcard '%s'%n", prefix, wildcard);
        
        IJavaReferenceType t2 = captureType.getUpperBound();
        if (t2 != null) {
        	System.out.printf("%sUpperbound '%s', a %s%n",
        			prefix, t2, getClassName(t2));
        	showType(nextPrefix, t2);
        }
        IJavaReferenceType t3 = captureType.getLowerBound();
        if (t3 != null) {
        	System.out.printf("%sUpperbound '%s', a %s%n",
        			prefix, t3, getClassName(t3));
        	showType(nextPrefix, t3);
        }
      } else if (type instanceof IJavaIntersectionType) {
        final IJavaIntersectionType intersectionType = (IJavaIntersectionType) type;
        final IJavaReferenceType primarySupertype = intersectionType.getPrimarySupertype();
        final IJavaReferenceType secondarySupertype = intersectionType.getSecondarySupertype();
        System.out.printf("%sPrimary supertype '%s', a %s%n",
            prefix, primarySupertype, getClassName(primarySupertype));
        showType(nextPrefix, primarySupertype);
        System.out.printf("%sSecondary supertype '%s', a %s%n",
            prefix, secondarySupertype, getClassName(secondarySupertype));
        showType(nextPrefix, secondarySupertype);
      }
    }
    
    private static String getClassName(final Object o) {
      final String name = o.getClass().getName();
      return name.substring(name.lastIndexOf('.') + 1);
    }
    
    @Override
    public Void visitExpression(final IRNode e) {
      showType(e); 
      return null;
    }
    
    @Override
    protected void handleAsMethodCall(final IRNode e) {
      showType(e);
    }

    public IBinder getBinder() {
      return binder;
    }

    public void clearCaches() {
      // do nothing
    }
  }
}
