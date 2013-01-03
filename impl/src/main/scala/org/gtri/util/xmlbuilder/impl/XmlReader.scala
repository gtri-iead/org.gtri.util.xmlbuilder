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

package org.gtri.util.xmlbuilder.impl


import javax.xml.stream.{XMLStreamReader, XMLStreamConstants}
import org.gtri.util.xsddatatypes._
import org.gtri.util.xmlbuilder.api.XmlEvent
import org.gtri.util.xmlbuilder.api.XmlFactory.XMLStreamReaderFactory
import org.gtri.util.iteratee.api._
import org.gtri.util.iteratee.impl.Enumerators._

import org.gtri.util.iteratee.impl.ImmutableBufferConversions._
import annotation.tailrec

/**
 * Created with IntelliJ IDEA.
 * User: Lance
 * Date: 11/4/12
 * Time: 6:49 PM
 * To change this template use File | Settings | File Templates.
 */
class XmlReader(factory : XMLStreamReaderFactory, issueHandlingCode : IssueHandlingCode = IssueHandlingCode.NORMAL, val chunkSize : Int = 256) extends Enumerator[XmlEvent] {
  require(chunkSize > 0)

  def initialState() = {
    val result = factory.create()
    Cont(result.reader(), new Progress(0,0,result.totalByteSize))
  }

  case class Cont(reader : XMLStreamReader, val progress : Progress) extends Enumerator.State[XmlEvent] {

    def statusCode = if(reader.hasNext) StatusCode.CONTINUE else StatusCode.SUCCESS

    def step() = {
      // TODO: what happens if reader fails?

      // Note: may exceed buffer size due to peek - this shouldn't matter downstream though
      val buffer = new collection.mutable.ArrayBuffer[XmlEvent](chunkSize)
//      if(reader.getEventType == XMLStreamConstants.START_DOCUMENT) {
//        buffer.append(StartXmlDocumentEvent(
//          reader.getEncoding,
//          reader.getVersion,
//          reader.isStandalone,
//          reader.getCharacterEncodingScheme,
//          getLocatorFromReader
//        ))
//        reader.next()
//      }
      // Fill buffer
      while(buffer.size < chunkSize && reader.hasNext) {
        for(event <- nextEvents().reverse) {
          buffer.append(event)
        }
      }
      // Calc next progress if possible
      val nextProgress = {
        if(progress.totalItemCount > 0) {
          val charOffset = reader.getLocation.getCharacterOffset
          // -1 is returned after eoi reached
          if(charOffset == -1) {
            new Progress(0,progress.totalItemCount,progress.totalItemCount)
          } else {
            new Progress(0,reader.getLocation.getCharacterOffset,progress.totalItemCount)
          }
        } else {
          Progress.empty
        }
      }
      // If buffer is empty we are done
      if(buffer.isEmpty) {
        Success(nextProgress)
      } else {
        val immutableCopyOfBuffer : IndexedSeq[XmlEvent] = buffer.toIndexedSeq
        Result(Cont(reader, nextProgress), immutableCopyOfBuffer)
      }
    }

    // Gets the next event - if next event is an element will peek at next few elements to try to extract value
    // and will return the events it peeked at inaddition to the AddXmlElementEvent
    private def nextEvents() : List[XmlEvent] = {
      reader.getEventType() match {
        case XMLStreamConstants.START_DOCUMENT=> {
          val retv = List(StartXmlDocumentEvent(
            reader.getEncoding,
            reader.getVersion,
            reader.isStandalone,
            reader.getCharacterEncodingScheme,
            getLocatorFromReader
          ))
          reader.next()
          retv
        }
        case XMLStreamConstants.END_DOCUMENT => {
          val retv = List(EndXmlDocumentEvent(getLocatorFromReader))
          reader.next()
          retv
        }
        case XMLStreamConstants.START_ELEMENT => {
          val (qName, attributes, prefixes) = getElementInfoFromReader()
          val locator = getLocatorFromReader
          reader.next()
          val (value, peekQueue) = peekParseElementValue()
          peekQueue :::
            AddXmlElementEvent(XmlElement(qName, value, attributes.toMap, prefixes.toMap), locator) :: Nil
        }
        case XMLStreamConstants.END_ELEMENT => {
          val retv = List(EndXmlElementEvent(getElementQNameFromReader, getLocatorFromReader))
          reader.next()
          retv
        }
        case XMLStreamConstants.CHARACTERS => {
          val retv = List(AddXmlTextEvent(reader.getText(), getLocatorFromReader))
          reader.next()
          retv
        }
        case XMLStreamConstants.CDATA => {
          val retv = List(AddXmlTextEvent(reader.getText(), getLocatorFromReader))
          reader.next()
          retv
        }
        case XMLStreamConstants.COMMENT => {
          val retv = List(AddXmlCommentEvent(reader.getText(), getLocatorFromReader))
          reader.next()
          retv
        }
        case _ =>
          reader.next()
          nextEvents()
      }
    }

    private def getElementQNameFromReader : XsdQName = {
      val prefix = newXsdNCName(reader.getPrefix())
      val uri = newXsdAnyURI(reader.getNamespaceURI())
      val localName = new XsdNCName(reader.getLocalName())
      new XsdQName(prefix, uri, localName)
    }

//    private def fetchElementFromReader(peekQueue : collection.mutable.Queue[XmlEvent]) : XmlElement = {
    private def getElementInfoFromReader() : (XsdQName, Seq[(XsdQName, String)], Seq[(XsdNCName, XsdAnyURI)]) = {
      val qName = getElementQNameFromReader
      val attributes = {
        for(i <- 0 until reader.getAttributeCount())
        yield {
          val prefix = newXsdNCName(reader.getPrefix())
          val uri = newXsdAnyURI(reader.getAttributeNamespace(i))
          val localName = new XsdNCName(reader.getAttributeLocalName(i))
          val qName = new XsdQName(prefix, uri, localName)
          val value = reader.getAttributeValue(i)
          qName -> value
        }
      }

      val prefixes = {
        for(i <- 0 until reader.getNamespaceCount())
        yield {
          val prefix = newXsdNCName(reader.getNamespacePrefix(i))
          val uri = newXsdAnyURI(reader.getNamespaceURI(i))
          prefix -> uri
        }
      }
      (qName, attributes, prefixes)
    }

    private def newXsdAnyURI(uri: String) = {
      if(uri == null) {
        XmlConstants.NULL_NS_URI
      } else {
        new XsdAnyURI(uri)
      }
    }
    private def newXsdNCName(name : String) = {
      if(name == null) {
        XmlConstants.DEFAULT_NS_PREFIX
      } else {
        new XsdNCName(name)
      }
    }
    private def getLocatorFromReader : XmlFileLocator = {
      val l = reader.getLocation()
      XmlFileLocator(l.getCharacterOffset, l.getColumnNumber, l.getLineNumber, l.getPublicId, l.getSystemId)
    }
    // Peek at the next few XmlEvents - if it is a string of text events followed by an end event then compress
    // the sequence by extracting the combined "value" of the text events and throw away the individual
    // text events
    @tailrec
    private def peekParseElementValue(peekQueue : List[XmlEvent] = Nil) : (Option[String], List[XmlEvent]) = {
      val events = nextEvents()
      events match {
        case List(e:AddXmlTextEvent) => {
          peekParseElementValue(e :: peekQueue)
        }
        case List(e:EndXmlElementEvent) => {
          if(peekQueue.nonEmpty) {
            val result = peekQueue.foldLeft(new StringBuilder) {
              (s,event) =>
                event match {
                  case AddXmlTextEvent(text,_) => s.append(text)
                }
            }
            (Some(result.toString), events)
          } else {
            (None, events)
          }
        }
        case _ => {
          (None, events ::: peekQueue)
        }
      }
    }
  }
}