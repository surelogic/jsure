package com.surelogic.dropsea;

import com.surelogic.NonNull;

public interface IKeyValue {

  @NonNull
  String getKey();

  @NonNull
  String getValueAsString();

  long getValueAsLong(long valueIfNotRepresentable);

  int getValueAsInt(int valueIfNotRepresentable);

  <T extends Enum<T>> T getValueAsEnum(T valueIfNotRepresentable, Class<T> elementType);

  @NonNull
  String encodeForPersistence();
}
