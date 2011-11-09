package com.surelogic.opgen.util;

import java.io.*;
import java.util.*;

public class LineWordParser {
  private final LineNumberReader reader;
  private StringTokenizer words;
  private String next = null;
  
  public LineWordParser(String opFilename) throws FileNotFoundException {
    reader = new LineNumberReader(new FileReader(opFilename));
  }
  
  public String readLine() throws IOException {
    StringBuilder sb = null;
    if (next != null) {
      sb = new StringBuilder(next);
      sb.append(" ");
      next = null;
    }
    if (words != null) {
      if (sb == null) {
        sb = new StringBuilder();
      }
      while (words.hasMoreTokens()) {
        sb.append(words.nextToken());  
        sb.append(" ");
      }
      words = null;
    }
    if (sb != null && sb.length() > 0) {
      return sb.toString();
    }
    return reader.readLine();
  }
  
  public void pushLine(String line) {   
    if (words != null) {
      throw new IllegalArgumentException("There are already words being parsed");
    }
    words = new StringTokenizer(line);
  }
  
  private String nextWord() throws IOException {
    while (words == null || !words.hasMoreTokens()) {
      String line = reader.readLine();
      if (line == null) {
        return null;
      }
      words = new StringTokenizer(line);
    }
    // assume that words has been setup to have a token
    String next = words.nextToken();
    if (next.startsWith("//")) {
      // skip the rest of the line
      words = null;
      return nextWord();
    }
    return next;
  }
  
  public String readWord() throws IOException {
    if (next != null) {
      String rv = next;
      next = null;
      return rv;
    }
    return nextWord();
  }
  
  public void pushWord(String word) {
    if (next != null) {
      throw new IllegalArgumentException("You already pushed a word");
    }
    if (word.equals("")) {
      throw new IllegalArgumentException("Trying to push an empty string");
    }
    next = word;
  }

  public void close() throws IOException {
    reader.close();
  }
}
