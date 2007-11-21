package com.surelogic.opgen.syntax;

final class UnknownProperty implements Property {
  private final String name;

  UnknownProperty(String name) {
    this.name = name;
  }

  public String getMessage() {
    return null;
  }

  public String getName() {
    return name;
  }
}
