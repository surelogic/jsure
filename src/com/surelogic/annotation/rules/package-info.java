/**
 * This contains the standard rules for defining regions, locks, method/thread effects,
 * and uniqueness.
 * <p>
 * Like the old system, each set of associated rules is defined in one class 
 * (a subclass of AnnotationRules):
 * <ul>
 * <li> A one or more com.surelogic.annotation.IAnnotationParseRule(s)
 * <br>   To parse text from whatever source (Java 5, Javadoc, XML) into AASTs
 * <li> Any associated com.surelogic.annotation.scrub.IAnnotationScrubber(s)
 * <br>   To scrub AASTs and create appropriate enclosing drops 
 * <li> Any associated com.surelogic.promise.IPromiseDropStorage(s)
 * <br>   To define SlotInfos for mapping their promisedFor IRNodes to the drops
 * </ul>
 */
package com.surelogic.annotation.rules;