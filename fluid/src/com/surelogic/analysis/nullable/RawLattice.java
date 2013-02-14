package com.surelogic.analysis.nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.dropsea.ir.drops.nullable.RawPromiseDrop;

import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.uwm.cs.fluid.util.AbstractLattice;

/**
 * Lattice for computing the raw type of a local variable.  Values from this
 * lattice are interpreted with respect to the declared type <i>T</i> of the
 * variable.  Normal values from this lattice are class declarations.  So a 
 * value of class <i>C</i> means the type of the variable is
 * <i>T</i><sup>raw(<i>C</i>)-</sup>.  The structure of this lattice is thus
 * derived from the class hierarchy of the program under analysis.  There are
 * three special values:
 * 
 * <dl>
 *   <dt>RAW</dt>
 *   <dd>This is the <b>TOP</b> value of the lattice, and is ordered directly
 *   above <code>java.lang.Object</code>.  This value means that the type
 *   of the local variable is <i>T</i><sup>raw-</sup>.
 *   
 *   <dt>IMPOSSIBLE<dt>
 *   <dd>This value exists only because of the {@link #meet} operation, so that
 *   we can give a value for the meet of two class where one is not an ancestor
 *   of the other.  This value is directly below each leaf class in the class
 *   hierarchy.</dd>
 *   
 *   <dt>NOT_RAW</dt>
 *   <dd>This is the <b>BOTTOM</b> value of the lattice, and is directly below
 *   <b>IMPOSSIBLE</b>.  It means that the variable does not have a raw type at
 *   all; the type of the variable is <i>T</i>.  The nullity of the type is
 *   obtained from external information.</dd>
 * </dl>
 * 
 * <p>The position of <b>IMPOSSIBLE</b> in the lattice is unfortunate, but
 * shouldn't cause any problems in practice because the value should never arise
 * during control flow analysis.  This is because the meet operation is never 
 * invoked. 
 */
public final class RawLattice
extends AbstractLattice<RawLattice.Element> {
  public static final Element[] ARRAY_PROTOTYPE = new Element[0];
  public static final Element RAW = Specials.RAW;
  public static final Element IMPOSSIBLE = Specials.IMPOSSIBLE;
  public static final Element NOT_RAW = Specials.NOT_RAW;
  
  
  
  public static interface Element {
    public boolean lessEq(Element other);
    public Element join(Element other);
    public Element meet(Element other);
  }
  
  private static enum Specials implements Element {
    RAW {
      @Override
      public boolean lessEq(final Element other) {
        return other == RAW;
      }

      @Override
      public Element join(final Element other) {
        return this;
      }

      @Override
      public Element meet(final Element other) {
        return other;
      }
    },
    
    IMPOSSIBLE {
      @Override
      public boolean lessEq(final Element other) {
        // less than or equal to everything but NOT_RAW
        return other != NOT_RAW;
      }

      @Override
      public Element join(final Element other) {
        return other == NOT_RAW ? this : other;
      }

      @Override
      public Element meet(final Element other) {
        return other == NOT_RAW ? other : this;
      }
    },
    
    NOT_RAW  {
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
    };
  }
  
  private final class ClassElement implements Element {
    private final IJavaDeclaredType type;
    
    public ClassElement(final IJavaDeclaredType t) {
      type = t;
    }
    
    @Override
    public boolean lessEq(final Element other) {
      if (other == Specials.RAW) {
        return true;
      } else if (other == Specials.IMPOSSIBLE || other == Specials.NOT_RAW) {
        return false;
      } else {
        final ClassElement ce = (ClassElement) other;
        return typeEnv.isSubType(type, ce.type);
      }
    }

    @Override
    public Element join(final Element other) {
      // Short circuit joins with ourselves!
      if (this == other) {
        return this;
      }
      
      if (other == Specials.RAW) {
        return other;
      } else if (other == Specials.IMPOSSIBLE || other == Specials.NOT_RAW) {
        return this;
      } else {
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
      if (other == Specials.RAW) {
        return this;
      } else if (other == Specials.IMPOSSIBLE || other == Specials.NOT_RAW) {
        return other;
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
  
  
  
  public RawLattice(final ITypeEnvironment te) {
    typeEnv = te;
    classElements = new HashMap<IJavaDeclaredType, RawLattice.ClassElement>();
  }
  
  

  @Override
  public boolean lessEq(final Element v1, final Element v2) {
    return v1.lessEq(v2);
  }

  @Override
  public Element top() {
    return Specials.RAW;
  }

  @Override
  public Element bottom() {
    return Specials.NOT_RAW;
  }
  
  public Element injectClass(final IJavaDeclaredType t) {
    ClassElement e = classElements.get(t);
    if (e == null) {
      e = new ClassElement(t);
      classElements.put(t, e);
    }
    return e;
  }

  public Element injectPromiseDrop(final RawPromiseDrop pd) {
    final NamedTypeNode typeName = pd.getAAST().getUpToType();
    if (typeName.getType().equals("*")) {
      return RAW;
    } else {
      return injectClass(
          (IJavaDeclaredType) typeName.resolveType().getJavaType());
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
