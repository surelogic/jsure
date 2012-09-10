package edu.cmu.cs.fluid.java;

import java.net.URI;

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
   * the non-{@link null} wrapped {@link ISrcRef} instance.
   */
  protected final ISrcRef f_wrapped;

  public WrappedSrcRef(ISrcRef wrapped) {
    if (wrapped == null)
      throw new IllegalArgumentException(I18N.err(44, "wrapped"));
    f_wrapped = wrapped;
  }

  @Override
  public Object getEnclosingFile() {
    return f_wrapped.getEnclosingFile();
  }

  @Override
  public URI getEnclosingURI() {
    return f_wrapped.getEnclosingURI();
  }

  @Override
  public String getRelativePath() {
    return f_wrapped.getRelativePath();
  }

  @Override
  public String getComment() {
    return f_wrapped.getComment();
  }

  @Override
  public int getLength() {
    return f_wrapped.getLength();
  }

  @Override
  public int getLineNumber() {
    return f_wrapped.getLineNumber();
  }

  @Override
  public int getOffset() {
    return f_wrapped.getOffset();
  }

  @Override
  public IJavadocElement getJavadoc() {
    return f_wrapped.getJavadoc();
  }

  @Override
  public void clearJavadoc() {
    f_wrapped.clearJavadoc();
  }

  @Override
  public Long getHash() {
    return f_wrapped.getHash();
  }

  @Override
  public String getPackage() {
    return f_wrapped.getPackage();
  }

  @Override
  public String getProject() {
    return f_wrapped.getProject();
  }

  @Override
  public String getCUName() {
    return f_wrapped.getCUName();
  }

  @Override
  public ISrcRef createSrcRef(int offset) {
    return f_wrapped.createSrcRef(offset);
  }

  @Override
  public String getJavaId() {
    return f_wrapped.getJavaId();
  }
}
