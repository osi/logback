/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2009, QOS.ch. All rights reserved.
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
package ch.qos.logback.classic;

import static ch.qos.logback.classic.ClassicTestConstants.ISO_REGEX;
import static ch.qos.logback.classic.ClassicTestConstants.MAIN_REGEX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.pattern.ConverterTest;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.testUtil.SampleConverter;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.pattern.parser.AbstractPatternLayoutBaseTest;
import ch.qos.logback.core.testUtil.StringListAppender;

public class PatternLayoutTest extends AbstractPatternLayoutBaseTest<ILoggingEvent> {

  private PatternLayout pl = new PatternLayout();
  private LoggerContext lc = new LoggerContext();
  Logger logger = lc.getLogger(ConverterTest.class);
  Logger root = lc.getLogger(Logger.ROOT_LOGGER_NAME);
  
  ILoggingEvent le;
  List<String> optionList = new ArrayList<String>();

  public PatternLayoutTest() {
    super();
    Exception ex = new Exception("Bogus exception");
    le = makeLoggingEvent(ex);
  }

  @Before
  public void setUp() {
    pl.setContext(lc);
  }

  ILoggingEvent makeLoggingEvent(Exception ex) {
    return new LoggingEvent(
        ch.qos.logback.core.pattern.FormattingConverter.class.getName(),
        logger, Level.INFO, "Some message", ex, null);
  }

  @Override
  public ILoggingEvent getEventObject() {
    return makeLoggingEvent(null);
  }

  public PatternLayoutBase<ILoggingEvent> getPatternLayoutBase() {
    return new PatternLayout();
  }

  @Test
  public void testOK() {
    pl.setPattern("%d %le [%t] %lo{30} - %m%n");
    pl.start();
    String val = pl.doLayout(getEventObject());
    // 2006-02-01 22:38:06,212 INFO [main] c.q.l.pattern.ConverterTest - Some
    // message
    String regex = ISO_REGEX + " INFO " + MAIN_REGEX
        + " c.q.l.c.pattern.ConverterTest - Some message\\s*";

    assertTrue(val.matches(regex));
  }

  @Test
  public void testNoExeptionHandler() {
    pl.setPattern("%m%n");
    pl.start();
    String val = pl.doLayout(le);
    assertTrue(val.contains("java.lang.Exception: Bogus exception"));
  }

  @Test
  public void testCompositePattern() {
    pl.setPattern("%-56(%d %lo{20}) - %m%n");
    pl.start();
    String val = pl.doLayout(getEventObject());
    // 2008-03-18 21:55:54,250 c.q.l.c.pattern.ConverterTest - Some message
    String regex = ISO_REGEX
        + " c.q.l.c.p.ConverterTest          - Some message\\s*";
    assertTrue(val.matches(regex));
  }

  @Test
  public void contextProperty() {
    pl.setPattern("%property{a}");
    pl.start();
    lc.putProperty("a", "b");
    
    String val = pl.doLayout(getEventObject());
    assertEquals("b", val);
  }
  @Test
  public void testNopExeptionHandler() {
    pl.setPattern("%nopex %m%n");
    pl.start();
    String val = pl.doLayout(le);
    assertTrue(!val.contains("java.lang.Exception: Bogus exception"));
  }

  @Test
  public void testWithParenthesis() {
    pl.setPattern("\\(%msg:%msg\\) %msg");
    pl.start();
    le = makeLoggingEvent(null);
    String val = pl.doLayout(le);
    // System.out.println("VAL == " + val);
    assertEquals("(Some message:Some message) Some message", val);
  }

  @Test
  public void testWithLettersComingFromLog4j() {
    // Letters: p = level and c = logger
    pl.setPattern("%d %p [%t] %c{30} - %m%n");
    pl.start();
    String val = pl.doLayout(getEventObject());
    // 2006-02-01 22:38:06,212 INFO [main] c.q.l.pattern.ConverterTest - Some
    // message
    String regex = ClassicTestConstants.ISO_REGEX + " INFO " + MAIN_REGEX
        + " c.q.l.c.pattern.ConverterTest - Some message\\s*";
    assertTrue(val.matches(regex));
  }

  @Test
  public void contextNameTest() {
    pl.setPattern("%contextName");
    lc.setName("aValue");
    pl.start();
    String val = pl.doLayout(getEventObject());
    assertEquals("aValue", val);
  }

  @Override
  public Context getContext() {
    return lc;
  }

  void configure(String file) throws JoranException {
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(lc);
    jc.doConfigure(file);
  }

  @Test
  public void testConversionRuleSupportInPatternLayout() throws JoranException {
    configure(ClassicTestConstants.JORAN_INPUT_PREFIX + "conversionRule/patternLayout0.xml");
    root.getAppender("LIST");
    String msg  = "Simon says";
    logger.debug(msg);
    StringListAppender<ILoggingEvent> sla = (StringListAppender<ILoggingEvent>)    root.getAppender("LIST");
    assertNotNull(sla);
    assertEquals(1, sla.strList.size());
    assertEquals(SampleConverter.SAMPLE_STR+" - "+msg, sla.strList.get(0)); 
  }
}
