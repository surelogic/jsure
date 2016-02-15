/*
 * Copyright (c) 2015 SureLogic, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the parameter, local variable, return value, or field to which
 * this annotation is applied is a reference to an object or <code>null</code>.
 * 
 * <p>
 * When applied to a method declaration, this annotation means that the method's
 * return value might be <code>null</code>. There is no need to a explicitly
 * differentiate between the return value and the receiver using a
 * <code>value</code> attribute, as we do, for example, with {@link Initialized}
 * , because the receiver is always non-<code>null</code>.
 * 
 * <p>
 * An unannotated local variable, parameter, return value, or field is already
 * assumed to be able to be <code>null</code>. This annotation is useful as an
 * explicit documentation that the programmer has considered the possibilities
 * and is aware that the value may be <code>null</code>. Additionally, assurance
 * may make use of the annotation to report warnings about the use of possibly
 * null values that might otherwise be suppressed.
 * <p>
 * The method {@link Cast#toNullable(Object)} must be used to assign a partially
 * initialized object, annotated with {@link Initialized}, to a {@link Nullable}
 * reference. While this should be avoided, an example is shown below.
 * 
 * <h3>Semantics:</h3>
 * 
 * <i>Local variable:</i> The local variable may be <code>null</code>.
 * <p>
 * 
 * <i>Parameter:</i> At the start of the method's execution, the annotated
 * parameter may be <code>null</code>.
 * <p>
 * <i>Return Value:</i> The value returned by the annotated method may be
 * <code>null</code>.
 * <p>
 * <i>Field:</i> The value of the field may be <code>null</code>.
 * 
 * <h3>Examples:</h3>
 * 
 * Here we declare two formal parameters, but only annotate one of them as
 * {@link Nullable}:
 * 
 * <pre>
 * public int totalLength(String s1, @Nullable String s2) {
 *   return s1.length() + s2.length();
 * }
 * </pre>
 * 
 * An assurance of nullabilty should not report any problems that may arise from
 * the use the <code>s1</code>, even though it is internally considered to be
 * possibly <code>null</code>, because it does not have any explicit design
 * intent. Assurance is free, however, to report problems about the use of
 * <code>s2</code> because it is annotated. The "Nullable Assurance" of JSure
 * reports a warning on the use of the <code>s2</code> in
 * <code>s2.length()</code> indicating that the call may dereference a null
 * value.
 * <p>
 * 
 * A correct implementation of the above method is
 * 
 * <pre>
 * public int totalLength(String s1, @Nullable String s2) {
 *   final int l1 = s1 == null ? 0 : s1.length();
 *   final int l2 = s2 == null ? 0 : s2.length();
 *   return l1 + l2;
 * }
 * </pre>
 * 
 * It is also possible to explicitly annotate a field, primarily for
 * documentation purposes, that may contain a <code>null</code> value.
 * <p>
 * The example code below shows a simple use of this annotation on a field, as
 * well as getter (return value) and setter (parameter) for a nullable value.
 * 
 * <pre>
 * public class Container {
 * 
 *   &#064;Nullable
 *   private Object value;
 * 
 *   &#064;Nullable
 *   public Object getValue() {
 *     return value;
 *   }
 * 
 *   public void setValue(@Nullable Object newValue) {
 *     value = newValue;
 *   }
 * }
 * </pre>
 * <p>
 * In the somewhat contrived example below the partially initialized object
 * referenced by the parameter <code>myPeer</code> is "cast" with
 * {@link Cast#toNonNull(Object)} and {@link Cast#toNullable(Object)},
 * respectively, so that it can be assigned to the non-null field
 * <code>peer1</code> and the nullable field <code>peer2</code>.
 * 
 * <pre>
 * &#064;ThreadSafe
 * public class B {
 * 
 *   &#064;NonNull
 *   private final A peer1;
 * 
 *   &#064;Nullable
 *   private final A peer2;
 * 
 *   &#064;RegionEffects(&quot;none&quot;)
 *   &#064;Starts(&quot;nothing&quot;)
 *   B(@Initialized(through = &quot;Object&quot;) A myPeer) {
 *     peer1 = Cast.toNonNull(myPeer);
 *     peer2 = Cast.toNullable(myPeer);
 *   }
 * }
 * </pre>
 * 
 * A "cast" is required because the it is not deemed safe to assign a partially
 * initialized object to a field (annotated or unannotated).
 * 
 * @see NonNull
 * @see Initialized
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE })
public @interface Nullable {
  // marker interface
}
