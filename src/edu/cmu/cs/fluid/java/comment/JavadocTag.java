/*
 * Created on Jul 8, 2004
 *
 */
package edu.cmu.cs.fluid.java.comment;

/**
 * @author Edwin
 *
 */
public class JavadocTag extends JavadocElement implements IJavadocTag {
  final String tag;
  
  /**
   * @param pos
   * @param len
   */
  public JavadocTag(int pos, int len, String tag) {
    super(pos, len);
    this.tag = tag;
  }

  public String getTag() {
    return tag;
  }

  /**
   * @param string
   */
  public void addString(String string) {
    add(string);
  }
}
