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
 * Container annotation for multiple {@link Promise} annotations. It is a
 * modeling error for an entity to have both a {@link Promises} and a
 * {@link Promise} annotation.
 * 
 * <h3>Semantics:</h3>
 * 
 * This annotation holds a list of {@link Promise} annotations without imposing
 * any further constraint on the program's implementation.
 * 
 * <h3>Examples:</h3>
 * 
 * Declaring two {@link Promise} annotations for the same class. The first
 * places a {@link Starts} annotation on all the constructors in the class and
 * the second places a {@link Borrowed} annotation on all the methods and
 * constructors in the class.
 * 
 * <pre>
 * &#064;Promises({
 *   &#064;Promise(&quot;@Starts(nothing) for new(**)&quot;),
 *   &#064;Promise(&quot;@Borrowed(this)&quot;)
 * })
 * class Example { ... }
 * </pre>
 * 
 * @see Promise
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PACKAGE, ElementType.TYPE })
public @interface Promises {
  /**
   * The {@link Promise} annotations to apply to the class.
   * 
   * @return the {@link Promise} annotations to apply to the class.
   */
  Promise[] value();
}
