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
    along with org.gtri.util.iteratee library. If not, see <http://www.gnu.org/licenses/>.

*/
package org.gtri.util.xmlbuilder.api;

import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.gtri.util.iteratee.api.Enumerator;
import org.gtri.util.iteratee.api.Iteratee;

/**
 * An interface for a factory object that can be used to create XMLReader and
 * XMLWriter objects.
 * 
 * @author lance.gatlin@gmail.com
 */
public interface XmlFactory {
  /**
   * An interface for a factory that creates XMLStreamReaders
   */
  public static interface XMLStreamReaderFactory {
    /**
     * The immutable result of the create method
     */
    static final class Result {
      private final XMLStreamReader reader;
      private final int totalByteSize;

      public Result(XMLStreamReader reader, int totalByteSize) {
        this.reader = reader;
        this.totalByteSize = totalByteSize;
      }

      public XMLStreamReader reader() {
        return reader;
      }

      public int totalByteSize() {
        return totalByteSize;
      }
      
    }
    /**
     * Create an XMLStreamReader
     * @return immutable result
     * @throws XMLStreamException 
     */
    Result create() throws XMLStreamException;
  }
  
  /**
   * Create a XMLStreamReaderFactory that caches an InputStream and creates
   * XMLStreamReaders from the cache.
   * @param in
   * @return an XMLStreamReader that reads from a cache of the InputStream
   */
  public XMLStreamReaderFactory createXMLStreamReaderFactory(InputStream in);
  
  /**
   * An interface for a factory to create XMLStreamWriters
   */
  public static interface XMLStreamWriterFactory {
    /**
     * Create an XMLStreamWriter
     * @return
     * @throws XMLStreamException 
     */
    XMLStreamWriter create() throws XMLStreamException;
  }
  
  /**
   * Create an XMLStreamWriterFactory that will write to the OutputStream using
   * the following formatting options:
   *   ENCODING = UTF-8
   *   INDENT = yes
   *   INDENT_SPACES = 2
   *   LINE_LENGTH = 80
   * @param out
   * @return 
   */
  public XMLStreamWriterFactory createXMLStreamWriterFactory(OutputStream out);
  
  /**
   * Create an XMLReader
   * @param factory to utilize to create the XMLStreamReader
   * @param chunkSize the size of the output buffers
   * @return an XMLReader
   */
  Enumerator<XmlEvent> createXmlReader(XMLStreamReaderFactory factory, int chunkSize);
  
  /**
   * Create an XMLWriter
   * @param factory to utilize to create the XMLStreamWriter
   * @return an XMLWriter
   */
  Iteratee<XmlEvent,?> createXmlWriter(XMLStreamWriterFactory factory);  
}
