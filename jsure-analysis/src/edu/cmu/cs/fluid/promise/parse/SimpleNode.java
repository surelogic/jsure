// $Header$
package edu.cmu.cs.fluid.promise.parse;

import java.io.PrintStream;

import edu.cmu.cs.fluid.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.operator.LoadOperator;
import edu.cmu.cs.fluid.java.promise.*;


/** Class with specific methods needed for parsing Java. */
public class SimpleNode extends JavaNode implements PromiseParserTreeConstants {
  static JavaOperator[] operatorArray = new JavaOperator[jjtNodeName.length];
  static boolean[] operatorUsed = new boolean[jjtNodeName.length];
  
  private static boolean loaded = false;
  static void loadPromises() {
    if (!loaded) {
      // System.out.println("Loading...");
      loaded = true; // prevent infinite recursion
      /* An artificial field added to ensure operators are all loaded. */
      @SuppressWarnings("unused") LoadOperator x = new LoadOperator();
      @SuppressWarnings("unused") LoadPromise y = new LoadPromise();
      int n = jjtNodeName.length;

      for (int i = 1; i < n; ++i) { // skip "void"
        try {
          JavaOperator op =  JavaOperator.findOperator(jjtNodeName[i]);
          // System.out.println("Loaded " + jjtNodeName[i]);
          operatorArray[i] = op;
        }
        catch (FluidRuntimeException e) {
          System.out.println("Could not load in JavaPromiseOperator:" + jjtNodeName[i]);
        }
      }
    }
  }

  // Do not do it here, the array is wrong size at beginning
  // static { loadOperators(); }

  @SuppressWarnings("unused")
  private SimpleNode(JavaOperator op) {
    super(tree, op);
  }

  /** Create a node with the operator that has the given name.
   * This is the entry for the parser.
   */
  public static JavaNode jjtCreate(int index) {
    JavaOperator op = operatorArray[index];
    if (op == null) {
      if (!loaded) {
        loadPromises();
        return jjtCreate(index);
      } else {
	      throw new FluidRuntimeException("Operator " + jjtNodeName[index] +
					" not found");
      }
    }
    operatorUsed[index] = true;
    return JavaNode.makeJavaNode(op);
    //return new SimpleNode(op);
  }

  public static void printUnusedOperators(PrintStream p) {
    boolean allUsed = true;
    for(int i=1; i<operatorUsed.length; i++) {
      if (!operatorUsed[i]) {
        p.print("Unused:\t");
        p.println(operatorArray[i].name());
        allUsed = false;
      }      
    }
    if (allUsed) {
      p.println("No unused promise operators");
    }
  }
  
  /** Parse in a .java file and immediately write the tree as an IR chunk. */
  /*
  public static void main(String[] args) {
    JavaNode n = JavaParser.parse(args);
    System.out.print("\nWriting file tmp.data ... ");
    IRStorage st = JavaNode.save(n);
    try {
      OutputStream out = new BufferedOutputStream(new FileOutputStream("tmp.data"));
      st.write(out);
      out.close();
    } catch (IOException e) {
      System.out.println("\n  Something went wrong ");
      System.out.println(e.toString());
    }
    System.out.println("Done.");
   }
  */
}
