/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/util/ParseUtil.java,v 1.1 2007/04/11 19:45:26 aarong Exp $*/
package edu.cmu.cs.fluid.java.util;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.CharLiteral;
import edu.cmu.cs.fluid.java.operator.IntLiteral;

/**
 * Class that contains utility methods for dealing with Java parsing issues.
 * @author aarong
 */
public final class ParseUtil {
  /** 
   * private constructor to prevent instantiation of class.
   */
  private ParseUtil() {
    // Do nothing
  }
  
  
  
  /**
   * Given a CharLiteral node, return the character represented by that
   * literal.  See JLS 3, Sections 3.10.4 and 3.10.6.
   * @param node A CharLiteral node
   * @return The character represented by that node.
   */
  public static int decodeCharLiteral(final IRNode node) {
    /* The token is literal bounded by ' on each side. */
    final String token = CharLiteral.getToken(node);
    
    /* The simple case is that we have a in-place single character */
    if (token.length() == 3) {
      return token.charAt(1);
    } else {
      /* Otherwise we have an escape sequence: token.charAt(1) == '\\' */
      final char esc = token.charAt(2);
      if (esc == 'b') { return '\b'; }
      else if (esc == 't') { return '\t'; }
      else if (esc == 'n') { return '\n'; }
      else if (esc == 'f') { return '\f'; }
      else if (esc == 'r') { return '\r'; }
      else if (esc == '\"') { return '\"'; }
      else if (esc == '\'') { return '\''; }
      else if (esc == '\\') { return '\\'; }
      else if (esc == 'u') { // unicode escape
        // snip off initial "'\\u" and trailing "'"
        final String hexString = token.substring(3, token.length()-1);
        return Integer.parseInt(hexString, 16);
      } else { // must be an octal escape
        // snip off initial "'\\" and trailing "'"
        final String octalString = token.substring(2, token.length()-1);
        return Integer.parseInt(octalString, 8);
      }
    }
  }
  
  /**
   * Given an IntLiteral node, return whether the literal is a long value. 
   * See JLS 3, Section 3.10.1.
   * @param node An IntLiteral node
   * @return <tt>true</tt> if the literal is a long, i.e., it has 
   * a trailing 'l' or 'L'.
   */
  public static boolean isIntLiteralLong(final IRNode node) {
    final String token = IntLiteral.getToken(node);
    final char lastChar = token.charAt(token.length()-1);
    return (lastChar == 'l' || lastChar == 'L');
  }
  
  /**
   * Given an IntLiteral node, return whether the literal is an integer.
   * See JLS 3, Section 3.10.1.
   * @param node An IntLiteral node
   * @return <tt>true</tt> if the literal is an integer, i.e., doesn't 
   * have a trailing 'l' or 'L'.
   */
  public static boolean isIntLiteralInt(final IRNode node) {
    return !isIntLiteralLong(node);
  }
  
  /**
   * Given an IntLiteral node, return the Java <code>int</code> that it represents.
   * See JLS 3, Section 3.10.1.  This method is not the same as 
   * {@link #decodeIntLiteralAsLong}: If given an IntLiteral that represents 
   * a <code>long</code> that is not representable as an <code>int</code>, then
   * <code>NumberFormatException</code> is thrown. 
   * @param node An IntLiteral node
   * @return The Java <code>int</code> represented by the node.
   * @throws NumberFormatException Thrown if the literal is not representable
   * as an <code>int</code>.  
   * @see #isIntLiteralInt(IRNode), {@link #isIntLiteralLong(IRNode)}
   */
  public static int decodeIntLiteralAsInt(final IRNode node) {
    final String token = IntLiteral.getToken(node);
    return Integer.decode(token).intValue();
  }
  
  /**
   * Given an IntLiteral node, return the Java <code>long</code> that it represents.
   * See JLS 3, Section 3.10.1.  This method is not the same as 
   * {@link #decodeIntLiteralAsInt}. 
   * @param node An IntLiteral node
   * @return The Java <code>long</code> represented by the node.
   * @see #isIntLiteralInt(IRNode), {@link #isIntLiteralLong(IRNode)}
   */
  public static long decodeIntLiteralAsLong(final IRNode node) {
    final String token = IntLiteral.getToken(node);
    return Long.decode(token).longValue();
  }
}
