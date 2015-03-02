/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/JavaPredicates.java,v 1.4 2003/07/02 20:19:58 thallora Exp $ */
package edu.cmu.cs.fluid.java;

import edu.cmu.cs.fluid.CommonStrings;

public class JavaPredicates
{
  private static String[] keywords = 
  {
    "abstract",
    "boolean",
    "break",
    "byte",
    "case",
    "catch",
    "char",
    "class",
    "const",
    "continue",
    "default",
    "do",
    "double",
    "else",
    "extends",
    "final",
    "finally",
    "float",
    "for",
    "goto",
    "if",
    "implements",
    "import",
    "instanceof",
    "int",
    "interface",
    "long",
    "native",
    "new",
    "package",
    "private",
    "protected",
    "public",
    "return",
    "short",
    "static",
    "super",
    "switch",
    "synchronized",
    "this",
    "throw",
    "throws",
    "transient",
    "try",
    "void",
    "volatile",
    "while"
  };

  private static String trueLiteral = "true";
  private static String falseLiteral = "false";

  private static String nullLiteral = "null";

  /**
   * Test if a string is a Java keyword.
   */
  public static boolean isKeyword( final String s )
  {
    final String s2 = CommonStrings.intern(s);
    for( int i = 0; i < keywords.length; i++ ) {
      if( keywords[i] == s2 ) return true;
    }
    return false;
  }

  /**
   * Test if a string is a Java boolean literal.
   */
  public static boolean isBooleanLiteral( final String s )
  {
	final String s2 = CommonStrings.intern(s);
    return (s2 == trueLiteral) || (s2 == falseLiteral);
  }

  /**
   * Test if a string is the Java null literal.
   */
  public static boolean isNullLiteral( final String s )
  {
    return (nullLiteral == CommonStrings.intern(s));
  }

  /**
   * Test if a string is a Java identifier.
   */
  public static boolean isIdentifier( final String s )
  {
    if( !isKeyword( s ) && !isBooleanLiteral( s ) && 
        !isNullLiteral( s ) && (s.length() > 0))
    {
      if( Character.isJavaIdentifierStart( s.charAt( 0 ) ) )
      {
        for( int i = 1; i < s.length(); i++ ) {
          if( !Character.isJavaIdentifierPart( s.charAt( i ) ) ) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }
}
