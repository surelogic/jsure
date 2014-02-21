package com.surelogic.analysis.nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.NonNullPromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.NullablePromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.RawPromiseDrop;

import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.uwm.cs.fluid.util.AbstractLattice;

/**
 * Lattice for computing the null/raw status of a variable.  Values from this
 * lattice are interpreted with respect to the declared type <i>T</i> of the
 * variable.  The structure of this lattice is derived from the type rules:
 * 
 * <ul>
 *   <li>If <i>S</i> &lt;: <i>R</i> then <i>T</i><sup>raw(<i>S</i>)-</sup> &lt;: <i>T</i><sup>raw(<i>R</i>)-</sup>
 *   <li><i>T</i><sup>raw(<i>R</i>)-</sup> &lt;: <i>T</i><sup>raw-</sup>
 *   <li>For any <i>R</i>, <i>T</i><sup>-</sup> &lt;: <i>T</i><sup>raw(<i>R</i>)-</sup>
 * </ul>
 * 
 * <p>The structure of the lattice is thus partially derived from the class
 * hierarchy of the program under analysis.  There are five special values:
 *
 * <dl>
 * <dt>MAYBE_NULL</dt>
 * <dd>This is the <em>TOP</em> value of the lattice and corresponds to the 
 * type <i>T</i><sup>+</sup>.  The value of the reference may be <code>null</code>.
 * 
 * <dt>NULL</dt>
 * <dd>This doesn't correspond to any type, but does indicate that the value
 * of the reference <em>must be null</em>.  This value is directly directly
 * below MAYBE_NULL, and is a sibling to RAW.
 * 
 * <dt>RAW</dt>
 * <dd>Directly below MAYBE_NULL, sibling to NULL, and directly above <code>java.lang.Object</code>.
 * This value means that the type of the local variable is
 * <i>T</i><sup>raw-</sup>.  The value of the reference cannot be <code>null</code>,
 * but the referenced object might not be initialized at all.
 * 
 * <dt>NOT_NULL</dt>
 * <dd>Directly below all the leaf classes in the class hierarchy, this value
 * corresponds to the type <i>T</i><sup>-</sup>.  The value of the reference
 * cannot be <code>null</code>, and the referenced object is fully initialized.
 * 
 * <dt>IMPOSSIBLE</dt>
 * <dd>The <em>BOTTOM</em> value. This value is directly below NULL and NOT_NULL.
 * </dl>
 * 
 * Otherwise, the lattice value represents a class <i>C</i>, and is interpreted
 * to mean that the type of the variable is 
 * <i>T</i><sup>raw(<i>C</i>)-</sup>.  The referenced value cannot be <code>null</code> 
 * and the referenced object is assumed to be initialized through class <i>C</i> only.
 * That is, any field declared in a class that is a subclass of <i>C</i> might
 * have the <code>null</code> value regardless of the null type qualifiers on
 * the field declaration. 
 */
public final class NonNullRawLattice
extends AbstractLattice<NonNullRawLattice.Element> {
//  public static final Element[] ARRAY_PROTOTYPE = new Element[0];
  public static final Element MAYBE_NULL = Specials.MAYBE_NULL;
  public static final Element NULL = Specials.NULL;
  public static final Element RAW = Specials.RAW;
  public static final Element NOT_NULL = Specials.NOT_NULL;
  public static final Element IMPOSSIBLE = Specials.IMPOSSIBLE;
  
  
  
  public static interface Element {
    public boolean lessEq(Element other);
    public Element join(Element other);
    public Element meet(Element other);
    public String getAnnotation();
    public boolean isAssignableFrom(ITypeEnvironment tEnv, Element other);
  }
  
  private static enum Specials implements Element {
    MAYBE_NULL {
      @Override
      public boolean lessEq(final Element other) {
        return other == MAYBE_NULL;
      }

      @Override
      public Element join(final Element other) {
        return MAYBE_NULL;
      }

      @Override
      public Element meet(final Element other) {
        return other;
      }
      
      @Override
      public String getAnnotation() {
        return "@Nullable";
      }
      
      @Override
      public boolean isAssignableFrom(
          final ITypeEnvironment tEnv, final Element other) {
        return other == NonNullRawLattice.NOT_NULL ||
            other == NonNullRawLattice.MAYBE_NULL ||
            other == NonNullRawLattice.NULL;
      }
    },
    
    NULL {
      @Override
      public boolean lessEq(final Element other) {
        return other == MAYBE_NULL || other == NULL;
      }

      @Override
      public Element join(final Element other) {
        if (other == IMPOSSIBLE || other == NULL) return this;
        else return MAYBE_NULL;
      }

      @Override
      public Element meet(final Element other) {
        if (other == MAYBE_NULL || other == NULL) return this;
        else return IMPOSSIBLE;
      }
      
      @Override
      public String getAnnotation() {
        return "null";
      }
      
      @Override
      public boolean isAssignableFrom(
          final ITypeEnvironment tEnv, final Element other) {
        return other == NonNullRawLattice.NULL;
      }
    },
    
    RAW {
      @Override
      public boolean lessEq(final Element other) {
        return other == MAYBE_NULL || other == RAW;
      }

      @Override
      public Element join(final Element other) {
        if (other == MAYBE_NULL || other == NULL) return MAYBE_NULL;
        else return this;
      }

      @Override
      public Element meet(final Element other) {
        if (other == MAYBE_NULL) return this;
        else if (other == NULL) return IMPOSSIBLE;
        else return other;
      }
      
      @Override
      public String getAnnotation() {
        return "@Initialized";
      }
      
      @Override
      public boolean isAssignableFrom(
          final ITypeEnvironment tEnv, final Element other) {
        return other == NonNullRawLattice.NOT_NULL ||
            other == NonNullRawLattice.RAW ||
            other instanceof ClassElement;
      }
    },
    
    NOT_NULL {
      @Override
      public boolean lessEq(final Element other) {
        return other != NULL && other != IMPOSSIBLE;
      }

      @Override
      public Element join(final Element other) {
        if (other == IMPOSSIBLE) return this;
        else if (other == NULL) return MAYBE_NULL;
        else return other;
      }

      @Override
      public Element meet(final Element other) {
        if (other == IMPOSSIBLE || other == NULL) return IMPOSSIBLE;
        else return this;
      }
      
      @Override
      public String getAnnotation() {
        return "@NonNull";
      }
      
      @Override
      public boolean isAssignableFrom(
          final ITypeEnvironment tEnv, final Element other) {
        return other == NonNullRawLattice.NOT_NULL;
      }
    },
    
    IMPOSSIBLE {
      @Override
      public boolean lessEq(final Element other) {
        return true;
      }

      @Override
      public Element join(final Element other) {
        return other;
      }

      @Override
      public Element meet(final Element other) {
        return this;
      }
            
      @Override
      public String getAnnotation() {
        return "impossible";
      }
      
      @Override
      public boolean isAssignableFrom(
          final ITypeEnvironment tEnv, final Element other) {
        return false;
      }
    };
  }
  
  public final class ClassElement implements Element {
    private final IJavaDeclaredType type;
    
    private ClassElement(final IJavaDeclaredType t) {
      type = t;
    }
    
    public IJavaDeclaredType getType() {
      return type;
    }
    
    @Override
    public boolean lessEq(final Element other) {
      if (other == Specials.MAYBE_NULL || other == Specials.RAW) {
        return true;
      } else if (other instanceof ClassElement) {
        final ClassElement ce = (ClassElement) other;
        return typeEnv.isSubType(type, ce.type);
      } else {
        return false; // NULL or NOT_NULL or IMPOSSIBLE
      }
    }

    @Override
    public Element join(final Element other) {
      // Short circuit joins with ourselves!
      if (this == other) {
        return this;
      }
      
      if (other == Specials.NOT_NULL || other == Specials.IMPOSSIBLE) {
        return this;
      } else if (other == Specials.NULL || other == Specials.MAYBE_NULL) {
        return Specials.MAYBE_NULL;
      } else if (other == Specials.RAW) { 
        return Specials.RAW;
      } else {  // Must be a ClassElement
        // Join with a class other than ourself (short-circuited above)
        final Set<IJavaDeclaredType> ancestors = new HashSet<IJavaDeclaredType>();
        IJavaDeclaredType current = type;
        while (current != null) {
          ancestors.add(current);
          current = current.getSuperclass(typeEnv);
        }
        
        final ClassElement ce = (ClassElement) other;
        current = ce.type;
        while (current != null) {
          if (ancestors.contains(current)) return injectClass(current);
          current = current.getSuperclass(typeEnv);
        }
        // shouldn't get here
        throw new RuntimeException(
            "Couldn't find least common ancestor for types " + 
                type + " and " + ce.type);
      }
    }

    @Override
    public Element meet(final Element other) {
      if (other == Specials.MAYBE_NULL || other == Specials.RAW) {
        return this;
      } else if (other == Specials.NOT_NULL || other == Specials.IMPOSSIBLE) {
        return other;
      } else if (other == Specials.NULL) {
        return Specials.IMPOSSIBLE;
      } else {
        final ClassElement ce = (ClassElement) other;
        if (type.isSubtypeOf(ce.type)) return this;
        else if (ce.type.isSubtypeOf(type)) return other;
        else return Specials.IMPOSSIBLE;
      }
    }
    
    @Override
    public boolean equals(final Object other) {
      if (other instanceof ClassElement) {
        final ClassElement ce = (ClassElement) other;
        return this.type.equals(ce.type);
      } else {
        return false;
      }
    }
    
    @Override
    public String getAnnotation() {
      return "@Initialized(through=\"" + type.toSourceText() + "\")";
    }
    
    @Override
    public boolean isAssignableFrom(
        final ITypeEnvironment tEnv, final Element other) {
      if (other instanceof ClassElement) {
        final IJavaType t1 = ((ClassElement) other).getType();
        final IJavaType t2 = this.getType();
        return tEnv.isSubType(t1, t2);
      } else {
        return other == NonNullRawLattice.NOT_NULL;
      }
    }
    
    @Override
    public int hashCode() {
      return type.hashCode();
    }
    
    @Override
    public String toString() {
      return "RAW(" + type.toSourceText() + ")";
    }
  }

  
  
  private final ITypeEnvironment typeEnv;
  private final Map<IJavaDeclaredType, ClassElement> classElements;
  
  
  
  public NonNullRawLattice(final ITypeEnvironment te) {
    typeEnv = te;
    classElements = new HashMap<IJavaDeclaredType, NonNullRawLattice.ClassElement>();
  }
  
  

  @Override
  public boolean lessEq(final Element v1, final Element v2) {
    return v1.lessEq(v2);
  }

  @Override
  public Element top() {
    return Specials.MAYBE_NULL;
  }

  @Override
  public Element bottom() {
    return Specials.IMPOSSIBLE;
  }
  
  public Element injectClass(final IJavaDeclaredType t) {
    ClassElement e = classElements.get(t);
    if (e == null) {
      e = new ClassElement(t);
      classElements.put(t, e);
    }
    return e;
  }

  public Element injectPromiseDrop(final PromiseDrop<?> pd) {
    if (pd instanceof RawPromiseDrop) {
      final NamedTypeNode typeName = ((RawPromiseDrop) pd).getAAST().getUpToType();
      if (typeName.getType().equals("*")) {
        return RAW;
      } else {
        return injectClass(
            (IJavaDeclaredType) typeName.resolveType().getJavaType());
      }
    } else if (pd == null || pd instanceof NullablePromiseDrop) {
      return MAYBE_NULL;
    } else if (pd instanceof NonNullPromiseDrop) {
      return NOT_NULL;
    } else {
      return IMPOSSIBLE;
    }
  }
  
  @Override
  public Element join(final Element v1, final Element v2) {
    return v1.join(v2);
  }

  @Override
  public Element meet(final Element v1, final Element v2) {
    return v1.meet(v2);
  }
}
