package edu.cmu.cs.fluid.promise.parse;

import java.util.logging.Level;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.promise.*;

/**
 * Handles the following standard tags: author, deprecated, return, see
 * exception, param, serial, serialField, serialData, throws, version
 * 
 * Also the following proposed tags: category, example, tutorial, index,
 * exclude, todo, internal, obsolete
 * 
 * Does not check to see if tags are valid
 * 
 * Not inline tags: docRoot, inheritDoc, value link, linkplain
 */
public class StandardTagRule extends AbstractParseRule {
  StandardTagRule(String s) {
    super(s, anyOp);
  }

  private static final String[] stdTags =
    {
      "author",
      "deprecated",
      "exception",
      // Handled by a SimpleInlineTagsRule
      // "param",
      // "return",
      "see",
      "serial",
      "serialField",
      "serialData",
      "since",
      "throws",
      "version",
      "category",
      "example",
      "tutorial",
      "index",
      "exclude",
      "todo",
      "TODO",
      "internal",
      "obsolete" };
  private static boolean setupDone = false;

  @Override
  protected String ensureCapitalizedTag(String tag) {
    return tag;
  }
  
  /**
	 *  
	 */
  public static synchronized void setupTagRules(IPromiseParser parser) {
    if (setupDone) {
      return;
    }

    for (int i = 0; i < stdTags.length; i++) {
      parser.addRule(new StandardTagRule(stdTags[i]));
    }
    setupDone = true;
  }

  /**
	 * @see IPromiseParseRule
	 */
  public boolean parse(IRNode n, String contents, IPromiseParsedCallback cb) {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Standard @" + name + " ignoring '" + contents + "'");
    }
    cb.parsed();
    return true;
  }
}
