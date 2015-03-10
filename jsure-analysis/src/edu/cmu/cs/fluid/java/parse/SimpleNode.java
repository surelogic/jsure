/* $Header: /var/cvs/fluid/code/fluid/java/parse/SimpleNode.java,v 1.3 2002/08/08 20:13:45 chance Exp $ */
package edu.cmu.cs.fluid.java.parse;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaOperator;
import edu.cmu.cs.fluid.java.operator.LoadOperator;
import edu.cmu.cs.fluid.tree.Operator;


/** Class with specific methods needed for parsing Java. */
public class SimpleNode extends JavaNode implements JavaParserTreeConstants {
  static JavaOperator[] operatorArray = new JavaOperator[jjtNodeName.length];

  private static boolean loaded = false;
  static void loadOperators() {
    if (!loaded) {
      // System.out.println("Loading...");
      loaded = true; // prevent infinite recursion
      /* An artificial field added to ensure operators are all loaded. */
      @SuppressWarnings("unused")
      LoadOperator x = new LoadOperator();
      int n = jjtNodeName.length;

      for (int i = 1; i < n; ++i) { // skip "void"
        try {
          JavaOperator op = JavaOperator.findOperator(jjtNodeName[i]);
          // System.out.println("Loaded " + jjtNodeName[i]);
          operatorArray[i] = op;
        }
        catch (FluidRuntimeException e) {
          System.out.println("Could not load " + jjtNodeName[i]);
        }
      }
    }
  }

  // Do not do it here, the array is wrong size at beginning
  // static { loadOperators(); }

  public SimpleNode(Operator op) {
    super(tree, op);
  }

  /** Create a node with the operator that has the given name.
   * This is the entry for the parser.
   */
  public static JavaNode jjtCreate(int index) {
    JavaOperator op = operatorArray[index];
    if (op == null) {
      if (!loaded) {
        loadOperators();
        return jjtCreate(index);
      } else {
	      throw new FluidRuntimeException("Operator " + jjtNodeName[index] +
					" not found");
      }
    }
    return JavaNode.makeJavaNode(op);
    //return new SimpleNode(op);
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