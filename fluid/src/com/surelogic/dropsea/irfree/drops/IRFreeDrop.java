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
import static com.surelogic.common.xml.XMLReader.PROJECT_ATTR;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.xml.Entity;
import com.surelogic.common.xml.MoreInfo;
import com.surelogic.common.xml.SourceRef;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.ISupportingInformation;
import com.surelogic.dropsea.ir.Category;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.AbstractSrcRef;
import edu.cmu.cs.fluid.java.ISrcRef;

public class IRFreeDrop implements IDrop {

  static {
    for (Category c : Category.getAll()) {
      Entity.internString(c.getMessage());
    }
  }

  @NonNull
  private final Entity f_entity; // TODO PERHAPS REMOVE IN FUTURE

  @Deprecated
  public @NonNull
  Entity getEntity() {
    return f_entity;
  }

  @NonNull
  private final Class<?> f_irClass;
  @NonNull
  private final List<IRFreeProposedPromiseDrop> f_proposedPromises = new ArrayList<IRFreeProposedPromiseDrop>(0);
  @Nullable
  protected Category f_category;
  @Nullable
  private ISrcRef f_srcRef;
  @NonNull
  private final List<ISupportingInformation> f_supportingInformation = new ArrayList<ISupportingInformation>(0);
  @NonNull
  private final String f_message;
  @NonNull
  private final String f_messageCanonical;
  @NonNull
  private final Long f_treeHash;
  @NonNull
  private final Long f_contextHash;

  public void addProposal(IRFreeProposedPromiseDrop info) {
    f_proposedPromises.add(info);
  }

  public IRFreeDrop(Entity e, Class<?> irClass) {
    if (e == null)
      throw new IllegalArgumentException(I18N.err(44, "e"));
    if (irClass == null)
      throw new IllegalArgumentException(I18N.err(44, "irClass"));
    f_irClass = irClass;
    f_entity = e;
    f_category = Category.getInstance(e.getAttribute(CATEGORY_ATTR));

    final String message = e.getAttribute(MESSAGE_ATTR);
    if (message != null)
      f_message = message;
    else
      f_message = getClass().getSimpleName() + " (EMPTY)";

    final String messageCanonical = e.getAttribute(MESSAGE_ID);
    if (messageCanonical != null)
      f_messageCanonical = messageCanonical;
    else
      f_messageCanonical = getClass().getSimpleName() + " (EMPTY)";

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
  }

  public void finishInit() {
    final SourceRef sr = f_entity.getSource();
    f_srcRef = sr != null ? makeSrcRef(sr) : null;

    for (MoreInfo i : f_entity.getInfos()) {
      f_supportingInformation.add(makeSupportingInfo(i));
    }
  }

  @Nullable
  public Category getCategory() {
    return f_category;
  }

  @NonNull
  public String getMessage() {
    return f_message;
  }

  @NonNull
  public String getMessageCanonical() {
    return f_messageCanonical;
  }

  public ISrcRef getSrcRef() {
    return f_srcRef;
  }

  @NonNull
  public String getTypeName() {
    return f_irClass.getName();
  }

  public final boolean instanceOf(Class<?> type) {
    if (type == null)
      return false;

    return type.isAssignableFrom(f_irClass);
  }

  public Collection<? extends IProposedPromiseDrop> getProposals() {
    return f_proposedPromises;
  }

  public Collection<ISupportingInformation> getSupportingInformation() {
    return f_supportingInformation;
  }

  public Long getTreeHash() {
    return f_treeHash;
  }

  public Long getContextHash() {
    return f_contextHash;
  }

  public String getXMLElementName() {
    return getEntity().getEntityName();
  }

  public void snapshotAttrs(XMLCreator.Builder s) {
    for (Map.Entry<String, String> a : f_entity.getAttributes().entrySet()) {
      s.addAttribute(a.getKey(), a.getValue());
    }
  }

  private ISupportingInformation makeSupportingInfo(final MoreInfo i) {
    return new ISupportingInformation() {
      final ISrcRef ref = makeSrcRef(i.source);

      public IRNode getLocation() {
        return null;
      }

      public String getMessage() {
        return i.message;
      }

      public ISrcRef getSrcRef() {
        return ref;
      }

      public boolean sameAs(IRNode link, int num, Object[] args) {
        throw new UnsupportedOperationException();
      }

      public boolean sameAs(IRNode link, String message) {
        throw new UnsupportedOperationException();
      }
    };
  }

  public static ISrcRef makeSrcRef(final SourceRef ref) {
    if (ref == null) {
      return null;
    }
    final int line = Integer.valueOf(ref.getLine());
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
        return ref.getAttribute(JAVA_ID_ATTR);
      }

      public String getCUName() {
        return ref.getAttribute(CUNIT_ATTR);
      }

      @Override
      public String getEnclosingFile() {
        return ref.getAttribute(FILE_ATTR);
      }

      @Override
      public String getRelativePath() {
        return ref.getAttribute(PATH_ATTR);
      }

      @Override
      public URI getEnclosingURI() {
        String uri = ref.getAttribute(URI_ATTR);
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
        String offset = ref.getAttribute(OFFSET_ATTR);
        if (offset == null) {
          return 0;
        } else {
          return Integer.valueOf(offset);
        }
      }

      @Override
      public int getLength() {
        String offset = ref.getAttribute(LENGTH_ATTR);
        if (offset == null) {
          return 0;
        } else {
          return Integer.valueOf(offset);
        }
      }

      public Long getHash() {
        String hash = ref.getAttribute(HASH_ATTR);
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
        return ref.getAttribute(PKG_ATTR);
      }

      public String getProject() {
        return ref.getAttribute(PROJECT_ATTR);
      }
    };
  }
}
