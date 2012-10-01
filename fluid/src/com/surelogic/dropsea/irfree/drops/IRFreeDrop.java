package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.CATEGORY_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.CONTEXT_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.CUNIT_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FILE_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.HASH_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.JAVA_ID_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.LENGTH_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.MESSAGE_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.MESSAGE_ID;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.OFFSET_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PATH_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PKG_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.URI_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.WITHIN_DECL_ATTR;
import static com.surelogic.common.xml.XMLReader.PROJECT_ATTR;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.IJavaRef;
import com.surelogic.common.JavaRef;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.xml.Entity;
import com.surelogic.common.xml.SourceRef;
import com.surelogic.dropsea.IDrop;

import edu.cmu.cs.fluid.java.AbstractSrcRef;
import edu.cmu.cs.fluid.java.ISrcRef;

public class IRFreeDrop implements IDrop {
  @Deprecated
  private final String f_xmlElementName; // TODO remove when SeaSummary is
                                         // removed

  @NonNull
  private final Class<?> f_irDropSeaClass;
  /**
   * This collection is {@code null} until some exist&mdash;most drops have no
   * proposed promises.
   */
  @Nullable
  private List<IRFreeProposedPromiseDrop> f_proposedPromises = null;
  @Nullable
  private String f_categorizingMessage = null;
  @Nullable
  private ISrcRef f_srcRef = null;
  @Nullable
  private IJavaRef f_javaRef = null;
  @NonNull
  private final String f_message;
  @NonNull
  private final String f_messageCanonical;
  @NonNull
  private final Long f_treeHash;
  @NonNull
  private final Long f_contextHash;
  /**
   * This collection is {@code null} until some exist&mdash;most drops have no
   * hints.
   */
  @Nullable
  private List<IRFreeHintDrop> f_analysisHints = null;

  void addProposal(IRFreeProposedPromiseDrop info) {
    if (f_proposedPromises == null) {
      f_proposedPromises = new ArrayList<IRFreeProposedPromiseDrop>(1);
    }
    f_proposedPromises.add(info);
  }

  void setSrcRef(ISrcRef value) {
    f_srcRef = value;
  }

  void setJavaRef(IJavaRef value) {
    f_javaRef = value;
  }

  void addHint(IRFreeHintDrop hint) {
    if (f_analysisHints == null) {
      f_analysisHints = new ArrayList<IRFreeHintDrop>(1);
    }
    f_analysisHints.add(hint);
  }

  IRFreeDrop(Entity e, Class<?> irClass) {
    if (e == null)
      throw new IllegalArgumentException(I18N.err(44, "e"));
    if (irClass == null)
      throw new IllegalArgumentException(I18N.err(44, "irClass"));
    f_irDropSeaClass = irClass;

    f_categorizingMessage = e.getAttribute(CATEGORY_ATTR);

    final String message = e.getAttribute(MESSAGE_ATTR);
    if (message != null)
      f_message = message;
    else
      f_message = getClass().getSimpleName() + " (EMPTY)";

    final String messageCanonical = e.getAttribute(MESSAGE_ID);
    if (messageCanonical != null)
      f_messageCanonical = messageCanonical;
    else
      f_messageCanonical = f_message; // per Javadoc on IDrop

    final String hash = e.getAttribute(HASH_ATTR);
    Long treeHash = null;
    if (hash != null) {
      try {
        treeHash = Long.parseLong(hash);
      } catch (NumberFormatException nfe) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(249, hash, HASH_ATTR), nfe);
      }
    }
    f_treeHash = treeHash != null ? treeHash : Long.valueOf(0);

    final String chash = e.getAttribute(CONTEXT_ATTR);
    Long contextHash = null;
    if (chash != null) {
      try {
        contextHash = Long.parseLong(chash);
      } catch (NumberFormatException nfe) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(249, chash, CONTEXT_ATTR), nfe);
      }
    }
    f_contextHash = contextHash != null ? contextHash : Long.valueOf(0);

    f_xmlElementName = e.getEntityName();
  }

  @Nullable
  public String getCategorizingMessage() {
    return f_categorizingMessage;
  }

  @NonNull
  public String getMessage() {
    return f_message;
  }

  @NonNull
  public String getMessageCanonical() {
    return f_messageCanonical;
  }

  @Nullable
  public ISrcRef getSrcRef() {
    return f_srcRef;
  }

  @Nullable
  public IJavaRef getJavaRef() {
    return f_javaRef;
  }

  @NonNull
  public Class<?> getIRDropSeaClass() {
    return f_irDropSeaClass;
  }

  public final boolean instanceOfIRDropSea(Class<?> type) {
    if (type == null)
      return false;

    return type.isAssignableFrom(f_irDropSeaClass);
  }

  @NonNull
  public Collection<IRFreeProposedPromiseDrop> getProposals() {
    if (f_proposedPromises != null)
      return f_proposedPromises;
    else
      return Collections.emptyList();
  }

  @NonNull
  public final Collection<IRFreeHintDrop> getHints() {
    if (f_analysisHints != null)
      return f_analysisHints;
    else
      return Collections.emptyList();
  }

  public Long getTreeHash() {
    return f_treeHash;
  }

  public Long getContextHash() {
    return f_contextHash;
  }

  // TODO remove when SeaSummary is removed
  public String getXMLElementName() {
    return f_xmlElementName;
  }

  // public void snapshotAttrs(XMLCreator.Builder s) {
  // for (Map.Entry<String, String> a : f_entity.getAttributes().entrySet()) {
  // s.addAttribute(a.getKey(), a.getValue());
  // }
  // }

  static int convert(String val) {
    if (val == null) {
      return 0;
    } else {
      return Integer.valueOf(val);
    }
  }

  static IJavaRef makeJavaRefFromSrcRef(SourceRef ref) {
    if (ref == null) {
      return null;
    }
    final int line = Integer.valueOf(ref.getLine());
    final String pkg = ref.getAttribute(PKG_ATTR);
    final String path = ref.getAttribute(PATH_ATTR);
    final String cuName = ref.getAttribute(CUNIT_ATTR);
    final String javaId = ref.getAttribute(JAVA_ID_ATTR);
    final String enclosingId = ref.getAttribute(WITHIN_DECL_ATTR);
    final String project = ref.getAttribute(PROJECT_ATTR);

    final int offset = convert(ref.getAttribute(OFFSET_ATTR));
    final int length = convert(ref.getAttribute(LENGTH_ATTR));

    if (cuName.contains("[]")) {
      System.out.println("BOGUS:");
      System.out.println(" -- (path) " + path);
      System.out.println(" --  (pkg) " + pkg);
      System.out.println("--    (cu) " + cuName);
      return null;
    } else {
      // Test of IJavaRef
      boolean classExt = cuName.endsWith(".class");
      final String classNm = classExt ? cuName.substring(0, cuName.length() - 6) : cuName;
      // Note that the default package is "" in the SourceRef instances
      final String jarStyleName = pkg + "/" + classNm;
      final JavaRef.Builder builder = new JavaRef.Builder(jarStyleName);
      builder.setRelativePath(path);
      builder.setEclipseProjectName(project);
      if (classExt)
        builder.setWithin(IJavaRef.Within.JAR_FILE);
      if (line < Integer.MAX_VALUE)
        builder.setLineNumber(line);
      if (offset < Integer.MAX_VALUE)
        builder.setOffset(offset);
      if (length < Integer.MAX_VALUE)
        builder.setLength(length);
      builder.setJavaId(javaId);
      builder.setEnclosingJavaId(enclosingId);
      return builder.build();
    }
  }

  static ISrcRef makeSrcRef(SourceRef ref) {
    if (ref == null) {
      return null;
    }
    final int line = Integer.valueOf(ref.getLine());
    final String pkg = ref.getAttribute(PKG_ATTR);
    final String file = ref.getAttribute(FILE_ATTR);
    final String path = ref.getAttribute(PATH_ATTR);
    final String cuName = ref.getAttribute(CUNIT_ATTR);
    final String javaId = ref.getAttribute(JAVA_ID_ATTR);
    final String enclosingId = ref.getAttribute(WITHIN_DECL_ATTR);
    final String project = ref.getAttribute(PROJECT_ATTR);
    final String hash = ref.getAttribute(HASH_ATTR);
    final String uri = ref.getAttribute(URI_ATTR);

    final int offset = convert(ref.getAttribute(OFFSET_ATTR));
    final int length = convert(ref.getAttribute(LENGTH_ATTR));

    return new AbstractSrcRef() {

      @Override
      public boolean equals(Object o) {
        if (this.getClass().isInstance(o)) {
          final ISrcRef other = (ISrcRef) o;
          return getOffset() == other.getOffset() && getCUName().equals(other.getCUName())
              && getPackage().equals(other.getPackage());
        }
        return false;
      }

      @Override
      public ISrcRef createSrcRef(int offset) {
        return this;
      }

      public String getJavaId() {
        return javaId;
      }

      public String getEnclosingJavaId() {
        return enclosingId;
      }

      public String getCUName() {
        return cuName;
      }

      @Override
      public String getEnclosingFile() {
        return file;
      }

      @Override
      public String getRelativePath() {
        return path;
      }

      @Override
      public URI getEnclosingURI() {
        if (uri != null) {
          try {
            return new URI(uri);
          } catch (URISyntaxException e) {
            System.out.println("Couldn't parse as URI: " + uri);
          }
        }
        return null;
      }

      @Override
      public int getOffset() {
        return offset;
      }

      @Override
      public int getLength() {
        return length;
      }

      public Long getHash() {
        if (hash == null) {
          throw new UnsupportedOperationException();
        } else {
          return Long.valueOf(hash);
        }
      }

      @Override
      public int getLineNumber() {
        return line;
      }

      public String getPackage() {
        return pkg;
      }

      public String getProject() {
        return project;
      }
    };
  }
}
