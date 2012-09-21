package com.surelogic.dropsea.ir;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

import com.surelogic.NonNull;
import com.surelogic.Singleton;
import com.surelogic.common.i18n.I18N;

/**
 * Defines a category which may be attached to any drop. Categories are intended
 * to help the user interface report results.
 * <p>
 * A noteworthy limitation of categories is that the user interface must set the
 * count with {@link #setCount(int)} before getting a formatted message
 * (containing the count) with {@link #getFormattedMessage()}.
 */
public final class Category {

  public interface CategoryFormatter {
    /**
     * Gets the formatter name to place between <tt>category.</tt> and a number
     * for lookups in the <tt>SureLogicResults.properties</tt> file. For
     * example, if the label is <tt>postfix</tt> and the number is 100 the key
     * generated will be <tt>category.postfix.00100</tt>.
     * 
     * @return a formatter name.
     */
    public String formatterName();

    public String format(Category link);
  }

  @Singleton
  public static final class PrefixFormatter implements CategoryFormatter {

    public static final PrefixFormatter INSTANCE = new PrefixFormatter();

    private PrefixFormatter() {
      // singleton
    }

    public String formatterName() {
      return "prefix";
    }

    public String format(final Category link) {
      return link.getCount() + " " + link.getMessage();
    }
  }

  @Singleton
  public static final class PostfixFormatter implements CategoryFormatter {

    public static final PostfixFormatter INSTANCE = new PostfixFormatter();

    private PostfixFormatter() {
      // singletion
    }

    public String formatterName() {
      return "postfix";
    }

    public String format(final Category link) {
      return link.getMessage() + " (" + link.getCount() + (link.getCount() > 1 ? " issues)" : " issue)");
    }
  }

  private static final CategoryFormatter[] FORMATTERS = { PrefixFormatter.INSTANCE, PostfixFormatter.INSTANCE };

  private static final Map<String, Category> keyToCategory = new HashMap<String, Category>();

  /**
   * Returns a category that is formatted with the count at the front of the
   * category description. For example if <code>count</code> = 4 and the
   * category key is "aliased result(s) found"
   * 
   * <pre>
   * 4 aliased result(s) found
   * </pre>
   * 
   * @param key
   *          the unique identifier for the category, multiple calls with the
   *          same <code>key</code> return the same instance.
   * @return a category.
   * @throws IllegalArgumentException
   *           if key is null.
   */
  public static Category getPrefixCountInstance(final String key) {
    return getInstance(key, PrefixFormatter.INSTANCE);
  }

  private static Category getInstance(final String key, final CategoryFormatter formatter) {
    if (key == null)
      throw new IllegalArgumentException(I18N.err(44, "key"));

    if (keyToCategory.containsKey(key)) {
      return keyToCategory.get(key);
    } else {
      String catKey = key;
      Category newCat = new Category(catKey, formatter);
      keyToCategory.put(catKey, newCat);
      return newCat;
    }
  }

  /**
   * Returns an instance of a Category identified by the given id in the
   * <tt>SureLogicResults.properties</tt> file. The formatting of the category
   * is determined by the key in the property file, which has the format
   * <tt>category.<i>formatter</i>.nnnnn</tt>. Currently the formatters
   * <tt>prefix</tt> and <tt>postfix</tt> are recognized. It is an error for a
   * given identifier to appear in more than one key, for example
   * 
   * <pre>
   * category.prefix.00010 = &hellip;
   * category.postfix.00010 = &hellip;
   * </pre>
   * 
   * Behavior in this case is undefined.
   * 
   * @param num
   *          The category id to look up.
   * @return a category
   * @exception MissingResourceException
   *              Thrown if no category key using the given id is found in the
   *              SureLoficResults property file.
   */
  public static Category getInstance(final int num) {
    for (final CategoryFormatter formatter : FORMATTERS) {
      try {
        final String key = I18N.category(formatter.formatterName(), num);
        if (key == null) {
          return null;
        }
        if (keyToCategory.containsKey(key)) {
          return keyToCategory.get(key);
        } else {
          String catKey = key;
          Category newCat = new Category(catKey, formatter);
          keyToCategory.put(catKey, newCat);
          return newCat;
        }
      } catch (final MissingResourceException e) {
        /*
         * Catch to keep the exception from blowing things up. We just want to
         * go to the next formatter, or exit the loop, if the call to
         * I18N.category() fails.
         */
      }
    }
    // If we get here, there is no category with the given id defined
    throw new MissingResourceException("No category defined with id " + num, null, null);
  }

  /**
   * Returns an instance of a Category that is formatted with the count at the
   * end of the category description. For example if <code>count</code> = 7 and
   * the category key is "Lock policy results"
   * 
   * <pre>
   * Lock policy results (7 items)
   * </pre>
   * 
   * @param key
   *          the unique identifier for the category, multiple calls with the
   *          same <code>key</code> return the same instance
   * @return a category
   */
  public static Category getResultInstance(String key) {
    return getInstance(key, PostfixFormatter.INSTANCE);
  }

  @NonNull
  private final String f_key;
  @NonNull
  private final CategoryFormatter f_formatter;
  private String message = null;
  private int count = 0;

  private Category(final String key, final CategoryFormatter formatter) {
    f_key = key;
    f_formatter = formatter;
  }

  /**
   * @return Returns the message.
   */
  public final String getMessage() {
    return (message == null ? f_key : message);
  }

  /**
   * @return Returns the formatted message.
   */
  public final String getFormattedMessage() {
    return f_formatter.format(this);
  }

  /**
   * @param message
   *          The message to set.
   */
  public final void setMessage(String message) {
    this.message = message;
  }

  /**
   * @return Returns the count.
   */
  public final int getCount() {
    return count;
  }

  /**
   * @param count
   *          the count of items to show in the formatted message
   * 
   * @see #getFormattedMessage()
   */
//  public final void setCount(int count) {
//    this.count = count;
//  }

  public final String getKey() {
    return f_key;
  }
}