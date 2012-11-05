package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.CATEGORY_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.CUNIT_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.DIFF_INFO;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.JAVA_ID_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.JAVA_REF;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.LENGTH_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.MESSAGE_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.MESSAGE_ID;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.OFFSET_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PKG_ATTR;
import static com.surelogic.common.xml.XMLReader.PROJECT_ATTR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.Decl;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ref.JavaRef;
import com.surelogic.common.xml.Entity;
import com.surelogic.common.xml.SourceRef;
import com.surelogic.dropsea.DiffInfoUtility;
import com.surelogic.dropsea.IDiffInfo;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IKeyValue;

public class IRFreeDrop implements IDrop {

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
  private IJavaRef f_javaRef = null;
  @NonNull
  private final String f_message;
  @Nullable
  private final String f_messageCanonical;
  /**
   * This collection is {@code null} until some exist&mdash;most drops have no
   * hints.
   */
  @Nullable
  private List<IRFreeHintDrop> f_analysisHints = null;
  @NonNull
  private final List<IKeyValue> f_diffInfos;

  void addProposal(IRFreeProposedPromiseDrop info) {
    if (f_proposedPromises == null) {
      f_proposedPromises = new ArrayList<IRFreeProposedPromiseDrop>(1);
    }
    f_proposedPromises.add(info);
  }

  boolean hasJavaRef() {
    return f_javaRef != null;
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

    f_messageCanonical = e.getAttribute(MESSAGE_ID);

    String diffInfoString = e.getAttribute(DIFF_INFO);
    if (diffInfoString != null) {
      f_diffInfos = DiffInfoUtility.parseListEncodedForPersistence(diffInfoString);
    } else {
      f_diffInfos = new ArrayList<IKeyValue>();

      /*
       * Attempt to read old tree/context hash if they exist in the file
       */
      String treeHashString = e.getAttribute("fAST-tree-hash");
      if (treeHashString == null)
        treeHashString = e.getAttribute("hash"); // old name
      if (treeHashString != null) {
        try {
          final long treeHashValue = Long.parseLong(treeHashString);
          f_diffInfos.add(DiffInfoUtility.getLongInstance(IDiffInfo.FAST_TREE_HASH, treeHashValue));
        } catch (NumberFormatException nfe) {
          SLLogger.getLogger().log(Level.WARNING, I18N.err(259, treeHashString, "fAST-tree-hash"), nfe);
        }
      }
      String contextHashString = e.getAttribute("fAST-context-hash");
      if (contextHashString == null)
        contextHashString = e.getAttribute("context"); // old name
      if (contextHashString != null) {
        try {
          final long contextHashValue = Long.parseLong(contextHashString);
          f_diffInfos.add(DiffInfoUtility.getLongInstance(IDiffInfo.FAST_CONTEXT_HASH, contextHashValue));
        } catch (NumberFormatException nfe) {
          SLLogger.getLogger().log(Level.WARNING, I18N.err(259, contextHashString, "fAST-context-hash"), nfe);
        }
      }
    }

    final String encodedJavaRef = e.getAttribute(JAVA_REF);
    if (encodedJavaRef != null) {
      try {
        final IJavaRef ref = e.parsePersistedRef(encodedJavaRef);
        f_javaRef = ref;
      } catch (Exception parseFailure) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(288, encodedJavaRef), parseFailure);
      }
    }
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

  public boolean containsDiffInfoKey(String key) {
    for (IKeyValue di : f_diffInfos)
      if (di.getKey().equals(key))
        return true;
    return false;
  }

  public String getDiffInfoOrNull(String key) {
    for (IKeyValue di : f_diffInfos)
      if (di.getKey().equals(key))
        return di.getValueAsString();
    return null;
  }

  public long getDiffInfoAsLong(String key, long valueIfNotRepresentable) {
    for (IKeyValue di : f_diffInfos)
      if (di.getKey().equals(key))
        return di.getValueAsLong(valueIfNotRepresentable);
    return valueIfNotRepresentable;
  }

  public int getDiffInfoAsInt(String key, int valueIfNotRepresentable) {
    for (IKeyValue di : f_diffInfos)
      if (di.getKey().equals(key))
        return di.getValueAsInt(valueIfNotRepresentable);
    return valueIfNotRepresentable;
  }

  static int convert(String val) {
    if (val == null) {
      return 0;
    } else {
      return Integer.valueOf(val);
    }
  }

  // TODO REMOVE WHEN NO LONGER NEEDED -- THIS IS FOR REGRESSION TESTS
  public String javaId;

  static void makeJavaRefFromSrcRefAndAddTo(IRFreeDrop drop, SourceRef ref) {
    if (ref == null || drop == null)
      return;
    if (drop.hasJavaRef())
      return;

    Pair<IJavaRef, String> pair = makeJavaRefAndJavaIdFromSrcRef(ref);
    if (pair != null) {
      drop.setJavaRef(pair.first());
      drop.javaId = pair.second();
    }
  }

  static IJavaRef makeJavaRefFromSrcRef(SourceRef ref) {
    Pair<IJavaRef, String> pair = makeJavaRefAndJavaIdFromSrcRef(ref);
    if (pair != null)
      return pair.first();
    else
      return null;
  }

  static Pair<IJavaRef, String> makeJavaRefAndJavaIdFromSrcRef(SourceRef ref) {
    if (ref == null) {
      return null;
    }
    final int line = Integer.valueOf(ref.getLine());
    final String pkg = ref.getAttribute(PKG_ATTR);
    final String cuName = ref.getAttribute(CUNIT_ATTR);
    final String javaId = ref.getAttribute(JAVA_ID_ATTR);
    // final String enclosingId = ref.getAttribute(WITHIN_DECL_ATTR);
    final String project = ref.getAttribute(PROJECT_ATTR);

    final int offset = convert(ref.getAttribute(OFFSET_ATTR));
    final int length = convert(ref.getAttribute(LENGTH_ATTR));

    if (cuName.contains("[]")) {
      // this is a SrcRef to [].length which is bogas -- quietly ignore this one
      return null;
    } else {
      boolean classExt = cuName.endsWith(".class");
      String classNm = classExt ? cuName.substring(0, cuName.length() - 6) : cuName;
      classNm = classNm.replaceAll("\\$", ".");
      // Note that the default package is "" in the SourceRef instances
      // Note that "package-info" needs to be delt with
      final String jarStyleName;
      if (SLUtility.PACKAGE_INFO.equals(classNm))
        jarStyleName = pkg + "/";
      else
        jarStyleName = pkg + "/" + classNm;
      final IDecl decl = Decl.getDeclForTypeNameFullyQualifiedSureLogic(jarStyleName);
      final JavaRef.Builder builder = new JavaRef.Builder(decl);
      builder.setEclipseProjectName(project);
      if (classExt)
        builder.setWithin(IJavaRef.Within.JAR_FILE);
      if (line < Integer.MAX_VALUE)
        builder.setLineNumber(line);
      if (offset < Integer.MAX_VALUE)
        builder.setOffset(offset);
      if (length < Integer.MAX_VALUE)
        builder.setLength(length);
      return new Pair<IJavaRef, String>(builder.build(), javaId);
    }
  }
}
