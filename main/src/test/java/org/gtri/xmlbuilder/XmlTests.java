/*
    Copyright 2012 Georgia Tech Research Institute

    Author: lance.gatlin@gtri.gatech.edu

    This file is part of org.gtri.util.xmlbuilder library.

    org.gtri.util.xmlbuilder library is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    org.gtri.util.xmlbuilder library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with org.gtri.util.xmlbuilder library. If not, see <http://www.gnu.org/licenses/>.

*/

package org.gtri.xmlbuilder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import javax.xml.stream.XMLStreamException;
import org.gtri.util.iteratee.api.*;
import org.gtri.util.iteratee.impl.test.TestPrintConsumer;
import org.gtri.util.iteratee.IterateeFactory;
import org.gtri.util.xmlbuilder.XmlFactory;
import org.gtri.util.xmlbuilder.api.XmlEvent;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lance.gatlin@gmail.com
 */
public class XmlTests {
  IterateeFactory factory = new IterateeFactory();
  
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
  public void testWriteXml() throws XMLStreamException, FileNotFoundException {
    System.out.println("===TEST WRITE XML===");
    System.out.println("===Building Plan===");
    Enumerator<XmlEvent> reader = XmlFactory.instance().createXmlReader(new FileInputStream("src/test/resources/test.xsd"));
    Iteratee<XmlEvent,?> writer = XmlFactory.instance().createXmlWriter(new FileOutputStream("target/test.out.xsd"));
    Plan2<XmlEvent,?> plan = factory.createPlan(reader, writer);
    System.out.println("===Running Plan===");
    Plan2.RunResult<XmlEvent,?> r = plan.run();
    System.out.println("===Issues===");
    for(Issue issue : r.allIssues()) {
      System.out.println(issue);
    }
    assertTrue(r.statusCode().isSuccess());
  }
  @Test
  public void testPrintXml() throws XMLStreamException, FileNotFoundException {
    System.out.println("===TEST PRINT XML===");
    System.out.println("===Building Plan===");
    Enumerator<XmlEvent> reader = XmlFactory.instance().createXmlReader(new FileInputStream("src/test/resources/test.xsd"));
    Iteratee<XmlEvent,?> writer = new TestPrintConsumer<XmlEvent>();
    Plan2<XmlEvent,?> plan = factory.createPlan(reader, writer);
    System.out.println("===Running Plan===");
    Plan2.RunResult<XmlEvent,?> r = plan.run();
    System.out.println("===Issues===");
    for(Issue issue : r.allIssues()) {
      System.out.println(issue);
    }
    assertTrue(r.statusCode().isSuccess());
  }
}
