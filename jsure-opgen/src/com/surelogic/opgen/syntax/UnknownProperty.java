package com.surelogic.opgen.syntax;

final class UnknownProperty implements Property {
  private final String name;

  UnknownProperty(String name) {
    this.name = name;
  }

  @Override
  public String getMessage() {
    return null;
  }

  @Override
  public String getName() {
    return name;
  }
}
