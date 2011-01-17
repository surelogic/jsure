package edu.cmu.cs.fluid.sea;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

import com.surelogic.common.i18n.I18N;

/**
 * Defines a category which may be attached to
 * {@link edu.cmu.cs.fluid.sea.InfoDrop} and
 * {@link edu.cmu.cs.fluid.sea.ResultDrop}.  Categories are intended to
 * help the user interface report results.
 * <p>
 * A noteworthy limitation of categories is that the user interface must set the
 * count with {@link #setCount(int)} before getting a formatted message
 * (containing the count) with {@link #getFormattedMessage()}. 
 */
public final class Category {
  public interface CategoryFormatter {
    public String label();
    public String format(Category link);
  }
  
  public static final class PrefixFormatter implements CategoryFormatter {
    public static final PrefixFormatter INSTANCE = new PrefixFormatter();
    
    private PrefixFormatter() {
      super();
    }
    
    public String label() {
      return "prefix";
    }
    
    public String format(final Category link) {
      return link.getCount() + " " + link.getMessage();
    }
  }
  
  public static final class PostfixFormatter implements CategoryFormatter {
    public static final PostfixFormatter INSTANCE = new PostfixFormatter();
    
    private PostfixFormatter() {
      super();
    }
    
    public String label() {
      return "postfix";
    }
    
    public String format(final Category link) {
      return link.getMessage() + " (" + link.getCount()
          + (link.getCount() > 1 ? " issues)" : " issue)");
    }
  }
  
  

  private static final CategoryFormatter[] FORMATTERS = { 
    PrefixFormatter.INSTANCE, PostfixFormatter.INSTANCE
  };

  private static final Map<String,Category> keyToCategory =
    new HashMap<String, Category>(); // String.intern() -> Category
  
  
  
  private String key;
  private String message = null;
  private int count = 0;
  private final CategoryFormatter formatter;

  
  
  private Category(final String key, final CategoryFormatter formatter) {
    this.key = key;
    this.formatter = formatter;
  }

  
  
  /**
   * Returns an instance of a Category that is formatted with the count
   * at the front of the category description.  For example if
   * <code>count</code> = 4 and the category key is "aliased result(s) found" 
   * <pre>
   * 4 aliased result(s) found
   * </pre>
   * 
   * @param key the unique identifier for the category, multiple calls with
   *   the same <code>key</code> return the same instance
   * @return a category
   */
  @Deprecated
  public static Category getInstance(final String key) {
    return getInstance(key, PrefixFormatter.INSTANCE);
  }



  public static Category getInstance(
      final String key, final CategoryFormatter formatter) {
    if (key == null) {
      return null;
    }
    if (keyToCategory.containsKey(key)) {
      return keyToCategory.get(key);
    } else {
      String catKey = key.intern();
      Category newCat = new Category(catKey, formatter);
      keyToCategory.put(catKey, newCat);
      return newCat;
    }
  }
  
  public static Category getInstance2(final int num) {
    for (final CategoryFormatter formatter : FORMATTERS) {
      try {
        final String key = I18N.category(num, formatter.label());
        if (key == null) {
          return null;
        }
        if (keyToCategory.containsKey(key)) {
          return keyToCategory.get(key);
        } else {
          String catKey = key.intern();
          Category newCat = new Category(catKey, formatter);
          keyToCategory.put(catKey, newCat);
          return newCat;
        }
      } catch (final MissingResourceException e) {
        /* Catch to keep the exception from blowing things up.  We just want
         * to go to the next formatter, or exit the loop, if the call to
         * I18N.category() fails.
         */
      }
    }
    // If we get here, there is no category with the given id defined
    throw new MissingResourceException(
        "No category defined with id " + num, null, null);
  }

  
  
  /**
   * Returns an instance of a Category that is formatted with the count
   * at the end of the category description.  For example if
   * <code>count</code> = 7 and the category key is "Lock policy results" 
   * <pre>
   * Lock policy results (7 items)
   * </pre>
   * 
   * @param key the unique identifier for the category, multiple calls with
   *   the same <code>key</code> return the same instance
   * @return a category
   */
  @Deprecated
  public static Category getResultInstance(String key) {
    return getInstance(key, PostfixFormatter.INSTANCE);
  }
  
  /**
   * @return Returns the message.
   */
  public final String getMessage() {
    return (message == null ? key : message);
  }

  /**
   * @return Returns the formatted message.
   */
  public final String getFormattedMessage() {
    return formatter.format(this);
  }

  /**
   * @param message The message to set.
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
   * @param count the count of items to show in the formatted message
   * 
   * @see #getFormattedMessage()
   */
  public final void setCount(int count) {
    this.count = count;
  }
  
  public final String getKey() {
    return key;
  }
}