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

package org.gtri.util.xmlbuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import org.gtri.util.iteratee.api.Enumerator;
import org.gtri.util.iteratee.api.Iteratee;
import org.gtri.util.xmlbuilder.api.XmlEvent;
import org.gtri.util.xmlbuilder.impl.XmlReader;
import org.gtri.util.xmlbuilder.impl.XmlWriter;
        
/**
 *
 * @author lance.gatlin@gmail.com
 */
public final class XmlFactory implements org.gtri.util.xmlbuilder.api.XmlFactory {
  private XmlFactory() { }
  public static final XmlFactory INSTANCE = new XmlFactory();
  public static XmlFactory instance() { return INSTANCE; }
  public static final int STD_CHUNK_SIZE = 256;

  @Override
  public Enumerator<XmlEvent> createXmlReader(final XMLStreamReaderFactory factory, int chunkSize) {
    return new XmlReader(factory, chunkSize);
  }
  
  public Enumerator<XmlEvent> createXmlReader(final XMLStreamReaderFactory factory) {
    return createXmlReader(factory, STD_CHUNK_SIZE);
  }
  
  private abstract class Lazy<T> {
    private T value = null;
    T get() {
      if(value == null) {
        value = init();
      }
      return value;
    }
    abstract T init();
  }
  
  public Enumerator<XmlEvent> createXmlReader(final InputStream in) {
    return createXmlReader(in, STD_CHUNK_SIZE);
  }
  
  public Enumerator<XmlEvent> createXmlReader(final InputStream in, int chunkSize) {
    final Lazy<ByteArrayOutputStream> lazyBaos = new Lazy<ByteArrayOutputStream>() {
      @Override
      public ByteArrayOutputStream init() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        try {
          n = in.read(buf);
          while (n > 0) {
            baos.write(buf, 0, n);
            n = in.read(buf);
          }
        } catch (IOException ex) {
          Logger.getLogger(XmlFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return baos;
      }
    };
    
    return createXmlReader(
      new XMLStreamReaderFactory() {
        @Override
        public Result create() throws XMLStreamException {
          byte[] content = lazyBaos.get().toByteArray();
          return new XMLStreamReaderFactory.Result(XMLInputFactory.newInstance().createXMLStreamReader(new ByteArrayInputStream(content)), content.length);
        }
      }, 
      chunkSize
    );
  }

  @Override
  public Iteratee<XmlEvent,?> createXmlWriter(final XMLStreamWriterFactory factory) {
    return new XmlWriter(factory);
  }
  
  public Iteratee<XmlEvent,?> createXmlWriter(final OutputStream out) {
    return createXmlWriter(new XMLStreamWriterFactory(){ 

      @Override
      public XMLStreamWriter create() throws XMLStreamException {
        Processor p = new Processor(false);
        Serializer s = new Serializer(out);
        s.setProcessor(p);
        s.setOutputProperty(Serializer.Property.ENCODING, "UTF-8");
        s.setOutputProperty(Serializer.Property.INDENT, "yes");
        s.setOutputProperty(Serializer.Property.SAXON_INDENT_SPACES, "2");
        s.setOutputProperty(Serializer.Property.SAXON_LINE_LENGTH, "80");
        try {
            return s.getXMLStreamWriter();
        } catch (SaxonApiException ex) {
          throw new RuntimeException(ex);
        }
      }
      //        return XMLOutputFactory.newInstance().createXMLStreamWriter(out, "UTF-8");
      
      
    });
  }
}
