package com.surelogic.dropsea;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.surelogic.Immutable;
import com.surelogic.NonNull;
import com.surelogic.Utility;
import com.surelogic.ValueObject;
import com.surelogic.common.i18n.I18N;

/**
 * A utility to construct, encode, and decode {@link IKeyValue} instances.
 */
@Utility
public final class KeyValueUtility {

  /**
   * Constructs a string-valued {@link IKeyValue} instance.
   * 
   * @param key
   *          the key for this instance.
   * @param value
   *          a value.
   * @return a string-valued {@link IKeyValue} instance.
   * 
   * @throws IllegalArgumentException
   *           if something goes wrong.
   */
  public static IKeyValue getStringInstance(String key, String value) {
    return new StringDiffInfo(key, value);
  }

  /**
   * Constructs a int-valued {@link IKeyValue} instance.
   * 
   * @param key
   *          the key for this instance.
   * @param value
   *          a value.
   * @return a int-valued {@link IKeyValue} instance.
   * 
   * @throws IllegalArgumentException
   *           if something goes wrong.
   */
  public static IKeyValue getIntInstance(String key, int value) {
    return new IntDiffInfo(key, value);
  }

  /**
   * Constructs a long-valued {@link IKeyValue} instance.
   * 
   * @param key
   *          the key for this instance.
   * @param value
   *          a value.
   * @return a long-valued {@link IKeyValue} instance.
   * 
   * @throws IllegalArgumentException
   *           if something goes wrong.
   */
  public static IKeyValue getLongInstance(String key, long value) {
    return new LongDiffInfo(key, value);
  }

  /**
   * Constructs an enum-valued {@link IKeyValue} instance.
   * 
   * @param key
   *          the key for this instance.
   * @param value
   *          an value.
   * @return a enum-valued {@link IKeyValue} instance.
   * 
   * @throws IllegalArgumentException
   *           if something goes wrong.
   */
  public static <T extends Enum<T>> IKeyValue getEnumInstance(String key, T value) {
    return new StringDiffInfo(key, value.name());
  }

  /**
   * Parses the result of {@link IKeyValue#encodeForPersistence()} back to a
   * {@link IKeyValue}.
   * 
   * @param value
   *          a string.
   * @return a diff-info value.
   * 
   * @throws IllegalArgumentException
   *           if something goes wrong.
   */
  @NonNull
  public static IKeyValue parseEncodedForPersistence(final String value) {
    if (value == null)
      throw new IllegalArgumentException(I18N.err(44, "value"));
    String v = value;
    boolean isInt = v.startsWith("I");
    boolean isLong = v.startsWith("L");
    v = v.substring(1); // remove type code
    final int sepIndex = v.indexOf(",");
    if (sepIndex == -1)
      throw new IllegalArgumentException("Not an encoded IDiffInfo: " + value);
    final String key = v.substring(0, sepIndex);
    final String diffInfoValue = v.substring(sepIndex + 1);
    if (isInt)
      return getIntInstance(key, Integer.parseInt(diffInfoValue));
    else if (isLong)
      return getLongInstance(key, Long.parseLong(diffInfoValue));
    else
      return getStringInstance(key, diffInfoValue);
  }

  /**
   * Encodes a list of diff-info values for persistence as a string. Use
   * {@link #parseListEncodedForPersistence(String)} to return the string to a
   * list of {@link IDiffInfo}.
   * 
   * @param diffInfos
   *          a list of diff-info values.
   * @return a string.
   * 
   * @throws IllegalArgumentException
   *           if something goes wrong.
   */
  @NonNull
  public static String encodeListForPersistence(List<IKeyValue> diffInfos) {
    if (diffInfos == null)
      throw new IllegalArgumentException(I18N.err(44, "diffInfos"));
    final StringBuilder b = new StringBuilder();
    if (diffInfos.isEmpty())
      b.append("n/a");
    else
      for (IKeyValue di : diffInfos) {
        String edi = di.encodeForPersistence();
        b.append(Integer.toString(edi.length()));
        b.append(':');
        b.append(edi);
      }
    return b.toString();
  }

  /**
   * Parses the result of {@link #encodeListForPersistence(List)} back to a list
   * of {@link IDiffInfo}.
   * 
   * @param value
   *          a string.
   * @return a possibly empty list of type references.
   * 
   * @throws IllegalArgumentException
   *           if something goes wrong.
   */
  @NonNull
  public static List<IKeyValue> parseListEncodedForPersistence(final String value) {
    if (value == null)
      throw new IllegalArgumentException(I18N.err(44, "value"));
    final List<IKeyValue> result = new ArrayList<IKeyValue>();
    if ("n/a".equals(value))
      return result;
    final StringBuilder b = new StringBuilder(value);
    while (true) {
      final int lengthSepIndex = b.indexOf(":");
      if (lengthSepIndex == -1)
        break;
      final int itemLength = Integer.parseInt(b.substring(0, lengthSepIndex));
      b.delete(0, lengthSepIndex + 1);
      final String encoded = b.substring(0, itemLength);
      if (encoded.length() < 1)
        break;
      result.add(parseEncodedForPersistence(encoded));
      b.delete(0, itemLength);
    }
    return result;
  }

  @Immutable
  @ValueObject
  static abstract class AbstractKeyValue implements IKeyValue {
    @NonNull
    final String f_key;

    AbstractKeyValue(String key) {
      if (key == null)
        throw new IllegalArgumentException(I18N.err(44, "key"));
      if (key.indexOf(',') != -1)
        throw new IllegalArgumentException("key cannot contain a comma: " + key);
      f_key = key;
    }

    @NonNull
    public final String getKey() {
      return f_key;
    }

    @NonNull
    public final String encodeForPersistence() {
      final StringBuilder b = new StringBuilder();
      // first character of the class name is used as a type indicator
      b.append(this.getClass().getSimpleName().substring(0, 1));
      b.append(getKey()).append(',').append(getValueAsString());
      return b.toString();
    }

    @Override
    public final String toString() {
      return this.getClass().getSimpleName() + "(" + getKey() + "->" + getValueAsString() + ")";
    }
  }

  @Immutable
  @ValueObject
  static final class StringDiffInfo extends AbstractKeyValue {
    @NonNull
    final String f_value;

    protected StringDiffInfo(String key, String value) {
      super(key);
      if (value == null)
        throw new IllegalArgumentException(I18N.err(44, "value"));
      f_value = value;
    }

    @NonNull
    public String getValueAsString() {
      return f_value;
    }

    public long getValueAsLong(long valueIfNotRepresentable) {
      return valueIfNotRepresentable;
    }

    public int getValueAsInt(int valueIfNotRepresentable) {
      return valueIfNotRepresentable;
    }

    public <T extends Enum<T>> T getValueAsEnum(T valueIfNotRepresentable, Class<T> elementType) {
      for (T element : EnumSet.allOf(elementType)) {
        if (element.toString().equals(f_value))
          return element;
      }
      return valueIfNotRepresentable;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((f_key == null) ? 0 : f_key.hashCode());
      result = prime * result + ((f_value == null) ? 0 : f_value.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof StringDiffInfo))
        return false;
      StringDiffInfo other = (StringDiffInfo) obj;
      if (f_key == null) {
        if (other.f_key != null)
          return false;
      } else if (!f_key.equals(other.f_key))
        return false;
      if (f_value == null) {
        if (other.f_value != null)
          return false;
      } else if (!f_value.equals(other.f_value))
        return false;
      return true;
    }
  }

  @Immutable
  @ValueObject
  static final class IntDiffInfo extends AbstractKeyValue {
    @NonNull
    final int f_value;

    protected IntDiffInfo(String key, int value) {
      super(key);
      f_value = value;
    }

    @NonNull
    public String getValueAsString() {
      return Integer.toString(f_value);
    }

    public long getValueAsLong(long valueIfNotRepresentable) {
      return (long) f_value;
    }

    public int getValueAsInt(int valueIfNotRepresentable) {
      return f_value;
    }

    public <T extends Enum<T>> T getValueAsEnum(T valueIfNotRepresentable, Class<T> elementType) {
      return valueIfNotRepresentable;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((f_key == null) ? 0 : f_key.hashCode());
      result = prime * result + f_value;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof IntDiffInfo))
        return false;
      IntDiffInfo other = (IntDiffInfo) obj;
      if (f_key == null) {
        if (other.f_key != null)
          return false;
      } else if (!f_key.equals(other.f_key))
        return false;
      if (f_value != other.f_value)
        return false;
      return true;
    }
  }

  @Immutable
  @ValueObject
  static final class LongDiffInfo extends AbstractKeyValue {
    @NonNull
    final long f_value;

    protected LongDiffInfo(String key, long value) {
      super(key);
      f_value = value;
    }

    @NonNull
    public String getValueAsString() {
      return Long.toString(f_value);
    }

    public long getValueAsLong(long valueIfNotRepresentable) {
      return f_value;
    }

    public int getValueAsInt(int valueIfNotRepresentable) {
      if (f_value < Integer.MIN_VALUE || f_value > Integer.MAX_VALUE)
        return valueIfNotRepresentable;
      else
        return (int) f_value;
    }

    public <T extends Enum<T>> T getValueAsEnum(T valueIfNotRepresentable, Class<T> elementType) {
      return valueIfNotRepresentable;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((f_key == null) ? 0 : f_key.hashCode());
      result = prime * result + (int) (f_value ^ (f_value >>> 32));
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof LongDiffInfo))
        return false;
      LongDiffInfo other = (LongDiffInfo) obj;
      if (f_key == null) {
        if (other.f_key != null)
          return false;
      } else if (!f_key.equals(other.f_key))
        return false;
      if (f_value != other.f_value)
        return false;
      return true;
    }
  }

  private KeyValueUtility() {
    // no instances
  }
}
