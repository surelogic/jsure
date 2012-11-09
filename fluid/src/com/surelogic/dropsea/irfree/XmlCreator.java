package com.surelogic.dropsea.irfree;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.surelogic.Nullable;
import com.surelogic.common.xml.Entities;

public class XmlCreator {

  private final PrintWriter pw;

  /**
   * Used to buffer output to the PrintWriter above
   */
  private final StringBuilder sb = new StringBuilder();
  protected final Builder b = new Builder(0);

  protected XmlCreator(OutputStream out) throws IOException {
    if (out != null) {
      pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
      pw.println("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
    } else {
      pw = null;
    }
  }

  protected final void flushBuffer() {
    if (pw != null) {
      if (sb.length() > 0) {
        pw.println(sb.toString());
        sb.setLength(0);
      }
      pw.flush();
    }
  }

  protected final void close() {
    flushBuffer();
    if (pw != null) {
      pw.close();
    }
  }

  /**
   * Encapsulates the creation of an XML entity
   * 
   * Q: how to write the output incrementally?
   */
  public class Builder {
    final Map<String, String> attributes = new HashMap<String, String>();

    final int indent;
    boolean firstAttr = true;
    String name;

    // TODO will this take up too much memory to keep everything around?
    final List<Builder> nested = new ArrayList<Builder>(0);

    Builder(int indent) {
      this.indent = indent;
    }

    public Builder nest(String name) {
      if (nested.isEmpty()) {
        // First nested entity, so we need to close
        Entities.closeStart(sb, false, true);
      }
      flushBuffer();
      // TODO these could be reused?
      Builder n = new Builder(indent + 1);
      nested.add(n);
      n.start(name);
      return n;
    }

    final void reset() {
      firstAttr = true;
      attributes.clear();
      nested.clear();
      name = null;
    }

    public void start(String name) {
      reset();
      this.name = name;
      Entities.start(name, sb, indent);
    }

    public void end() {
      if (nested.isEmpty()) {
        Entities.closeStart(sb, true, true);
      } else {
        Entities.end(name, sb, indent);
      }
      flushBuffer();
    }

    public final void addAttribute(String name, boolean value) {
      if (value) {
        addAttribute(name, Boolean.toString(value));
      }
    }

    public final void addAttribute(String name, int value) {
      addAttribute(name, Integer.toString(value));
    }

    public final boolean addAttribute(String name, Long value) {
      if (value == null) {
        return false;
      }
      addAttribute(name, value.toString());
      return true;
    }

    public final void addAttribute(String name, String value) {
      addAttribute(name, value, null);
    }

    public final void addAttribute(String name, String value, @Nullable Entities useToEscape) {
      String oldValue = attributes.get(name);
      if (oldValue != null) {
        if (oldValue.equals(value)) {
          return;
        } else {
          throw new IllegalStateException("Attribute set twice: " + name + " = " + oldValue + " (old), " + value + " (new)");
        }
      }
      if (firstAttr) {
        firstAttr = false;
      } else {
        // sb.append("\n\t");
        Entities.newLine(sb, indent);
      }
      Entities.addAttribute(name, value, useToEscape, sb);
      attributes.put(name, value);
    }

    public Map<String, String> getAttributes() {
      return attributes;
    }

    public boolean hasNested() {
      return !nested.isEmpty();
    }

    public Iterable<Builder> getNestedBuilders() {
      if (nested.isEmpty()) {
        return Collections.emptyList();
      }
      return nested;
    }

    public Entity build() {
      // System.out.println("Building "+name+": "+this);
      Entity e = new Entity(name, getAttributes());
      for (Builder n : getNestedBuilders()) {
        e.addRef(n.build());
      }
      return e;
    }
  }
}
