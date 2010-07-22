package com.surelogic.analysis.uniqueness.uwm.store;

/**
 * Replaces the STATE_* constants as well as the PseudoVariable class.  We can
 * use the enumeration elements SHARED, BORROWED, and UNDEFINED directly instead
 * of creating PseudoVariable wrappers.
 */
enum State {
  NULL,
  UNIQUE,
  SHARED,
  BORROWED,
  UNDEFINED;
}
