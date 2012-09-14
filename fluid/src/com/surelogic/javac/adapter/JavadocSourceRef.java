package com.surelogic.javac.adapter;

import java.util.StringTokenizer;

import com.surelogic.javac.FileResource;

import edu.cmu.cs.fluid.java.comment.IJavadocElement;
import edu.cmu.cs.fluid.java.comment.IJavadocTag;
import edu.cmu.cs.fluid.java.comment.JavadocElement;
import edu.cmu.cs.fluid.java.comment.JavadocTag;

public class JavadocSourceRef extends SourceRef {
  /**
   * Leading comment or JavaDoc, or null if none.
   */
  private final String f_leadingComment;

  public JavadocSourceRef(FileResource cuRef, long start, long end, long line, String comment) {
    super(cuRef, start, end, line);
    f_leadingComment = comment;
  }

  public IJavadocElement getJavadoc() {
    JavadocElement doc = new JavadocElement(start, 0);
    String tag = null;
    StringBuilder sb = new StringBuilder();
    StringTokenizer st = new StringTokenizer(f_leadingComment, "\n");
    while (st.hasMoreTokens()) {
      String line = st.nextToken().trim();
      if (line.startsWith("@")) {
        if (tag != null) {
          doc.add(createTag(tag, sb.toString()));
          sb.setLength(0);
        }
        // Find the first space after the tag
        int firstSpace = line.indexOf(' ');
        while (firstSpace >= 0 && !Character.isLetter(line.charAt(firstSpace - 1))) {
          firstSpace = line.indexOf(' ', firstSpace + 1);
        }
        if (firstSpace >= 0) {
          tag = line.substring(1, firstSpace).trim();
          sb.append(line.substring(firstSpace + 1));
        } else {
          tag = line.substring(1).trim();
        }
        // System.out.println("@"+tag+" -- "+sb);
      } else if (tag != null) {
        sb.append(' ').append(line);
        // System.out.println("@"+tag+" -- "+sb);
      } else {
        // System.out.println("Skipping description: "+line);
        continue;
      }
    }
    if (tag != null) {
      doc.add(createTag(tag, sb.toString()));
    }
    return doc;
  }

  private IJavadocTag createTag(String tag, String contents) {
    JavadocTag jt = new JavadocTag(this.start, 0, tag);
    jt.addString(contents);
    return jt;
  }
}
