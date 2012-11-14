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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.gtri.util.iteratee.api.Consumer;
import org.gtri.util.iteratee.api.Producer;

/**
 *
 * @author Lance
 */
public interface XmlFactory {
  
  public static interface XMLStreamReaderFactory {
    XMLStreamReader create() throws XMLStreamException;
  }
  
  public static interface XMLStreamWriterFactory {
    XMLStreamWriter create() throws XMLStreamException;
  }
  
  Producer<XmlEvent> createXmlReader(XMLStreamReaderFactory factory, int chunkSize);
  
  Consumer<XmlEvent> createXmlWriter(XMLStreamWriterFactory factory);  
}
