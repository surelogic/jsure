package edu.cmu.cs.fluid.sea.proxy;

import java.util.Collections;
import java.util.Map;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop.Origin;

public class ProposedPromiseBuilder implements IDropBuilder {
  private final String annotation;
  private final String contents;
  private final Map<String, String> attrs;
  private final String replacedAnno;
  private final String replacedContents;
  private final Map<String, String> replacedAttrs;
  private final IRNode at;
  private final IRNode from;
  private final Origin origin;

  public ProposedPromiseBuilder(String anno, String contents, Map<String, String> attrs, String replacedAnno, String replaced,
      Map<String, String> replacedAttrs, IRNode at, IRNode from, Origin origin) {
    annotation = anno;
    this.contents = contents;
    this.attrs = attrs;
    this.replacedAnno = replacedAnno;
    replacedContents = replaced;
    this.replacedAttrs = replacedAttrs;
    this.at = at;
    if (at == null) {
      throw new IllegalArgumentException();
    }
    this.from = from;
    this.origin = origin;
  }

  public ProposedPromiseBuilder(String anno, String contents, String replacedContents, IRNode at, IRNode from, Origin origin) {
    this(anno, contents, Collections.<String, String> emptyMap(), null, replacedContents, Collections.<String, String> emptyMap(),
        at, from, origin);
  }

  public ProposedPromiseBuilder(String anno, String contents, IRNode at, IRNode from, Origin origin) {
    this(anno, contents, Collections.<String, String> emptyMap(), null, null, Collections.<String, String> emptyMap(), at, from,
        origin);
  }

  public ProposedPromiseDrop buildDrop() {
    // System.out.println("\tCreating proposal: "+annotation+" "+contents+"  from  "+DebugUnparser.toString(from));
    return new ProposedPromiseDrop(annotation, contents, attrs, replacedAnno, replacedContents, replacedAttrs, at, from, origin);
  }

  public int build() {
    buildDrop();
    return 1;
  }
}
