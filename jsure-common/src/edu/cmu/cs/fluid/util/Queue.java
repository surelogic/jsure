/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/Queue.java,v 1.3 2003/07/02 20:19:07 thallora Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.NoSuchElementException;

/** A simple implementation of queues.
 */
public class Queue {
  public Queue() {}
  private QueueElement head = null;
  private QueueElement tail = null;
  public void enqueue(Object value) {
    QueueElement qe = new QueueElement(value,null);
    if (head == null) {
      head = qe;
    } else {
      tail.next = qe;
    }
    tail = qe;
    // System.out.println("Enqueued " + value);
  }
  public Object dequeue() throws NoSuchElementException {
    QueueElement qe = head;
    if (qe == null) throw new NoSuchElementException("queue is empty");
    head = qe.next;
    // System.out.println("Dequeued " + qe.value);
    return qe.value;
  }
  public boolean isEmpty() {
    return head == null;
  }
}

class QueueElement {
  Object value;
  QueueElement next;
  QueueElement(Object v, QueueElement n) {
    value = v;
    next = n;
  }
}
