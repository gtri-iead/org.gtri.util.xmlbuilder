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

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
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
  public void testWriteXml() throws XMLStreamException, FileNotFoundException, IOException {
    System.out.println("===TEST WRITE XML===");
    System.out.println("===Building Plan===");
    Enumerator<XmlEvent> reader = XmlFactory.instance().createXmlReader(new FileInputStream("src/test/resources/test.xsd"),1);
    Iteratee<XmlEvent,?> writer = XmlFactory.instance().createXmlWriter(new FileOutputStream("target/test.out.xsd"));
    Plan2<XmlEvent,?> plan = factory.createPlan(reader, writer);
    System.out.println("===Running Plan===");
    
    Plan2.State.Result<XmlEvent,?> lastResult = null;
    for(Plan2.State.Result<XmlEvent,?> current : plan) {
      System.out.println("progress=" + current.next().progress());
      lastResult = current;
    }
    lastResult = lastResult.next().endOfInput();
    
//    Plan2.RunResult<XmlEvent,?> r = plan.run();
//    System.out.println("===Issues===");
//    for(Issue issue : current.allIssues()) {
//      System.out.println(issue);
//    }
    assertTrue(lastResult.next().statusCode().isSuccess());
    assertTrue(streamsAreEqual(new FileInputStream("src/test/resources/test.xsd"), new FileInputStream("target/test.out.xsd")));
  }
  
  @Test
  public void testPrintXml() throws XMLStreamException, FileNotFoundException {
    System.out.println("===TEST PRINT XML===");
    System.out.println("===Building Plan===");
    Enumerator<XmlEvent> reader = XmlFactory.instance().createXmlReader(new FileInputStream("src/test/resources/test.xsd"),1);
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

  public class LinesOfText extends ArrayList<String> {

    LinesOfText(final String filename) throws IOException, FileNotFoundException {
      readLines(new FileReader(filename));
    }

    LinesOfText(final InputStream in) throws IOException {
      readLines(new InputStreamReader(in));
    }

    private void readLines(final Reader r) throws IOException {
      String line = "";
      final BufferedReader in = new BufferedReader(r);
      while ((line = in.readLine()) != null) {
        this.add(line);
      }
    }
  }
  
  boolean streamsAreEqual(final InputStream in, final InputStream compare) throws FileNotFoundException, IOException {
      /*
       * COMPARE the first output to the second
       */
      final LinesOfText l1 = new LinesOfText(in);
      final LinesOfText l2 = new LinesOfText(compare);

      Patch patch = DiffUtils.diff(l1, l2);

      for(final Delta delta : patch.getDeltas()) {
        System.out.println(delta.getOriginal());
        System.out.println(delta.getRevised());
        System.out.println("");
      }
      return patch.getDeltas().isEmpty();
    }
}
