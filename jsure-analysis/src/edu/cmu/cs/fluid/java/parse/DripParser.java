package edu.cmu.cs.fluid.java.parse;

import java.util.Iterator;
import java.util.NoSuchElementException;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DripOperator;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.parse.JJOperator;
import edu.cmu.cs.fluid.parse.ParseError;
import edu.cmu.cs.fluid.tree.Operator;

public class DripParser extends JavaParser {
  public DripParser(java.io.InputStream stream) {
    super(stream);
  }

  public static void main(String args[]) {
    JavaNode.dumpTree(System.out, parse(args), 1);
  }

  public static JavaNode parse(String args[]) {
    try {
      JavaNode n = JavaParser.parse(args);
      checkDripTree(n);
      return n;
    } catch (ParseError e) {
      System.out.println("Oops.");
      System.out.println(e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  public static void checkDripTree(IRNode n) throws ParseError {
    Iterator enm = JJOperator.tree.bottomUp(n);
    while (true) {
      try {
        IRNode x = (IRNode) enm.next();
        Operator op = JJOperator.tree.getOperator(x);
        if (!(op instanceof DripOperator)) {
          throw new ParseError(op.name() + " is not part of the Drip subset");
        }
      } catch (NoSuchElementException e) {
        break;
      }
    }
  }
}
