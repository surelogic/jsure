package com.surelogic.analysis.uniqueness.classic.sideeffecting.store;

/**
 * Replaces the STATE_* constants as well as the PseudoVariable class.  We can
 * use the enumeration elements SHARED, BORROWED, and UNDEFINED directly instead
 * of creating PseudoVariable wrappers.
 */
public enum State {
  NULL,
  UNIQUE,
  SHARED,
  BORROWED,
  UNDEFINED;
}
