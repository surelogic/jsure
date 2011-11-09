package com.surelogic.opgen.antTasks;

import java.util.*;


import org.apache.tools.ant.*;
import org.apache.tools.ant.types.EnumeratedAttribute;

import com.surelogic.opgen.*;
import com.surelogic.opgen.generator.*;

// 1.  Create a Java class that extends org.apache.tools.ant.Task, or another class that was desgined to be extended.
public class GenerateTask extends Task {  
  private static final String INTERFACE = "interface";
  private static final String CRYSTAL = "crystal";
  private static final String OPERATOR = "operator";
  
  static class Kinds extends EnumeratedAttribute {
    private String[] values = {
        INTERFACE, CRYSTAL, OPERATOR  
    };
    
    @Override
    public String[] getValues() {
      return values;
    }
    
  }
  // 2. For each attribute, write a setter method. 
  //    The setter method must be a public void method that takes a single argument. 
  //    The name of the method must begin with set, followed by the attribute name, 
  //    with the first character of the name in uppercase, and the rest in lowercase*. 
  //    That is, to support an attribute named file you create a method setFile. 
  //    Depending on the type of the argument, Ant will perform some conversions for you, see below.
  
  private Kinds kind;
  private String dest;
  private List<String> srcs;
  
  public void setDest(String d) {
    dest = d;
  }

  public void setKind(Kinds k) {    
    kind = k;
  }
  
  public void setSrcs(String a) {
    StringTokenizer st = new StringTokenizer(a, ",");
    srcs = new ArrayList<String>();
    while (st.hasMoreTokens()) {
      srcs.add(st.nextToken());
    }
  }
  
  // 3. If your task shall contain other tasks as nested elements (like parallel), 
  //    your class must implement the interface org.apache.tools.ant.TaskContainer. 
  //    If you do so, your task can not support any other nested elements. See below.
  
  // 4. If the task should support character data (text nested between the start end end tags), 
  //    write a public void addText(String) method. 
  //    Note that Ant does not expand properties on the text it passes to the task.
  
  // 5. For each nested element, write a create, add or addConfigured method. 
  //    A create method must be a public method that takes no arguments and returns an Object type. 
  //    The name of the create method must begin with create, followed by the element name. 
  //    An add (or addConfigured) method must be a public void method that takes a single argument
  //    of an Object type with a no-argument constructor. 
  //    The name of the add (addConfigured) method must begin with add (addConfigured), followed by the element name. 
  //    For a more complete discussion see below.
  
  //6. Write a public void execute method, with no arguments, that throws a BuildException. This method implements the task itself.
  @Override
  public void execute() throws BuildException {    
    AbstractGenerator g;
    
    if (kind == null) {
      throw new BuildException("kind was not specified");
    }
    final String k = kind.getValue();
    if (k.equals(INTERFACE)) {
      g = new InterfaceGen();
    }
    else if (k.equals(CRYSTAL)) {
      g = new CrystalGen();
    }
    else if (k.equals(OPERATOR)) {
      g = new OperatorGen();
    }
    else {
      throw new BuildException("Unknown kind was specified: "+kind);
    }
    if (dest != null) {
      srcs.add(0, "-out");
      srcs.add(1, dest);
    }
    g.generate(srcs.toArray(new String[srcs.size()]));
  }
}
