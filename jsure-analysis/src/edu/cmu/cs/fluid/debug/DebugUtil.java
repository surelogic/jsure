/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/debug/DebugUtil.java,v 1.5 2007/07/12 19:03:22 aarong Exp $*/
package edu.cmu.cs.fluid.debug;

import java.io.PrintStream;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.*;

public class DebugUtil {
  private static String getIndent(final int indent) {
    switch (indent) {
      case 0:
        return "";
      case 2:
        return "  ";          
      case 4:
        return "    ";
      case 6:
        return "      ";
      case 8:
        return "        ";
      case 10:
        return "          ";
      case 12:
        return "            ";          
      case 14:
        return "              ";
      case 16:
        return "                ";
      case 18:
        return "                  ";
      case 20:
        return "                    ";          
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i<indent; i++) {
      sb.append(' ');
    }
    return sb.toString();
  }
  
  public static void println(PrintStream out, int indent, String msg) {
    out.print(getIndent(indent));
    out.println(msg);
  }

  public static void dumpAncestors(PrintStream out, IRNode root) {
    IRNode n = root;
    while (n != null) {
      Operator op = JJNode.tree.getOperator(n);
      String id   = JJNode.getInfoOrNull(n);
      if (id != null) {
        out.println(n+" "+op.name()+" "+id);
      } else {
        out.println(n+" "+op.name());
      }
      n = JavaPromise.getParentOrPromisedFor(n);
    }
  }
}
