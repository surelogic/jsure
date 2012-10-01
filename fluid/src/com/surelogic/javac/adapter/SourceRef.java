package com.surelogic.javac.adapter;

import java.net.URI;

import com.surelogic.javac.FileResource;

import edu.cmu.cs.fluid.java.ISrcRef;

public class SourceRef extends AbstractRef {
  final FileResource ref;
  final int start, end;

  public SourceRef(FileResource cuRef, long start, long end, long line) {
    super(line);
    ref = cuRef;
    this.start = (int) start;
    this.end = (int) end;
  }

  public ISrcRef createSrcRef(int offset) {
    SourceRef src = new SourceRef(ref, offset, offset, f_line);
    return src;
    // return this;
  }

  public String getCUName() {
    return ref.getCUName();
  }

  public String getEnclosingFile() {
    return ref.getURI().toString();
  }

  public URI getEnclosingURI() {
    return ref.getURI();
  }

  public String getRelativePath() {
    return ref.getRelativePath();
  }

  public Long getHash() {
    // throw new UnsupportedOperationException();
    return null;
  }

  public int getLength() {
    return end - start;
  }

  public int getLineNumber() {
    return f_line;
  }

  public int getOffset() {
    return start;
  }

  public String getPackage() {
    return ref.getPackage();
  }

  public String getProject() {
    return ref.getProjectName();
  }
}
