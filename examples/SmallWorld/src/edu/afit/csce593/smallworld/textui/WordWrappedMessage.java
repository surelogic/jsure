package edu.afit.csce593.smallworld.textui;

/**
 * A class that produces word wrapped string messages. The message starts empty
 * and has sentences appended to it. These sentences are word wrapped to a
 * desired text width. The default text width is 50.
 * 
 * @author T.J. Halloran
 * @author Robert Graham
 */
public final class WordWrappedMessage {

	/**
	 * A String holding the operating system specific line separator obtained
	 * from the system properties.
	 */
	public static String LINESEP = System.getProperty("line.separator");

	/**
	 * A buffer to hold the message.
	 */
	private final StringBuilder f_buffer = new StringBuilder();

	/**
	 * The desired text width of lines. Specifies the column at which we word
	 * wrap the text appended into this message.
	 */
	private int f_desiredTextWidth = 50;

	/**
	 * The text width of the current line we are word wrapping (at the very end
	 * of the buffer).
	 */
	private int f_lineTextWidth = 0;

	/**
	 * Appends the given string to this message. The string is word wrapped.
	 * 
	 * @param s
	 *            the string to append to this message.
	 */
	public void append(String s) {
		s = s.trim();
		boolean done = false;
		while (!done) {
			int endOfWord = nextWord(s);
			if (endOfWord == -1) {
				/*
				 * Add the last bit of the string with a trailing space to
				 * ensure that the next sentence appended to this word wrapped
				 * message does not bump up against the end of this sentence.
				 */
				addWord(s + " ");
				done = true;
			} else {
				/*
				 * Add a word, including its trailing separator
				 */
				addWord(s.substring(0, endOfWord));
				s = s.substring(endOfWord); // remove the word from s
			}
		}
	}

	/**
	 * Returns the index of the first character of the second word of a string.
	 * Words are separated by either a space or a line separator. Empty words
	 * are allowed; that is, consecutive word separators are treated as if they
	 * separate empty words. Thus the value returned is the index of the first
	 * character following the first word separator, even if it is another
	 * separator. If the string does not contain a word separator, the value
	 * returned is -1.
	 * 
	 * @param s
	 *            The string to examine
	 * @return the index of the character of s following the first occurrence of
	 *         a space or line separator, or -1 if none exists
	 */
	private static int nextWord(String s) {
		for (int i = 0; i < s.length(); i++)
			if (s.startsWith(LINESEP, i))
				return i + LINESEP.length();
			else if (s.charAt(i) == ' ')
				return i + 1;
		return -1;
	}

	/**
	 * Appends a blank line at the end of the current message. Useful for
	 * creating a new paragraph of information in the message output.
	 */
	public void appendBlankLine() {
		if (f_lineTextWidth != 0) {
			f_buffer.append(LINESEP);
			f_lineTextWidth = 0;
		}
		f_buffer.append(LINESEP);
	}

	/**
	 * Adds a single word to the message, breaking for a new line if the width
	 * of the current line would become greater than the desired text width.
	 * 
	 * @param word
	 *            the word to add to this message's buffer.
	 */
	private void addWord(String word) {
		if (f_lineTextWidth + word.length() > f_desiredTextWidth) {
			f_buffer.append(LINESEP);
			f_lineTextWidth = 0;
		}
		f_buffer.append(word);
		if (word.endsWith(LINESEP))
			f_lineTextWidth = 0;
		else
			f_lineTextWidth += word.length();
	}

	/**
	 * Clears this message.
	 */
	public void clear() {
		f_lineTextWidth = 0;
		f_buffer.delete(0, f_buffer.length()); // clear the message
	}

	/**
	 * Makes this word wrapped message a copy of the specified word wrapped
	 * message. Both the contents and the desired text width are copied.
	 * 
	 * @param message
	 *            the word wrapped message to copy
	 */
	public void replaceContents(WordWrappedMessage message) {
		clear();
		f_buffer.append(message);
		f_lineTextWidth = message.f_lineTextWidth;
		f_desiredTextWidth = message.f_desiredTextWidth;
	}

	/**
	 * Sets the desired text width of lines. Specifies the column at which we
	 * word wrap the text appended into this message.
	 * 
	 * @param textWidth
	 *            the desired width of lines in this message.
	 */
	public void setDesiredTextWidth(int textWidth) {
		assert (textWidth > 20);
		f_desiredTextWidth = textWidth;
	}

	@Override
	public String toString() {
		return f_buffer.toString();
	}
}
