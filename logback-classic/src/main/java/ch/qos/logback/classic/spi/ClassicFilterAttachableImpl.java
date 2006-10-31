/**
 * Logback: the generic, reliable, fast and flexible logging framework.
 * 
 * Copyright (C) 1999-2006, QOS.ch
 * 
 * This library is free software, you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation.
 */
package ch.qos.logback.classic.spi;

import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.filter.ClassicFilter;
import ch.qos.logback.core.filter.Filter;

/**
 * Implementation of ClassicFilterAttachable.
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
final public class ClassicFilterAttachableImpl implements ClassicFilterAttachable {

  ClassicFilter headFilter;
  ClassicFilter tailFilter;

  /**
   * Add a filter to end of the filter list.
   */
  public void addFilter(ClassicFilter newFilter) {
    if (headFilter == null) {
      headFilter = newFilter;
      tailFilter = newFilter;
    } else {
      tailFilter.setNext(newFilter);
      tailFilter = newFilter;
    }
  }

  /**
   * Get first filter in the chain.
   */
  public ClassicFilter getFirstFilter() {
    return headFilter;
  }

  /**
   * Clear the filter chain
   */
  public void clearAllFilters() {
    ClassicFilter f = headFilter;
    while (f != null) {
      final ClassicFilter next = f.getNext();
      f.setNext(null);
      f = next;
    }
    f = null;
    headFilter = null;
    tailFilter = null;
  }

  /**
   * Loop through the filters in the chain. As soon as a filter decides on 
   * ACCEPT or DENY, then that value is returned. If all of the filters return
   * NEUTRAL, then  NEUTRAL is returned.
   */
  public int getFilterChainDecision(Marker marker, Logger logger,
      Level level, String format, Object[] params, Throwable t) {
    ClassicFilter f = headFilter;

    while (f != null) {
      switch (f.decide(marker, logger,  level,  format, params,  t)) {
      case Filter.DENY:
        return Filter.DENY;

      case Filter.ACCEPT:
        return Filter.ACCEPT;

      case Filter.NEUTRAL:
        f = f.getNext();
      }
    }
    return Filter.NEUTRAL;
  }
}