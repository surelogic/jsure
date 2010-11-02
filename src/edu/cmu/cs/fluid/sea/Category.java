package edu.cmu.cs.fluid.sea;

import java.util.HashMap;
import java.util.Map;

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

  static final Map<String,Category> keyToCategory = new HashMap<String,Category>(); // String.intern() -> Category

  private String key;

  private String message = null;

  private int count = 0;

  private CategoryFormatter formatter = new CategoryFormatter() {

    public String format(final Category link) {
      return link.getCount() + " " + link.getMessage();
    }
  };

  private Category(String key) {
    this.key = key;
  }

  /**
   * Returns an instance of a Category that is formatted with the count
   * at the front of the category description.  For example if
   * <code>count</code> = 4 and the category key is " aliased result(s) found" 
   * <pre>
   * 4 aliased result(s) found
   * </pre>
   * 
   * @param key the unique identifier for the category, multiple calls with
   *   the same <code>key</code> return the same instance
   * @return a category
   */
  public static final Category getInstance(String key) {
	if (key == null) {
		return null;
	}
    if (keyToCategory.containsKey(key)) {
      return keyToCategory.get(key);
    } else {
      String catKey = key.intern();
      Category newCat = new Category(catKey);
      keyToCategory.put(catKey, newCat);
      return newCat;
    }
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
  public static final Category getResultInstance(String key) {
    final Category result = getInstance(key);
    result.formatter = new CategoryFormatter() {

      public String format(final Category link) {
        return link.getMessage() + " (" + link.getCount()
            + (link.getCount() > 1 ? " issues)" : " issue)");
      }
    };
    return result;
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