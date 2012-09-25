package edu.cmu.cs.fluid.java;

import java.net.URI;

import com.surelogic.NonNull;
import com.surelogic.common.i18n.I18N;

import edu.cmu.cs.fluid.java.comment.IJavadocElement;

/**
 * This abstract class provides the basis to wrap an existing {@link ISrcRef}
 * instance and extend its behavior by overriding any of the methods.
 * <p>
 * Overriding methods can obtain a reference to the wrapped {@link ISrcRef}
 * instance via the protected field {@link WrappedSrcRef#f_wrapped}.
 */
public abstract class WrappedSrcRef implements ISrcRef {

  /**
   * the wrapped {@link ISrcRef} instance.
   */
  @NonNull
  protected final ISrcRef f_wrapped;

  public WrappedSrcRef(ISrcRef wrapped) {
    if (wrapped == null)
      throw new IllegalArgumentException(I18N.err(44, "wrapped"));
    f_wrapped = wrapped;
  }

  public String getEnclosingFile() {
    return f_wrapped.getEnclosingFile();
  }

  public URI getEnclosingURI() {
    return f_wrapped.getEnclosingURI();
  }

  public String getRelativePath() {
    return f_wrapped.getRelativePath();
  }

  public String getComment() {
    return f_wrapped.getComment();
  }

  public int getLength() {
    return f_wrapped.getLength();
  }

  public int getLineNumber() {
    return f_wrapped.getLineNumber();
  }

  public int getOffset() {
    return f_wrapped.getOffset();
  }

  public IJavadocElement getJavadoc() {
    return f_wrapped.getJavadoc();
  }

  public void clearJavadoc() {
    f_wrapped.clearJavadoc();
  }

  public Long getHash() {
    return f_wrapped.getHash();
  }

  public String getPackage() {
    return f_wrapped.getPackage();
  }

  public String getProject() {
    return f_wrapped.getProject();
  }

  public String getCUName() {
    return f_wrapped.getCUName();
  }

  public ISrcRef createSrcRef(int offset) {
    return f_wrapped.createSrcRef(offset);
  }

  public String getJavaId() {
    return f_wrapped.getJavaId();
  }
  
  public String getEnclosingJavaId() {
	  return f_wrapped.getEnclosingJavaId();
  }
}
