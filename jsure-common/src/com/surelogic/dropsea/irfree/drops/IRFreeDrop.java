package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IKeyValue;
import com.surelogic.dropsea.KeyValueUtility;
import com.surelogic.dropsea.irfree.DiffHeuristics;

public abstract class IRFreeDrop implements IDrop {

  @NonNull
  private final String f_simpleName;

  @NonNull
  private final String f_fullName;

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
  private final boolean f_isFromSrc;

  /**
   * This collection is {@code null} until some exist&mdash;most drops have no
   * hints.
   */
  @Nullable
  private List<IRFreeHintDrop> f_analysisHints = null;
  @NonNull
  private final List<IKeyValue> f_diffInfos;

  boolean aliasTheMessage() {
    return false;
  }

  public boolean includeInDiff() {
	return true;
  }
  
  void addProposal(IRFreeProposedPromiseDrop info) {
    if (f_proposedPromises == null) {
      f_proposedPromises = new ArrayList<>(1);
    }
    f_proposedPromises.add(info);
  }

  void addHint(IRFreeHintDrop hint) {
    if (f_analysisHints == null) {
      f_analysisHints = new ArrayList<>(1);
    }
    f_analysisHints.add(hint);
  }

  IRFreeDrop(Entity e) {
    if (e == null)
      throw new IllegalArgumentException(I18N.err(44, "e"));

    final String type = e.getAttributeByAliasIfPossible(TYPE_ATTR);
    if (type == null)
      throw new IllegalArgumentException(I18N.err(44, "simpleName"));
    f_simpleName = type;

    final String fullType = e.getAttributeByAliasIfPossible(FULL_TYPE_ATTR);
    if (fullType == null)
      throw new IllegalArgumentException(I18N.err(44, "fullName"));
    f_fullName = fullType;

    f_categorizingMessage = e.getAttributeByAliasIfPossible(CATEGORY_ATTR);

    final boolean aliasMsg = aliasTheMessage();
    final String message = aliasMsg ? e.getAttributeByAliasIfPossible(MESSAGE_ATTR) : e.getAttribute(MESSAGE_ATTR);
    if (message != null)
      f_message = message;
    else
      f_message = "(EMPTY)";

    f_messageCanonical = aliasMsg ? e.getAttributeByAliasIfPossible(MESSAGE_ID) : e.getAttribute(MESSAGE_ID);

    String diffInfoString = e.getAttribute(DIFF_INFO);
    if (diffInfoString != null) {
      f_diffInfos = KeyValueUtility.parseListEncodedForPersistence(diffInfoString);
    } else {
      f_diffInfos = new ArrayList<>();

      /*
       * Attempt to read old tree/context hash if they exist in the file
       */
      String treeHashString = e.getAttribute("fAST-tree-hash");
      if (treeHashString == null)
        treeHashString = e.getAttribute("hash"); // old name
      if (treeHashString != null) {
        try {
          final long treeHashValue = Long.parseLong(treeHashString);
          f_diffInfos.add(KeyValueUtility.getLongInstance(DiffHeuristics.FAST_TREE_HASH, treeHashValue));
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
          f_diffInfos.add(KeyValueUtility.getLongInstance(DiffHeuristics.FAST_CONTEXT_HASH, contextHashValue));
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
    f_isFromSrc = "true".equals(e.getAttribute(FROM_SRC));
  }

  @Override
  @Nullable
  public String getCategorizingMessage() {
    return f_categorizingMessage;
  }

  @Override
  @NonNull
  public String getMessage() {
    return f_message;
  }

  @Override
  @NonNull
  public String getMessageCanonical() {
    return f_messageCanonical;
  }

  @Override
  @Nullable
  public IJavaRef getJavaRef() {
    return f_javaRef;
  }

  @Override
  public final boolean isFromSrc() {
    return f_isFromSrc;
  }

  // @Override
  // @NonNull
  // public Class<?> getIRDropSeaClass() {
  // return f_irDropSeaClass;
  // }
  //
  // @Override
  // public final boolean instanceOfIRDropSea(Class<?> type) {
  // if (type == null)
  // return false;
  //
  // return type.isAssignableFrom(f_irDropSeaClass);
  // }

  @NonNull
  public String getSimpleClassName() {
    return f_simpleName;
  }

  @NonNull
  public String getFullClassName() {
    return f_fullName;
  }

  @Override
  @NonNull
  public Collection<IRFreeProposedPromiseDrop> getProposals() {
    if (f_proposedPromises != null) {
      /*
       * We need to filter the results that cannot currently be edited into the
       * code. This is proposals about binary code that do not have an
       * assumption target. this should be changed back to
       * 
       * return f_proposedPromises;
       * 
       * when automatic editing of XML is complete.
       */
      Collection<IRFreeProposedPromiseDrop> result = new ArrayList<>();
      for (IRFreeProposedPromiseDrop ppd : f_proposedPromises) {
        if (ppd.isFromSrc() || ppd.getAssumptionRef().isFromSource())
          result.add(ppd);
      }
      return result;
    } else
      return Collections.emptyList();
  }

  @Override
  @NonNull
  public final Collection<IRFreeHintDrop> getHints() {
    if (f_analysisHints != null)
      return f_analysisHints;
    else
      return Collections.emptyList();
  }

  @Override
  public boolean containsDiffInfoKey(String key) {
    for (IKeyValue di : f_diffInfos)
      if (di.getKey().equals(key))
        return true;
    return false;
  }

  @Override
  public String getDiffInfoOrNull(String key) {
    for (IKeyValue di : f_diffInfos)
      if (di.getKey().equals(key))
        return di.getValueAsString();
    return null;
  }

  @Override
  public long getDiffInfoAsLong(String key, long valueIfNotRepresentable) {
    for (IKeyValue di : f_diffInfos)
      if (di.getKey().equals(key))
        return di.getValueAsLong(valueIfNotRepresentable);
    return valueIfNotRepresentable;
  }

  @Override
  public int getDiffInfoAsInt(String key, int valueIfNotRepresentable) {
    for (IKeyValue di : f_diffInfos)
      if (di.getKey().equals(key))
        return di.getValueAsInt(valueIfNotRepresentable);
    return valueIfNotRepresentable;
  }

  @Override
  public <T extends Enum<T>> T getDiffInfoAsEnum(String key, T valueIfNotRepresentable, Class<T> elementType) {
    for (IKeyValue di : f_diffInfos)
      if (di.getKey().equals(key))
        return di.getValueAsEnum(valueIfNotRepresentable, elementType);
    return valueIfNotRepresentable;
  }

  @Override
  public IJavaRef getDiffInfoAsJavaRefOrThrow(String key) {
    for (IKeyValue di : f_diffInfos)
      if (di.getKey().equals(key))
        return di.getValueAsJavaRefOrThrow();
    throw new IllegalArgumentException("no value for " + key);
  }

  @Override
  public IJavaRef getDiffInfoAsJavaRefOrNull(String key) {
    for (IKeyValue di : f_diffInfos)
      if (di.getKey().equals(key))
        return di.getValueAsJavaRefOrNull();
    return null;
  }

  @Override
  public IDecl getDiffInfoAsDeclOrThrow(String key) {
    for (IKeyValue di : f_diffInfos)
      if (di.getKey().equals(key))
        return di.getValueAsDeclOrThrow();
    throw new IllegalArgumentException("no value for " + key);
  }

  @Override
  public IDecl getDiffInfoAsDeclOrNull(String key) {
    for (IKeyValue di : f_diffInfos)
      if (di.getKey().equals(key))
        return di.getValueAsDeclOrNull();
    return null;
  }

  static int convert(String val) {
    if (val == null) {
      return 0;
    } else {
      return Integer.valueOf(val);
    }
  }
}
