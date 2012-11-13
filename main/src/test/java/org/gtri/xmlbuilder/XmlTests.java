/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gtri.xmlbuilder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javax.xml.stream.XMLStreamException;
import org.gtri.util.iteratee.api.*;
import org.gtri.util.xmlbuilder.XmlFactory;
import org.gtri.util.xmlbuilder.impl.XmlEvent;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 *
 * @author Lance
 */
public class XmlTests {
  Planner planner = new org.gtri.util.iteratee.impl.Planner();
  
  public XmlTests() {
  }
  
  @BeforeClass
  public static void setUpClass() throws Exception {
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

  @Test
  public void testXml() throws XMLStreamException, FileNotFoundException {
    Producer<XmlEvent> reader = XmlFactory.instance().createXmlReader(new FileInputStream("src/test/resources/test.xsd"));
//    XmlWriter writer = XmlFactory.instance().createXmlWriter(new FileOutputStream("target/test.out.xsd"));
    Consumer<XmlEvent> c = new org.gtri.util.xmlbuilder.impl.test.XmlTestConsumer();
    planner.connect(reader, c).run();
  }
}
