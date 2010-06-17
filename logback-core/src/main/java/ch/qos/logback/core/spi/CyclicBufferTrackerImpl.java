/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2010, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.core.spi;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.helpers.CyclicBuffer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Ceki G&uuml;c&uuml;
 */
public class CyclicBufferTrackerImpl<E> implements CyclicBufferTracker<E> {

  int bufferSize = DEFAULT_BUFFER_SIZE;
  int maxNumBuffers = DEFAULT_NUMBER_OF_BUFFERS;
  int bufferCount = 0;

  private Map<String, Entry> map = new HashMap<String, Entry>();

  private Entry head; // least recently used entries are towards the head
  private Entry tail; // most recently used entries are towards the tail
  long lastCheck = 0;

  public CyclicBufferTrackerImpl() {
    head = new Entry(null, null, 0);
    tail = head;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public void setBufferSize(int size) {
  }

  public int getMaxNumberOfBuffers() {
    return maxNumBuffers;
  }

  public void setMaxNumberOfBuffers(int maxNumBuffers) {
    this.maxNumBuffers = maxNumBuffers;
  }

  public CyclicBuffer<E> get(String key, long timestamp) {
    Entry existing = map.get(key);
    if (existing == null) {
      CyclicBuffer<E> cb = processNewEntry(key, timestamp);
      return cb;
    } else {
      existing.setTimestamp(timestamp);
      moveToTail(existing);
      return existing.value;
    }
  }

  private CyclicBuffer<E> processNewEntry(String key, long timestamp) {
    CyclicBuffer<E> cb = new CyclicBuffer<E>(bufferSize);
    Entry entry = new Entry(key, cb, timestamp);
    map.put(key, entry);
    bufferCount++;
    rearrangeTailLinks(entry);
    if (bufferCount >= maxNumBuffers) {
      removeHead();
    }
    return cb;
  }

  private void removeHead() {
    CyclicBuffer cb = head.value;
    if (cb != null) {
      cb.clear();
    }
    map.remove(head.key);
    bufferCount--;
    head = head.next;
    head.prev = null;
  }

  private void moveToTail(Entry e) {
    rearrangePreexistingLinks(e);
    rearrangeTailLinks(e);
  }

  private void rearrangePreexistingLinks(Entry e) {
    if (e.prev != null) {
      e.prev.next = e.next;
    }
    if (e.next != null) {
      e.next.prev = e.prev;
    }
    if (head == e) {
      head = e.next;
    }
  }


  public synchronized void clearStaleBuffers(long now) {
    if (lastCheck + CoreConstants.MILLIS_IN_ONE_SECOND > now) {
      return;
    }
    lastCheck = now;
    while (head.value != null && isEntryStale(head, now)) {
      CyclicBuffer<E> cb = head.value;
      cb.clear();
      removeHead();
    }
  }

  final private boolean isEntryStale(Entry entry, long now) {
    return ((entry.timestamp + THRESHOLD) < now);
  }

   List<String> keyList() {
    List<String> result = new LinkedList<String>();
    Entry e = head;
    while (e != tail) {
      result.add(e.key);
      e = e.next;
    }
    return result;
  }
  private void rearrangeTailLinks(Entry e) {
    if (head == tail) {
      head = e;
    }
    Entry preTail = tail.prev;
    if (preTail != null) {
      preTail.next = e;
    }
    e.prev = preTail;
    e.next = tail;
    tail.prev = e;
  }

  // ================================================================

  private class Entry {
    Entry next;
    Entry prev;

    String key;
    CyclicBuffer<E> value;
    long timestamp;

    Entry(String k, CyclicBuffer<E> v, long timestamp) {
      this.key = k;
      this.value = v;
      this.timestamp = timestamp;
    }

    public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((key == null) ? 0 : key.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      final Entry other = (Entry) obj;
      if (key == null) {
        if (other.key != null)
          return false;
      } else if (!key.equals(other.key))
        return false;
      if (value == null) {
        if (other.value != null)
          return false;
      } else if (!value.equals(other.value))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "(" + key + ", " + value + ")";
    }
  }
}