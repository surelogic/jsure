package edu.cmu.cs.fluid.sea.xml;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xml.sax.Attributes;

import com.surelogic.common.xml.Entity;
import com.surelogic.common.xml.MoreInfo;
import com.surelogic.common.xml.SourceRef;
import com.surelogic.common.xml.XMLCreator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.AbstractSrcRef;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.DropPredicate;
import edu.cmu.cs.fluid.sea.IDrop;
import edu.cmu.cs.fluid.sea.IProposedPromiseDrop;
import edu.cmu.cs.fluid.sea.ISupportingInformation;

public class IRFreeDrop extends Entity implements IDrop {
  static {
    for (Category c : Category.getAll()) {
      internString(c.getMessage());
    }
  }

  final List<IRFreeDrop> dependents;
  final List<IRFreeDrop> deponents;
  final List<IRFreeProposedPromiseDrop> proposals;
  Category category;
  ISrcRef ref;
  List<ISupportingInformation> supportingInfos;

  public void snapshotAttrs(XMLCreator.Builder s) {
    for (Map.Entry<String, String> a : attributes.entrySet()) {
      s.addAttribute(a.getKey(), a.getValue());
    }
    // TODO handle src refs specially?
  }

  public Long getTreeHash() {
    String hash = getAttribute(HASH_ATTR);
    if (hash == null) {
      return Long.valueOf(0);
    }
    return Long.parseLong(hash);
  }

  public Long getContextHash() {
    return Long.parseLong(getAttribute(CONTEXT_ATTR));
  }

  void addProposal(IRFreeProposedPromiseDrop info) {
    proposals.add(info);
  }

  void addDeponent(IRFreeDrop info) {
    deponents.add(info);
  }

  void addDependent(IRFreeDrop info) {
    dependents.add(info);
  }

  IRFreeDrop(String name, Attributes a) {
    super(name, a);
    if (name.endsWith("drop")) {
      dependents = new ArrayList<IRFreeDrop>(1);
      deponents = new ArrayList<IRFreeDrop>(1);
      proposals = new ArrayList<IRFreeProposedPromiseDrop>(0);
    } else {
      dependents = Collections.emptyList();
      deponents = Collections.emptyList();
      proposals = Collections.emptyList();
    }
    category = Category.getInstance(getAttribute(CATEGORY_ATTR));
  }

  void finishInit() {
    if (getSource() != null) {
      ref = makeSrcRef(getSource());
    } else {
      ref = null;
    }
    if (!getInfos().isEmpty()) {
      supportingInfos = new ArrayList<ISupportingInformation>();
      for (MoreInfo i : getInfos()) {
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

  static ISrcRef makeSrcRef(final SourceRef ref) {
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
      public Object getEnclosingFile() {
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

  public boolean isValid() {
    return true;
  }

  public void setCategory(Category c) {
    category = c;
  }

  public Category getCategory() {
    return category;
  }

  public String getMessage() {
    return getAttribute(MESSAGE_ATTR);
  }

  public ISrcRef getSrcRef() {
    return ref;
  }

  public String getTypeName() {
    return getAttribute(TYPE_ATTR);
  }

  public boolean instanceOf(Class<?> type) {
    final String thisTypeName = getAttribute(SeaSnapshot.useFullType ? FULL_TYPE_ATTR : TYPE_ATTR);
    final Class<?> thisType = SeaSnapshot.findType(thisTypeName);
    if (thisType != null)
      return type.isAssignableFrom(thisType);
    else
      return false;
  }

  public boolean hasMatchingDeponents(DropPredicate p) {
    for (IRFreeDrop i : deponents) {
      if (p.match(i)) {
        return true;
      }
    }
    return false;
  }

  // @Override
  public Set<? extends IDrop> getMatchingDeponents(DropPredicate p) {
    final Set<IRFreeDrop> result = new HashSet<IRFreeDrop>();
    for (IRFreeDrop i : deponents) {
      if (p.match(i)) {
        result.add(i);
      }
    }
    return result;
  }

  // @Override
  public boolean hasMatchingDependents(DropPredicate p) {
    for (IRFreeDrop i : dependents) {
      if (p.match(i)) {
        return true;
      }
    }
    return false;
  }

  // @Override
  public Set<? extends IDrop> getMatchingDependents(DropPredicate p) {
    final Set<IRFreeDrop> result = new HashSet<IRFreeDrop>();
    for (IRFreeDrop i : dependents) {
      if (p.match(i)) {
        result.add(i);
      }
    }
    return result;
  }

  public Collection<? extends IProposedPromiseDrop> getProposals() {
    return proposals;
  }

  public Collection<ISupportingInformation> getSupportingInformation() {
    return supportingInfos;
  }

  // @Override
  public String getXMLElementName() {
    return getEntityName();
  }
}
