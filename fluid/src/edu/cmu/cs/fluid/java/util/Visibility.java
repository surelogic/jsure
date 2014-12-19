/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.java.util;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public enum Visibility {
  PRIVATE {
    @Override
    public String getSourceText() {
      return "private";
    }
    
    @Override
    public int getModifier() {
      return JavaNode.PRIVATE;
    }
  },
  
  DEFAULT {
    @Override
    public String getSourceText() {
      return " ";  // should be "", but use space to prevent disruption of oracles
    }
    
    @Override
    public int getModifier() {
      return 0;
    }
  },
  
  PROTECTED {
    @Override
    public String getSourceText() {
      return "protected";
    }
    
    @Override
    public int getModifier() {
      return JavaNode.PROTECTED;
    }
  },
  
  PUBLIC {
    @Override
    public String getSourceText() {
      return "public";
    }
    
    @Override
    public int getModifier() {
      return JavaNode.PUBLIC;
    }
  };

  /**
   * Get the modifier string associated with the visibility.
   */
  public abstract String getSourceText();
  
  /**
   * Get the modifier bit.
   */
  public abstract int getModifier();
  
  /**
   * Get the name of the element, but all lower case.
   */
  public final String nameLowerCase() {
    return name().toLowerCase();
  }
  
  /**
   * Are we as visible or more so than the given visibility?
   */
  public final boolean atLeastAsVisibleAs(final Visibility other) {
    return this.compareTo(other) >= 0;
  }

  
  
  /**
   * Get the visibility of a NewRegionDeclaration, FieldDeclaration,
   * MethodDeclaration, ConstructorDeclaration.
   */
  public static Visibility getVisibilityOf(final IRNode decl) {
    final Operator op = JJNode.tree.getOperator(decl);
    final int mods;
    if (EnumConstantDeclaration.prototype.includes(op)) {
      return PUBLIC;
    } else if (VariableDeclarator.prototype.includes(op)) {
      mods = VariableDeclarator.getMods(decl);
    } else {
      mods = JavaNode.getModifiers(decl);
    }
    return getVisibility(mods);
  }

  public static Visibility getVisibility(final int mods) {
    if ((mods & JavaNode.PRIVATE) != 0) {
      return PRIVATE;
    } else if ((mods & JavaNode.PROTECTED) != 0) {
      return PROTECTED;
    } else if ((mods & JavaNode.PUBLIC) != 0) {
      return PUBLIC;
    } else {
      return DEFAULT;
    }
  }
    
  /**
   * Is the first declaration at least as visible as the second?
   */
  public static boolean atLeastAsVisibleAs(final IRNode d1, final IRNode d2) {
    return getVisibilityOf(d1).atLeastAsVisibleAs(getVisibilityOf(d2));
  }
}
