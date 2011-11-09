package com.surelogic.opgen.syntax;

import java.util.*;
import java.util.regex.Matcher;

/**
 * Represents $foo:FooType(arg1,arg2)
 * 
 * @author chance
 */
public class Attribute extends SyntaxElement {
  public final String name;
  public final String type;
  public final List<String> args;
  
  Attribute(int i, String t, Matcher m) {
    super(i, t);
    String name = m.group(1);
    if (name == null) {
      // Default to $slot1, $slot2, etc.
      name = "slot"+i;
    } else {
      name = name.substring(0,name.length()-1);
    }      
    this.name = name;
    this.type = m.group(2);
    
    String args = m.group(3);
    if (args != null) {      
      List<String> l = new ArrayList<String>();
      StringTokenizer st = new StringTokenizer(args, "(,)");
      while (st.hasMoreTokens()) {
        l.add(st.nextToken());
      }
      this.args = Collections.unmodifiableList(l);
    } else {
      this.args = Collections.emptyList(); 
    }
  }
  
  public boolean isAbstract() { 
    return false;
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Attribute ");
    sb.append(index);
    sb.append(": ");
    sb.append(name);
    sb.append(": ");
    sb.append(type);
    if (args.size() > 0) {
      boolean first = true;
      sb.append(" ( ");
      for (String arg : args) {
        if (!first) {
          sb.append(", ");
        }
        first = false;
        sb.append(arg);
      }
      sb.append(" ) ");
    }
    return sb.toString();
  }
  
  @Override
  public boolean equals(Object o) {
    if (o instanceof Attribute) {
      Attribute a2 = (Attribute) o;      
      if (!name.equals(a2.name) || !type.equals(a2.type)) {
        return false;
      }
      return (args.equals(a2.args));
    }
    return false;
  }
  @Override
  public int hashCode() {
    return name.hashCode() + type.hashCode();
  }

  static Attribute create(int i, String word, Matcher m) {
    if (word.startsWith("$@")) {
      return new AbstractAttribute(i, word, m);
    } else {
      return new Attribute(i, word, m);
    }
  }
}
