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
 * Container annotation for multiple {@link Initialized} annotations on a
 * method. It is a modeling error for a method to have both an
 * {@link Initialized} and a {@link InitializedReferences} annotation.
 * 
 * <h3>Semantics:</h3>
 * 
 * This annotation holds a list of {@link Initialized} declarations without
 * imposing any further constraint on the program's implementation.
 * 
 * <h3>Examples:</h3>
 * 
 * The only reason to apply more than one &#064;Initialized annotation to a
 * method is to declare that both the return value and the receiver of the
 * method are raw references. The example below declares a method
 * <code>C.process()</code> whose receiver is assumed to be initialized through
 * class <code>B</code> only, and that returns a reference to a <code>Z</code>
 * object that may only be assumed to be initialized through <code>X</code>.
 * 
 * <pre>
 * class X { &hellip; }
 * class Y { &hellip; }
 * class Z { &hellip; }
 * 
 * class A { &hellip; }
 * class B { &hellip; }
 * 
 * class C {
 *   &hellip;
 *   &#064;InitializedReferences({
 *     &#064;Initialized(through=&quot;B&quot;, value=&quot;this&quot;),
 *     &#064;Initialized(through=&quot;X&quot;, value=&quot;return&quot;)
 *   })
 *   public Z processIt() { &hellip; }
 * }
 * </pre>
 * 
 * @see Initialized
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface InitializedReferences {
  /**
   * The {@link Initialized} annotations to apply to the method.
   * 
   * @return the {@link Initialized} annotations to apply to the method.
   */
  Initialized[] value();
}
