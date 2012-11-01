package com.surelogic.analysis.type.constraints;

import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.promise.AnnotationBoundsNode;
import com.surelogic.annotation.rules.EqualityRules;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProofDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.AnnotationBoundsPromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ContainablePromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.operator.EnumDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeFormal;
import edu.cmu.cs.fluid.parse.JJNode;

public enum TypeAnnotations {
  CONTAINABLE {
    @Override
    public TypeTester forFieldDeclaration() { return ContainableTesters.FOR_FIELD_DECLARATIONS; }
    @Override
    public TypeTester forFinalObject() { return ContainableTesters.FOR_FINAL_OBJECT; }
    @Override
    public TypeTester forParameterizedTypeActual() { return ContainableTesters.FOR_PARAMETERIZED_TYPE_ACTUAL; }
  },
  
  IMMUTABLE {
    @Override
    public TypeTester forFieldDeclaration() { return ImmutableTesters.FOR_FIELD_DECLARATIONS; }
    @Override
    public TypeTester forFinalObject() { return ImmutableTesters.FOR_FINAL_OBJECT; }
    @Override
    public TypeTester forParameterizedTypeActual() { return ImmutableTesters.FOR_PARAMETERIZED_TYPE_ACTUAL; }
  },
  
  REFERENCE_OBJECT {
    @Override
    public TypeTester forFieldDeclaration() { return ReferenceObjectTesters.FOR_FIELD_DECLARATIONS; }
    @Override
    public TypeTester forFinalObject() { return ReferenceObjectTesters.FOR_FINAL_OBJECT; }
    @Override
    public TypeTester forParameterizedTypeActual() { return ReferenceObjectTesters.FOR_PARAMETERIZED_TYPE_ACTUAL; }
  },
  
  THREAD_SAFE {
    @Override
    public TypeTester forFieldDeclaration() { return ThreadSafeTesters.FOR_FIELD_DECLARATIONS; }
    @Override
    public TypeTester forFinalObject() { return ThreadSafeTesters.FOR_FINAL_OBJECT; }
    @Override
    public TypeTester forParameterizedTypeActual() { return ThreadSafeTesters.FOR_PARAMETERIZED_TYPE_ACTUAL; }
  },
  
  VALUE_OBJECT {
    @Override
    public TypeTester forFieldDeclaration() { return ValueObjectTesters.FOR_FIELD_DECLARATIONS; }
    @Override
    public TypeTester forFinalObject() { return ValueObjectTesters.FOR_FINAL_OBJECT; }
    @Override
    public TypeTester forParameterizedTypeActual() { return ValueObjectTesters.FOR_PARAMETERIZED_TYPE_ACTUAL; }
  };
  
  
  
  // ----------------------------------------------------------------------
  // Methods for the enumeration elements
  // ----------------------------------------------------------------------

  public abstract TypeTester forFieldDeclaration();
  public abstract TypeTester forFinalObject();
  public abstract TypeTester forParameterizedTypeActual();

  
  
  // ----------------------------------------------------------------------
  // Methods for testing the annotations placed on a type formal
  // ----------------------------------------------------------------------

  private enum TypeAnnoImpl {
    CONTAINABLE {
      @Override
      public NamedTypeNode[] getAnnotatedFormals(final AnnotationBoundsNode abNode) {
        return abNode.getContainable();
      }
      
      @Override
      public boolean testAgainstImpliedContainableBounds(final ContainablePromiseDrop cDrop) {
        return false;
      }
    },
    
    IMMUTABLE {
      @Override
      public NamedTypeNode[] getAnnotatedFormals(final AnnotationBoundsNode abNode) {
        return abNode.getImmutable();
      }
      
      @Override
      public boolean testAgainstImpliedContainableBounds(final ContainablePromiseDrop cDrop) {
        return false;
      }
    },
    
    REFERENCE {
      @Override
      public NamedTypeNode[] getAnnotatedFormals(final AnnotationBoundsNode abNode) {
        return abNode.getReference();
      }
      
      @Override
      public boolean testAgainstImpliedContainableBounds(final ContainablePromiseDrop cDrop) {
        return cDrop != null && cDrop.allowReferenceObject();
      }
    },
    
    THREADSAFE {
      @Override
      public NamedTypeNode[] getAnnotatedFormals(final AnnotationBoundsNode abNode) {
        return abNode.getThreadSafe();
      }
      
      @Override
      public boolean testAgainstImpliedContainableBounds(final ContainablePromiseDrop cDrop) {
        return cDrop != null;
      }
    },
      
    VALUE {
      @Override
      public NamedTypeNode[] getAnnotatedFormals(final AnnotationBoundsNode abNode) {
        return abNode.getValue();
      }
      
      @Override
      public boolean testAgainstImpliedContainableBounds(final ContainablePromiseDrop cDrop) {
        return false;
      }
    };
    
    public abstract NamedTypeNode[] getAnnotatedFormals(AnnotationBoundsNode abNode);
    
    public abstract boolean testAgainstImpliedContainableBounds(ContainablePromiseDrop cDrop);
  }

  static PromiseDrop<?> testTypeFormalForX(final TypeAnnoImpl testFor,
      final IJavaTypeFormal formal, final boolean exclusively) {
    final IRNode decl = formal.getDeclaration();
    final String name = TypeFormal.getId(decl);
    final IRNode typeDecl = JJNode.tree.getParent(JJNode.tree.getParent(decl));
    
    /* Favor explicit annotation bounds over those implied by 
     * @Containable
     */
    PromiseDrop<?> result = null;
    final AnnotationBoundsPromiseDrop abDrop = LockRules.getAnnotationBounds(typeDecl);
    if (abDrop != null) {
      result = testFormalAgainstAnnotationBounds(testFor, abDrop.getAAST(), name, exclusively) ? abDrop : null;
    }
    
    if (result == null) {
      final ContainablePromiseDrop cDrop = LockRules.getContainableImplementation(typeDecl);
      if (cDrop != null) {
        result = testFormalAgainstContainable(testFor, cDrop, exclusively) ? cDrop : null;
      }
    }
    
    return result;
  }

  /**
   * See if {@code formalName} has the given type annotation from the 
   * AnnotationBounds annotation.  If {@code exclusively} is {@value true}
   * then {@code formalName} may not have any other type annotations 
   * according to the AnnotationBounds annotation.
   */
  private static boolean testFormalAgainstAnnotationBounds(
      final TypeAnnoImpl testFor, final AnnotationBoundsNode abNode,
      final String formalName, final boolean exclusively) {
    boolean result = testFormalAgainstNamedTypes(formalName, testFor.getAnnotatedFormals(abNode));
    if (result && exclusively) {
      // test that formalName is not also annotated with something else
      for (final TypeAnnoImpl other : TypeAnnoImpl.values()) {
        if (other != testFor) {
          if (testFormalAgainstNamedTypes(formalName, other.getAnnotatedFormals(abNode))) {
            result = false;
            break;
          }
        }
      }
    }
    return result;
  }

  private static boolean testFormalAgainstContainable(
      final TypeAnnoImpl testFor, final ContainablePromiseDrop cDrop,
      final boolean exclusively) {
    boolean result = testFor.testAgainstImpliedContainableBounds(cDrop);
    if (result && exclusively) {
      // test that the formal isn't also annotated with something else
      for (final TypeAnnoImpl other : TypeAnnoImpl.values()) {
        if (other != testFor) {
          if (other.testAgainstImpliedContainableBounds(cDrop)) {
            result = false;
            break;
          }
        }
      }
    }
    return result;
  }

  private static boolean testFormalAgainstNamedTypes(
      final String formalName, final NamedTypeNode[] annotationBounds) {
    for (final NamedTypeNode namedType : annotationBounds) {
      if (namedType.getType().equals(formalName)) {
        return true;
      }
    }
    return false;
  }

  
  
  // ----------------------------------------------------------------------
  // Testers for containable
  // ----------------------------------------------------------------------
  
  private enum ContainableTesters implements TypeTester {
    FOR_FIELD_DECLARATIONS {
      @Override
      public boolean testArrayType(final IJavaArrayType type) {
        return testArrayTypeImpl(type);
      }
      
      @Override
      public ProofDrop testTypeDeclaration(final IRNode type) {
        return LockRules.getContainableType(type);
      }
      
      @Override
      public PromiseDrop<?> testFormalAgainstAnnotationBounds(final IJavaTypeFormal formal) {
        return testTypeFormalForX(TypeAnnoImpl.CONTAINABLE, formal, true);
      }
    },
    
    FOR_FINAL_OBJECT {
      @Override
      public boolean testArrayType(final IJavaArrayType type) {
        return testArrayTypeImpl(type);
      }
      
      @Override
      public ProofDrop testTypeDeclaration(final IRNode type) {
        return LockRules.getContainableImplementation(type);
      }
      
      @Override
      public PromiseDrop<?> testFormalAgainstAnnotationBounds(final IJavaTypeFormal formal) {
        return testTypeFormalForX(TypeAnnoImpl.CONTAINABLE, formal, true);
      }
    },
    
    FOR_PARAMETERIZED_TYPE_ACTUAL {
      @Override
      public boolean testArrayType(final IJavaArrayType type) {
        return testArrayTypeImpl(type);
      }
      
      @Override
      public ProofDrop testTypeDeclaration(final IRNode type) {
        return LockRules.getContainableType(type);
      }
      
      @Override
      public PromiseDrop<?> testFormalAgainstAnnotationBounds(final IJavaTypeFormal formal) {
        return testTypeFormalForX(TypeAnnoImpl.CONTAINABLE, formal, false);
      }
    };

    private static boolean testArrayTypeImpl(final IJavaArrayType type) {
      if (type.getDimensions() == 1) {
        final IJavaType baseType = type.getBaseType();
        return baseType instanceof IJavaPrimitiveType;
      } else {
        return false;
      }
    }
  } 

  
  
  // ----------------------------------------------------------------------
  // Testers for immutable
  // ----------------------------------------------------------------------

  private enum ImmutableTesters implements TypeTester {
    FOR_FIELD_DECLARATIONS {
      @Override
      public boolean testArrayType(final IJavaArrayType type) {
        return false;
      }
      
      @Override
      public ProofDrop testTypeDeclaration(final IRNode type) {
        return LockRules.getImmutableType(type);
      }
      
      @Override
      public PromiseDrop<?> testFormalAgainstAnnotationBounds(final IJavaTypeFormal formal) {
        return testTypeFormalForX(TypeAnnoImpl.IMMUTABLE, formal, true);
      }
    },
    
    FOR_FINAL_OBJECT {
      @Override
      public boolean testArrayType(final IJavaArrayType type) {
        return false;
      }
      
      @Override
      public ProofDrop testTypeDeclaration(final IRNode type) {
        return LockRules.getImmutableImplementation(type);
      }
      
      @Override
      public PromiseDrop<?> testFormalAgainstAnnotationBounds(final IJavaTypeFormal formal) {
        return testTypeFormalForX(TypeAnnoImpl.IMMUTABLE, formal, true);
      }
    },
    
    FOR_PARAMETERIZED_TYPE_ACTUAL {
      @Override
      public boolean testArrayType(final IJavaArrayType type) {
        return false;
      }
      
      @Override
      public ProofDrop testTypeDeclaration(final IRNode type) {
        return LockRules.getImmutableType(type);
      }
      
      @Override
      public PromiseDrop<?> testFormalAgainstAnnotationBounds(final IJavaTypeFormal formal) {
        return testTypeFormalForX(TypeAnnoImpl.IMMUTABLE, formal, false);
      }
    };
  } 

  
  
  // ----------------------------------------------------------------------
  // Testers for reference object
  // ----------------------------------------------------------------------

  private enum ReferenceObjectTesters implements TypeTester {
    FOR_FIELD_DECLARATIONS {
      @Override
      public boolean testArrayType(final IJavaArrayType type) {
        return false;
      }
      
      @Override
      public ProofDrop testTypeDeclaration(final IRNode type) {
        return testTypeDeclarationImpl(type);
      }
      
      @Override
      public PromiseDrop<?> testFormalAgainstAnnotationBounds(final IJavaTypeFormal formal) {
        return testTypeFormalForX(TypeAnnoImpl.REFERENCE, formal, true);
      }
    },
    
    FOR_FINAL_OBJECT {
      @Override
      public boolean testArrayType(final IJavaArrayType type) {
        return false;
      }
      
      @Override
      public ProofDrop testTypeDeclaration(final IRNode type) {
        // XXX: wrong, but not currently used
        return testTypeDeclarationImpl(type);
      }
      
      @Override
      public PromiseDrop<?> testFormalAgainstAnnotationBounds(final IJavaTypeFormal formal) {
        return testTypeFormalForX(TypeAnnoImpl.REFERENCE, formal, true);
      }
    },
    
    FOR_PARAMETERIZED_TYPE_ACTUAL {
      @Override
      public boolean testArrayType(final IJavaArrayType type) {
        return false;
      }
      
      @Override
      public ProofDrop testTypeDeclaration(final IRNode type) {
        return testTypeDeclarationImpl(type);
      }
      
      @Override
      public PromiseDrop<?> testFormalAgainstAnnotationBounds(final IJavaTypeFormal formal) {
        return testTypeFormalForX(TypeAnnoImpl.REFERENCE, formal, false);
      }
    };
    
    private static final int ENUM_IMPLICITLY_REF_OBJECT = 764;
    private static final String JAVA_LANG_ENUM = "java.lang.Enum";

    private static ProofDrop testTypeDeclarationImpl(final IRNode type) {
      if (EnumDeclaration.prototype.includes(type) || JavaNames.getFullTypeName(type).equals(JAVA_LANG_ENUM)) {
        final ResultDrop result = new ResultDrop(type);
        result.setConsistent();
        result.setMessage(ENUM_IMPLICITLY_REF_OBJECT, JavaNames.getRelativeTypeNameDotSep(type));
        return result;
      } else {
        return EqualityRules.getRefObjectDrop(type);
      }
    }
  } 
  
  

  // ----------------------------------------------------------------------
  // Testers for thread safe
  // ----------------------------------------------------------------------

  private enum ThreadSafeTesters implements TypeTester {
    FOR_FIELD_DECLARATIONS {
      @Override
      public boolean testArrayType(final IJavaArrayType type) {
        return false;
      }
      
      @Override
      public ProofDrop testTypeDeclaration(final IRNode type) {
        return LockRules.getThreadSafeTypePromise(type);
      }
      
      @Override
      public PromiseDrop<?> testFormalAgainstAnnotationBounds(final IJavaTypeFormal formal) {
        return testForThreadSafeOrImmutable(formal);
      }
    },
    
    FOR_FINAL_OBJECT {
      @Override
      public boolean testArrayType(final IJavaArrayType type) {
        return false;
      }
      
      @Override
      public ProofDrop testTypeDeclaration(final IRNode type) {
        return LockRules.getThreadSafeImplPromise(type);
      }
      
      @Override
      public PromiseDrop<?> testFormalAgainstAnnotationBounds(final IJavaTypeFormal formal) {
        return testForThreadSafeOrImmutable(formal);
      }
    },
    
    FOR_PARAMETERIZED_TYPE_ACTUAL {
      @Override
      public boolean testArrayType(final IJavaArrayType type) {
        return false;
      }
      
      @Override
      public ProofDrop testTypeDeclaration(final IRNode type) {
        return LockRules.getThreadSafeType(type);
      }
      
      @Override
      public PromiseDrop<?> testFormalAgainstAnnotationBounds(final IJavaTypeFormal formal) {
        return testTypeFormalForX(TypeAnnoImpl.THREADSAFE, formal, false);
      }
    };

    private static PromiseDrop<?> testForThreadSafeOrImmutable(
        final IJavaTypeFormal formal) {
      final PromiseDrop<?> result = testTypeFormalForX(TypeAnnoImpl.THREADSAFE, formal, true);
      if (result == null) {
        return testTypeFormalForX(TypeAnnoImpl.IMMUTABLE, formal, true);
      }
      return result;
    }
  } 

  
  
  // ----------------------------------------------------------------------
  // Testers for value object
  // ----------------------------------------------------------------------

  private enum ValueObjectTesters implements TypeTester {
    FOR_FIELD_DECLARATIONS {
      @Override
      public boolean testArrayType(final IJavaArrayType type) {
        return false;
      }
      
      @Override
      public ProofDrop testTypeDeclaration(final IRNode type) {
        return EqualityRules.getValueObjectDrop(type);
      }
      
      @Override
      public PromiseDrop<?> testFormalAgainstAnnotationBounds(final IJavaTypeFormal formal) {
        return testTypeFormalForX(TypeAnnoImpl.VALUE, formal, true);
      }
    },
    
    FOR_FINAL_OBJECT {
      @Override
      public boolean testArrayType(final IJavaArrayType type) {
        return false;
      }
      
      @Override
      public ProofDrop testTypeDeclaration(final IRNode type) {
        // XXX: wrong, but not currently used
        return EqualityRules.getValueObjectDrop(type);
      }
      
      @Override
      public PromiseDrop<?> testFormalAgainstAnnotationBounds(final IJavaTypeFormal formal) {
        return testTypeFormalForX(TypeAnnoImpl.VALUE, formal, true);
      }
    },
    
    FOR_PARAMETERIZED_TYPE_ACTUAL {
      @Override
      public boolean testArrayType(final IJavaArrayType type) {
        return false;
      }
      
      @Override
      public ProofDrop testTypeDeclaration(final IRNode type) {
        return EqualityRules.getValueObjectDrop(type);
      }
      
      @Override
      public PromiseDrop<?> testFormalAgainstAnnotationBounds(final IJavaTypeFormal formal) {
        return testTypeFormalForX(TypeAnnoImpl.VALUE, formal, false);
      }
    };
  } 
}
