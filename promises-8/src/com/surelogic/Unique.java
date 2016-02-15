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
 * Declares that the parameter, receiver, return value, or field to which this
 * annotation is applied is a unique reference to an object. Normally this
 * indicates that the referenced object is not aliased. It is allowed, however,
 * to pass a unique reference to a method as an actual argument or the receiver
 * if the formal argument or receiver, respectively, is {@link Borrowed}. That
 * is, {@link Unique} values can be safely passed to the parameter or used as
 * the receiver with the guarantee that they will still be unique when the
 * method returns. Said another way we create a temporary alias on the stack
 * then ensure that it goes away.
 * <p>
 * Annotating <code>&#64;Unique("return")</code> on a constructor is defined to
 * be equivalent to annotating <code>&#64;Borrowed("this")</code>. Either of
 * these annotations indicates that the object being constructed is not aliased
 * during construction, which implies that the reference "returned" by the
 * {@code new} expression that invokes the constructor is unique. Which
 * annotation is preferred, <code>&#64;Unique("return")</code> or
 * <code>&#64;Borrowed("this")</code>, is a matter of programmer preference.
 * <p>
 * Methods that override a method annotated with
 * <code>&#64;Unique("return")</code> must also be explicitly annotated
 * <code>&#64;Unique("return")</code>. It is a modeling error if they are not.
 * Methods that override a method with a <code>&#64;Unique</code> parameter are
 * <i>not</i> required to maintain that parameter's uniqueness (uniqueness on a
 * parameter is contravariant), but may via explicit annotation.
 * <p>
 * Annotating <code>&#64;Unique</code> on a field is defined to mean that the
 * {@code Instance} region of the object referenced by the annotated field is
 * mapped into the {@code Instance} region of the object that contains the
 * annotated field if the annotated field is {@code final}. If the annotated
 * field is not {@code final}, the {@code Instance} region of the object
 * referenced by the annotated field is mapped into the field itself. A
 * {@code final} {@code static} field cannot be annotated with
 * <code>&#64;Unique</code>: the annotation {@link UniqueInRegion} must be used
 * instead.
 * <p>
 * It is a modeling error to annotate a reference if the type is primitive. For
 * example,
 * 
 * <pre>
 * &#64;Unique("return") public int getValue() { &hellip; }
 * </pre>
 * 
 * and
 * 
 * <pre>
 * public void setValue(&#64;Unique int value) { &hellip; }
 * </pre>
 * 
 * would generate modeling errors.
 * 
 * <h3>Semantics:</h3>
 * 
 * <i>Parameter:</i> At the start of the method's execution the annotated
 * parameter is either <code>null</code> or refers to an object that is not
 * referenced by a field of any object.
 * <p>
 * <i>Return Value:</i> The value returned by the annotated method is
 * <code>null</code> or is an object that is not referenced by a field of any
 * object.
 * <p>
 * <i>Receiver:</i> For a method, the object the method was invoked upon, at any
 * call to the method must be a unique reference. This places an assertion on
 * the caller of a method, not upon the method implementation itself.
 * <p>
 * <i>Field:</i> At all times, the value of the annotated field is either
 * <code>null</code> or is an object that is not referenced by a field of any
 * other object or another field of the same object. The {@code Instance} region
 * of the object referenced by the annotated field is mapped into the
 * {@code Instance} region of the object that contains the annotated field.
 * 
 * <h3>Examples:</h3>
 * 
 * A class with a unique field. Note that the method {@code getUniqueObject}
 * ensures that the returned reference is unique by creating a new object.
 * 
 * <pre>
 * public class UniqueField {
 * 
 *   &#064;Unique
 *   private Object o;
 * 
 *   public void setField(@Unique Object value) {
 *     o = value;
 *     process(o);
 *   }
 * 
 *   &#064;Unique(&quot;return&quot;)
 *   public Object getUniqueObject() {
 *     return new Object();
 *   }
 * 
 *   &#064;RegionEffects(&quot;reads value:Instance&quot;)
 *   public static void process(@Borrowed Object value) {
 *     // for future processing
 *   }
 * }
 * </pre>
 * 
 * The call to {@code process} is allowed because that method promises that the
 * alias to {@code o} created by the {@code value} parameter will be returned
 * once the method call completes. However, {@code process} must also promise
 * not to read state outside of its passed parameters. This is done with the
 * {@link RegionEffects} annotation on the {@code process} method declaration.
 * <p>
 * This annotation is often used to support a {@link RegionLock} assertion on a
 * constructor because if the receiver is not leaked during object construction
 * then the state under construction will remain within the thread that invoked
 * {@code new}.
 * 
 * <pre>
 * &#064;RegionLock(&quot;Lock is this protects Instance&quot;)
 * public class Example {
 * 
 *   int x = 1;
 *   int y;
 * 
 *   &#064;Unique(&quot;return&quot;)
 *   public Example(int y) {
 *     this.y = y;
 *   }
 *   ...
 * }
 * </pre>
 * 
 * The scoped promise {@link Promise} can be used if the constructor is implicit
 * (i.e., generated by the compiler). It has the ability to place promises on
 * implicit and explicit constructors.
 * 
 * <pre>
 * &#064;RegionLock(&quot;Lock is this protects Instance&quot;)
 * &#064;Promise(&quot;@Unique(return) for new(**)&quot;)
 * public class Example {
 *   int x = 1;
 *   int y = 1;
 *   ...
 * }
 * </pre>
 * 
 * @see Borrowed
 * @see Region
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER })
public @interface Unique {
  /**
   * When annotating a method, this attribute is used to disambiguate whether
   * the annotation refers to the method's receiver, the method's return value,
   * or both. The value is comma separated list of tokens, and has the following
   * set of legal values (ignoring white space issues):
   * <ul>
   * <li>{@code ""}
   * <li>{@code "this"}
   * <li>{@code "return"}
   * <li>{@code "this, return"}
   * <li>{@code "return, this"}
   * </ul>
   * 
   * <p>
   * The values are interpreted thusly
   * <ul>
   * <li>If the list contains the value {@code "this"}, it indicates the
   * receiver is unique. This value is only allowed on methods.
   * <li>If the list contains the value {@code "return"}, it indicates the
   * return value is unique. This value is allowed on methods and constructors.
   * <li>If the list contains both {@code "this"} and {@code "return"}, it
   * indicates that both the receiver and the return value are unique. This
   * value is only allowed on methods.
   * </ul>
   * <p>
   * This attribute is not used when annotating a parameter or a field: the
   * attribute value must be the empty string in these cases.
   * <p>
   * The value of this attribute must conform to the following grammar (in <a
   * href="http://www.ietf.org/rfc/rfc4234.txt">Augmented Backus&ndash;Naur
   * Form</a>):
   * 
   * <pre>
   * value = [(&quot;this&quot; [&quot;,&quot; &quot;return&quot;]) / (&quot;return&quot; [&quot;,&quot; &quot;this&quot;])] ; See above comments
   * </pre>
   * 
   * @return a value following the syntax described above.
   */
  String value() default "";
}
