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
 * Declares that the object referenced by the local variable, return value,
 * receiver, or formal parameter may be only partially initialized. That is,
 * some of the fields of the class may hold <code>null</code> values even though
 * the field is annotated to be {@link NonNull} and final fields may be
 * unassigned and still contain default language values.
 * <em>Furthermore, the local variable,
 * return value, receiver, or formal parameter is itself non-<code>null</code>.</em>
 * <p>
 * The extent to which the referenced object is guaranteed to be initialized is described by
 * the {@link #through() through} attribute.
 * <p>
 * When the annotation appears on a method, the {@link #value() value} attribute
 * is used to distinguish whether the return value or receiver is being
 * annotated. However, to concisely support the greatest amount of Java code,
 * <tt>"this"</tt> is considered the default value on a method. That is, <code>
 * &#064;Initialized(through=&quot;Object&quot;) void m() { &hellip; }
 * </code> is considered identical to <code>
 * &#064;Initialized(through=&quot;Object&quot;, value=&quot;this&quot;) void m() { &hellip; }
 * </code> but, of course, either syntax is accepted.
 * <p>
 * Partially initialized references originate in constructors: an object of type
 * <code>C</code> is not fully initialized until its final constructor, the one
 * defined within <code>C</code>, executes. If a reference to the receiver is
 * passed to another method during construction, then that reference is
 * partially initialized through the most recently constructed superclass
 * component. That is, if we have class <code>C</code> extends <code>B</code>
 * extends <code>A</code>, and during the execution of the constructor for
 * <code>B</code> a reference to the object being constructed is leaked, then
 * that reference is partially initialized through <code>A</code>.
 * <p>
 * The special methods {@link Cast#toNonNull(Object)} or
 * {@link Cast#toNullable(Object)} must be used to assign a partially
 * initialized object to any other type of reference, even an unannotated one.
 * While this should be avoided, an example is shown below.
 * <p>
 * This annotation is required to document the use of partially initialized
 * objects in classes annotated with {@link TrackPartiallyInitialized}. But it
 * may be placed within any code where initialization is deemed interesting.
 * However, it is recommended that such code be annotated to be
 * {@link TrackPartiallyInitialized} so that a full specification is done for
 * such types.
 * 
 * <h3>Semantics:</h3>
 * 
 * <i>Local variable:</i> The local variable will never be <code>null</code> and
 * always refers to an object that is at least as initialized as the given
 * {@link #through() through} attribute.
 * <p>
 * 
 * <i>Parameter:</i> At the start of the method's execution, the annotated
 * parameter may refer to an object that is not fully initialized. The parameter
 * is never <code>null</code> and always refers to an object that is at least as
 * initialized as the given {@link #through() through} attribute.
 * <p>
 * <i>Return Value:</i> The value returned by the annotated method refers to an
 * object that is not fully initialized. The return value is not
 * <code>null</code> and always refers to an object that is at least as
 * initialized as the given {@link #through() through} attribute.
 * <p>
 * <i>Receiver:</i> For a method, the object the method was invoked upon, may
 * not be fully initialized. The receiver is never <code>null</code> and always
 * refers to an object that is at least as initialized as the given
 * {@link #through() through} attribute.
 * 
 * <h3>Examples:</h3>
 * 
 * Note that many of the examples for this annotation reflect poor Java idioms.
 * It is best practice to call as few methods in a constructor as possible, and
 * to <i>never</i> invoke a dispatching method. That said, in real-world systems
 * this ideal may not be possible so it is important to understand how to deal
 * with such code.
 * <p>
 * The constructor for <code>A</code> uses the object under construction as the
 * receiver for the method <code>m()</code>. Thus method <code>m()</code> cannot
 * assume the receiver is fully initialized. In particular, the field
 * <code>path</code> in <code>B.m()</code> cannot be assumed to be non-
 * <code>null</code> even though it is annotated as {@link NonNull}.
 * 
 * <pre>
 * &#064;TrackPartiallyInitialized
 * class A {
 *   &#064;NonNull
 *   protected String name;
 * 
 *   public A(@NonNull String s) {
 *     this.name = s;
 *     this.m(55);
 *   }
 * 
 *   &#064;Initialized(through=&quot;Object&quot;)
 *   protected void m(int x) { &amp;hellip }
 * }
 * 
 * &#064;TrackPartiallyInitialized
 * class B extends A {
 *   &#064;NonNull
 *   protected String path;
 * 
 *   public B(@NonNull String p, @NonNull String s) {
 *     super(s);
 *     path = p;
 *   }
 * 
 *   &#064;Initialized(through=&quot;Object&quot;)
 *   protected void m(int x) {
 *     &amp;hellip this.path &amp;hellip // Cannot assume that &quot;this.path&quot; is non-null
 *   }
 * }
 * </pre>
 * <p>
 * In the somewhat contrived example below the partially initialized object
 * referenced by the parameter <code>myPeer</code> is "cast" with
 * {@link Cast#toNonNull(Object)} and {@link Cast#toNullable(Object)},
 * respectively, such that it can be assigned to the non-null field
 * <code>peer1</code> and the nullable field <code>peer2</code>.
 * 
 * <pre>
 * &#064;ThreadSafe
 * &#064;TrackPartiallyInitialized
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
 * A cast is required because it is not generally safe to assign a partially
 * initialized object to a field (annotated or unannotated). This example is
 * acceptable, however, because the reference is never dereferenced.
 * 
 * @see TrackPartiallyInitialized
 * @see NonNull
 * @see Nullable
 * @see InitializedReferences
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE })
public @interface Initialized {
  /**
   * Describes the extent to which the referenced object must be initialized.
   * <dl>
   * <dt>through="<i>type-name</i>"
   * <dd>The object may be assumed to initialized up to the named ancestor type.
   * That is, the constructors for each ancestor type from
   * <code>java.lang.Object</code> down to and including the named type have
   * been successfully invoked. Any {@link NonNull} field in the object declared
   * in types that are descendants of the named type may in fact be
   * <code>null</code>. The name may be fully qualified or relative to the
   * imports declared in the compilation unit
   * </dl>
   * 
   * @return the extent to which the referenced object must be initialized.
   */
  String through();

  /**
   * When annotating a method, this attribute is used to disambiguate whether
   * the annotation refers to the method's receiver, the method's return value,
   * or both. The value is comma separated list of tokens, and has the following
   * set of legal values (ignoring white space issues):
   * <ul>
   * <li>{@code ""}
   * <li>{@code "this"}
   * <li>{@code "return"}
   * </ul>
   * 
   * <p>
   * The values are interpreted thusly
   * <ul>
   * <li>If the list contains the value {@code "this"}, it indicates the
   * receiver may be partially initialized. This value is only allowed on
   * methods.
   * <li>If the list contains the value {@code "return"}, it indicates the
   * return value may be partially initialized. This value is only allowed on
   * methods.
   * </ul>
   * <p>
   * This attribute is not used when annotating a parameter or a field: the
   * attribute value must be the empty string in these cases.
   * <p>
   * If this attribute is not given when annotating a method: {@code "this"} is
   * used.
   * <p>
   * The value of this attribute must conform to the following grammar (in <a
   * href="http://www.ietf.org/rfc/rfc4234.txt">Augmented Backus&ndash;Naur
   * Form</a>):
   * 
   * <pre>
   * value = [&quot;this&quot;  / &quot;return&quot; ] ; See above comments
   * </pre>
   * 
   * @return a value following the syntax described above.
   */
  String value() default "";
}
