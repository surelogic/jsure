package edu.cmu.cs.fluid.promise.parse;

/**
 * A file to show what all the promises should look like
 * @author Edwin Chan
 * @region public Public extends All
 * @region private Private extends java.lang.Object:Instance
 * @region protected Protected extends Instance
 * @region Default extends java.lang.Object:All
 * @region static Static extends java.lang.Object:All
 * @-region InterfaceRegion
 * @-mapRegion InterfaceRegion into Default
 * @lock Lock is this protects Instance
 */
public class PromiseTest {
 /** @mapInto Default */
 int count; 
 
 /**
  * @unique
  * @aggregate [] into Private, length into Private
  */
 private Object[] elements;

 /**
  * @writes this.Default, o.All
  * @borrowed this
  * @unique o
  * @param o {@unique}
  */ 
 void add(Object o) {
   count++;
 } 

 /**
  * @reads this.Private
  * @writes nothing
  * @borrowed this
  * @requiresLock Lock
  * @return {@borrowed}
  */
 Object get(int i) {
   return elements[i];
 }

 /**
  * @-ignore any(Object).All,
  * @writes  any(java.lang.Object).All
  * @borrowed this, o, lock
  * @param lock {@isLock Lock}
  * @isLock lock Lock
  */ 
 void add(Object o, Object lock) {
   count++;
 }

 /**
  * @reads nothing
  * @return {@borrowed} {@isLock Lock}
  * @returnsLock Lock
  */
 Object getLock() {
 	 return this;
 }
 
 /**
  * @synchronized
  */
 PromiseTest() {} 
}
