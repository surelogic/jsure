package edu.cmu.cs.fluid.java;

import java.util.StringTokenizer;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.JavaRef;

import edu.cmu.cs.fluid.java.comment.IJavadocElement;
import edu.cmu.cs.fluid.java.comment.IJavadocTag;
import edu.cmu.cs.fluid.java.comment.JavadocElement;
import edu.cmu.cs.fluid.java.comment.JavadocTag;

public final class FluidJavaRef extends JavaRef implements IFluidJavaRef {

  public static final class Builder extends JavaRef.Builder {

    private IJavadocElement f_javadocElement;

    public Builder(IFluidJavaRef copy) {
      super(copy);
      f_javadocElement = copy.getJavadoc();
    }

    public Builder(@NonNull String typeNameFullyQualifiedSureLogic) {
      super(typeNameFullyQualifiedSureLogic);
    }

    @Override
    public Builder setWithin(Within value) {
      super.setWithin(value);
      return this;
    }

    @Override
    public Builder setTypeType(TypeType value) {
      super.setTypeType(value);
      return this;
    }

    @Override
    public Builder setEclipseProjectName(String value) {
      super.setEclipseProjectName(value);
      return this;
    }

    @Override
    public Builder setLineNumber(int value) {
      super.setLineNumber(value);
      return this;
    }

    @Override
    public Builder setOffset(int value) {
      super.setOffset(value);
      return this;
    }

    @Override
    public Builder setLength(int value) {
      super.setLength(value);
      return this;
    }

    @Override
    public Builder setJavaId(String value) {
      super.setJavaId(value);
      return this;
    }

    @Override
    public Builder setEnclosingJavaId(String value) {
      super.setEnclosingJavaId(value);
      return this;
    }

    public Builder setJavadoc(final String declarationComment, final int declarationCommentOffset) {
      f_javadocElement = createIJavadocElement(declarationComment, declarationCommentOffset);
      return this;
    }

    @Override
    public IFluidJavaRef build() {
      return new FluidJavaRef(f_within, f_typeNameFullyQualifiedSureLogic, f_typeType, f_eclipseProjectName, f_lineNumber,
          f_offset, f_length, f_javaId, f_enclosingJavaId, f_javadocElement);
    }

    @Override
    public IFluidJavaRef buildOrNullOnFailure() {
      try {
        return build();
      } catch (Exception ignore) {
        // ignore
      }
      return null;
    }

    private IJavadocElement createIJavadocElement(final String declarationComment, final int declarationCommentOffset) {
      if (declarationComment == null)
        return null;

      JavadocElement doc = new JavadocElement(declarationCommentOffset, 0);
      String tag = null;
      StringBuilder sb = new StringBuilder();
      StringTokenizer st = new StringTokenizer(declarationComment, "\n");
      while (st.hasMoreTokens()) {
        String line = st.nextToken().trim();
        if (line.startsWith("@")) {
          if (tag != null) {
            doc.add(createTag(tag, sb.toString(), declarationCommentOffset));
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
        doc.add(createTag(tag, sb.toString(), declarationCommentOffset));
      }
      return doc;
    }

    private IJavadocTag createTag(final String tag, final String contents, final int declarationCommentOffset) {
      JavadocTag jt = new JavadocTag(declarationCommentOffset, 0, tag);
      jt.addString(contents);
      return jt;
    }
  }

  protected FluidJavaRef(final @NonNull Within within, final @NonNull String typeNameFullyQualifiedSureLogic,
      final @Nullable TypeType typeTypeOrNullifUnknown, final @Nullable String eclipseProjectNameOrNullIfUnknown,
      final int lineNumber, final int offset, final int length, final @Nullable String javaIdOrNull,
      final @Nullable String enclosingJavaIdOrNull, final @Nullable IJavadocElement javadocElementOrNull) {
    super(within, typeNameFullyQualifiedSureLogic, typeTypeOrNullifUnknown, eclipseProjectNameOrNullIfUnknown, lineNumber, offset,
        length, javaIdOrNull, enclosingJavaIdOrNull);
    f_javadocElement = javadocElementOrNull;
  }

  @Nullable
  private final IJavadocElement f_javadocElement;

  @Override
  public IJavadocElement getJavadoc() {
    return f_javadocElement;
  }
}
