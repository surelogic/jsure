/*$Header: /cvs/fluid/fluid/src/com/surelogic/promise/PromiseDropGroup.java,v 1.1 2007/09/25 19:50:51 chance Exp $*/
package com.surelogic.promise;

public class PromiseDropGroup {
  final String name;
  
  private PromiseDropGroup(String name) {
    this.name = name;
  }
  
  public final String name() {
    return name;
  }
}
