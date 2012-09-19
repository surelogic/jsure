package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.CATEGORY_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.CONTEXT_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.CUNIT_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FILE_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.FULL_TYPE_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.HASH_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.JAVA_ID_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.LENGTH_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.MESSAGE_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.OFFSET_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PATH_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PKG_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TYPE_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.URI_ATTR;
import static com.surelogic.common.xml.XMLReader.PROJECT_ATTR;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.xml.Entity;
import com.surelogic.common.xml.MoreInfo;
import com.surelogic.common.xml.SourceRef;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.ISupportingInformation;
import com.surelogic.dropsea.ir.Category;
import com.surelogic.dropsea.irfree.DropTypeUtility;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.AbstractSrcRef;
import edu.cmu.cs.fluid.java.ISrcRef;

public class IRFreeDrop implements IDrop {

  @NonNull
  private final Entity f_entity; // TODO PERHAPS REMOVE IN FUTURE

  @Deprecated
  public @NonNull
  Entity getEntity() {
    return f_entity;
  }

  static {
    for (Category c : Category.getAll()) {
      Entity.internString(c.getMessage());
    }
  }

  private final List<IRFreeProposedPromiseDrop> proposals = new ArrayList<IRFreeProposedPromiseDrop>(0);
  protected Category category;
  private ISrcRef ref;
  private List<ISupportingInformation> supportingInfos;

  public void snapshotAttrs(XMLCreator.Builder s) {
    for (Map.Entry<String, String> a : f_entity.getAttributes().entrySet()) {
      s.addAttribute(a.getKey(), a.getValue());
    }
  }

  public Long getTreeHash() {
    String hash = getEntity().getAttribute(HASH_ATTR);
    if (hash == null) {
      return Long.valueOf(0);
    }
    return Long.parseLong(hash);
  }

  public Long getContextHash() {
    return Long.parseLong(getEntity().getAttribute(CONTEXT_ATTR));
  }

  public void addProposal(IRFreeProposedPromiseDrop info) {
    proposals.add(info);
  }

  public IRFreeDrop(Entity e) {
    if (e == null)
      throw new IllegalArgumentException(I18N.err(44, "e"));
    f_entity = e;
    category = Category.getInstance(e.getAttribute(CATEGORY_ATTR));
  }

  public void finishInit() {
    if (f_entity.getSource() != null) {
      ref = makeSrcRef(f_entity.getSource());
    } else {
      ref = null;
    }
    if (!f_entity.getInfos().isEmpty()) {
      supportingInfos = new ArrayList<ISupportingInformation>();
      for (MoreInfo i : f_entity.getInfos()) {
        supportingInfos.add(makeSupportingInfo(i));
      }
    } else {
      supportingInfos = Collections.emptyList();
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

  @Nullable
  public Category getCategory() {
    return category;
  }

  @NonNull
  public String getMessage() {
    final String result = getEntity().getAttribute(MESSAGE_ATTR);
    if (result != null)
      return result;
    else
      return getClass().getSimpleName() + " (EMPTY)";
  }

  public ISrcRef getSrcRef() {
    return ref;
  }

  @NonNull
  public String getTypeName() {
    final String result = getEntity().getAttribute(TYPE_ATTR);
    if (result != null)
      return result;
    else
      return getClass().getName();
  }

  public final boolean instanceOf(Class<?> type) {
    final String thisTypeName = getEntity().getAttribute(FULL_TYPE_ATTR);
    final Class<?> thisType = DropTypeUtility.findType(thisTypeName);
    if (thisType != null)
      return type.isAssignableFrom(thisType);
    else
      return false;
  }

  public Collection<? extends IProposedPromiseDrop> getProposals() {
    return proposals;
  }

  public Collection<ISupportingInformation> getSupportingInformation() {
    return supportingInfos;
  }

  // @Override
  public String getXMLElementName() {
    return f_entity.getEntityName();
  }
}
