package com.surelogic.opgen.syntax;

import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;

public class OpSyntax {
  public static final OpSyntax invalid = new OpSyntax();
  public static final String OPT_CHILD_PREFIX = "Opt";
  
  public final String packageName;
  public final String sourceFilename;
  public final long lastModified;
  /**
   * The text that appears before and after the syntax block
   */
  public final String beforeText, afterOp, afterText;
  public final String modifiers;
  public final String name;
  public final String parentOperator;
  public final boolean isRoot;
  public final boolean isConcrete;
  
  public final List<String> superifaces;
  public final List<SyntaxElement> syntax;
  public final List<Attribute> attributes;
  public final List<Child> children;
  public final Modifier variability;
  public final int variableChildIndex;
  public final Child variableChild;
  public final boolean usesPrecedence;
  public final int numChildren;
  
  public final Map<Property,String> props;
  
  OpSyntax(String pkgName, String sourceFilename, long lastModified,
           String beforeText, String afterOp, String afterText,
           String modifiers, String opname, String superop,
           boolean isroot, boolean isConcrete,
           List<String> superifaces,
           List<SyntaxElement> syntax,
           List<Attribute> attributes,
           List<Child> childs,
           Modifier variability,
           boolean precedence,
           int variablechild,
           Child variableChild,
           Map<Property,String> props)
  {
    this.packageName = pkgName;
    this.sourceFilename = sourceFilename;
    this.lastModified = lastModified;
    this.beforeText = beforeText;
    this.afterOp = afterOp;
    this.afterText = afterText;
    this.modifiers = modifiers;
    this.name = opname;
    this.parentOperator = superop;
    this.isRoot = isroot;
    this.isConcrete = isConcrete;
    
    this.superifaces = Collections.unmodifiableList(new ArrayList<String>(superifaces));
    this.syntax = Collections.unmodifiableList(new ArrayList<SyntaxElement>(syntax));
    this.attributes = Collections.unmodifiableList(new ArrayList<Attribute>(attributes));
    this.children = Collections.unmodifiableList(new ArrayList<Child>(childs));
    this.variability = variability;
    this.usesPrecedence = precedence;
    this.variableChildIndex = variablechild;
    this.variableChild = variableChild;
    numChildren = childs.size() - (variability==null? 0:1);
    
    this.props = Collections.unmodifiableMap(new HashMap<Property,String>(props));
  }
  
  OpSyntax() {
    this.packageName = "";
    this.sourceFilename = "";
    this.lastModified = -1;
    this.beforeText = "";
    this.afterOp = "";
    this.afterText = "";
    this.modifiers = "";
    this.name = "invalid operator";
    this.parentOperator = null;
    this.isRoot = true;
    this.isConcrete = false;
    
    this.superifaces = Collections.emptyList();
    this.syntax = Collections.emptyList();
    this.attributes = Collections.emptyList();
    this.children = Collections.emptyList();
    this.variability = null;
    this.usesPrecedence = false;
    this.variableChildIndex = -1;
    this.variableChild = null;
    numChildren = 0;
    
    this.props = Collections.emptyMap();
  }

  public boolean isVariable() {
    return variability != null;
  }
  
  public boolean hasBinding() {
    return afterOp.contains("IHasBinding");
  }
  
  public boolean hasMatching(Attribute a1) {
    for (Attribute a2 : attributes) {
      if (a1.equals(a2)) {
        return true;
      }
    }
    return false;
  }
  
  public Child findMatching(Child c1) {
    for (Child c2 : children) {
      if (c1.equals(c2)) {
        return c2;
      }
    }
    return null;
  }
  
  public Child findMatching(String name) {
    for (Child c : children) {
      if (c.name.equals(name)) {
        return c;
      }
    }
    return null;
  }
  
  public boolean hasMatching(Child c1) {
    return findMatching(c1) != null;
  }
  
  public void generateFromSyntax(Map<String,String> typeTable, SyntaxStrategy p) {
    final int total = this.syntax.size();
    p.init();

    for(int j=0; j<total; j++) {
      SyntaxElement syntax = this.syntax.get(j);       

      if (syntax instanceof Child) {
        p.doForChild(this, j, (Child) syntax, j == variableChildIndex);
      }
      else if (syntax instanceof Attribute) {
        Attribute a = (Attribute) syntax;
        String type = typeTable.get(a.type);
        if (type == null) {
          type = "Object";
        }
        p.doForInfo(this, j, (Attribute) syntax, type);
      }
      else if (syntax instanceof Token) {
        p.doForToken((Token) syntax);
      }
    }
  }
  
  public void printState(PrintStream out) {
    out.print(beforeText);
    out.print(modifiers);
    out.print("operator "+name);
    if (!isRoot) {
      out.print(" extends "+parentOperator);
      for (String superiface : superifaces) {
        out.print(", "+superiface);
      }
    } else {
      boolean first = true;
      for (String superiface : superifaces) {
        if (first) {
          first = false;
          out.print(" extends ");
        } else {
          out.print(", ");
        }
        out.print(superiface);
      }
    }
    out.println();
    out.println("\nisroot = "+isRoot);
    out.print(afterOp);
    
    out.println("\ntotal = "+syntax.size());
    out.println("children = "+numChildren);
    out.println("slots = "+attributes.size()); 
    int i = 0;
    for(SyntaxElement s : syntax) {
      out.println("Syntax["+i+"] = "+s);
      i++;
    }
    if (variability != null) {
      out.println("Variability = "+variability);
      out.println("Variable child index = "+variableChildIndex);
      out.println("Variable child = "+variableChild);
    }
    out.println();
    printProperties(out);
    out.println();
    out.print(afterText);
  }

  public final void printProperties(PrintStream out) {
    if (!props.isEmpty()) {
      out.println("Properties:");
      for (Entry<Property,String> e : props.entrySet()) {
        out.println("\t"+e.getKey().getName()+" = "+e.getValue());
      }
    }
  } 
  
  @Override
  public String toString() {
    return "OpSyntax '"+name+"'";
  }
}
