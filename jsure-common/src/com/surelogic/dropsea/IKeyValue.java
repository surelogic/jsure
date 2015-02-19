package com.surelogic.dropsea;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;

public interface IKeyValue {

  @NonNull
  String getKey();

  @NonNull
  String getValueAsString();

  long getValueAsLong(long valueIfNotRepresentable);

  int getValueAsInt(int valueIfNotRepresentable);

  @NonNull
  <T extends Enum<T>> T getValueAsEnum(T valueIfNotRepresentable, Class<T> elementType);

  @NonNull
  IJavaRef getValueAsJavaRefOrThrow();

  @Nullable
  IJavaRef getValueAsJavaRefOrNull();

  @NonNull
  IDecl getValueAsDeclOrThrow();

  @Nullable
  IDecl getValueAsDeclOrNull();

  @NonNull
  String encodeForPersistence();
}
