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

import collection.mutable.ArrayBuffer
import javax.xml.stream.{XMLStreamReader, XMLStreamConstants}
import org.gtri.util.xsddatatypes._
import org.gtri.util.xmlbuilder.api.XmlEvent
import org.gtri.util.xmlbuilder.api.XmlFactory.XMLStreamReaderFactory
import org.gtri.util.iteratee.api._
import scala.Some
import org.gtri.util.iteratee.impl.{Enumerators, ImmutableBuffers}
import org.gtri.util.iteratee.impl.Enumerators._

import org.gtri.util.iteratee.impl.ImmutableBuffers.Conversions._

/**
 * Created with IntelliJ IDEA.
 * User: Lance
 * Date: 11/4/12
 * Time: 6:49 PM
 * To change this template use File | Settings | File Templates.
 */
object XmlReader {
}
class XmlReader(factory : XMLStreamReaderFactory, val chunkSize : Int = 256) extends Enumerator[XmlEvent] {
  def initialState() = Cont(factory.create())

  case class Cont(reader : XMLStreamReader) extends Enumerators.Cont[XmlEvent] {
    // TODO: provide progress somehow
    def progress = Progress.empty

    def step() = {
      val buffer = new ArrayBuffer[XmlEvent](chunkSize)
      while(buffer.size < chunkSize && reader.hasNext) {
        for(event <- nextEvent) {
          buffer.append(event)
        }
      }
      if(buffer.nonEmpty) {
        Result(this, collection.immutable.Vector(buffer: _*), ImmutableBuffers.empty)
      } else {
        Result(Success(progress), ImmutableBuffers.empty, ImmutableBuffers.empty)
      }
    }

    private def nextEvent : Option[XmlEvent] = {
      reader.next match {
        case XMLStreamConstants.START_DOCUMENT=> {
          Some(StartXmlDocumentEvent(
            reader.getEncoding,
            reader.getVersion,
            reader.isStandalone,
            reader.getCharacterEncodingScheme,
            getLocatorFromReader
          ))
        }
        case XMLStreamConstants.END_DOCUMENT => {
          Some(EndXmlDocumentEvent(getLocatorFromReader))
        }
        case XMLStreamConstants.START_ELEMENT => {
          Some(AddXmlElementEvent(fetchElementFromReader(), getLocatorFromReader))

        }
        case XMLStreamConstants.END_ELEMENT => {
          Some(EndXmlElementEvent(getElementQNameFromReader, getLocatorFromReader))
        }
        case XMLStreamConstants.CHARACTERS => {
          Some(AddXmlTextEvent(reader.getText(), getLocatorFromReader))
        }
        case XMLStreamConstants.CDATA => {
          Some(AddXmlTextEvent(reader.getText(), getLocatorFromReader))
        }
        case XMLStreamConstants.COMMENT => {
          Some(AddXmlCommentEvent(reader.getText(), getLocatorFromReader))
        }
        case _ => None
      }
    }

    private def getElementQNameFromReader : XsdQName = {
      val prefix = newXsdNCName(reader.getPrefix())
      val uri = newXsdAnyURI(reader.getNamespaceURI())
      val localName = new XsdNCName(reader.getLocalName())
      new XsdQName(prefix, uri, localName)
    }

    private def fetchElementFromReader() : XmlElement = {
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

      // TODO: peek for value
      val value = None
      XmlElement(qName, value, attributes.toMap, prefixes.toMap)
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
  }
}

//class XmlReader(factory : XMLStreamReaderFactory, val chunkSize : Int = 256) extends Enumerator[XmlEvent] {
//
//  def initialState = XmlReader.State(factory.create())
//}
//
//
//
//class XmlReader(factory : XMLStreamReaderFactory, val chunkSize : java.lang.Integer = 8) extends Producer[XmlEvent] {
//
//  def enumerator : Enumerator[XmlEvent] = {
//    new Enumerator[XmlEvent] {
//
//      private val outputBuffer = new ArrayBuffer[XmlEvent](chunkSize)
//      private val inputQueue = new Queue[XmlEvent]()
//      private val reader = factory.create()
//
//      def enumerate[V](i : Iteratee[XmlEvent,V]) : Iteratee[XmlEvent,V] = {
//        doEnumerate(next, i)
//      }
//
//      def close() {
//        reader.close()
//      }
//
//      @tailrec
//      private def next : Option[XmlEvent] = {
//        if(inputQueue.nonEmpty) {
//          Some(inputQueue.dequeue)
//        } else {
//          if(reader.hasNext) {
//            reader.next match {
//              case XMLStreamConstants.START_DOCUMENT=> {
//                Some(StartXmlDocumentEvent(
//                  reader.getEncoding,
//                  reader.getVersion,
//                  reader.isStandalone,
//                  reader.getCharacterEncodingScheme,
//                  getLocatorFromReader
//                ))
//              }
//              case XMLStreamConstants.END_DOCUMENT => {
//                Some(EndXmlDocumentEvent(getLocatorFromReader))
//              }
//              case XMLStreamConstants.START_ELEMENT => {
////                val value = peekParseElementValue(next, Nil)
////                Some(AddXmlElementEvent(getElementFromReader(value), getLocatorFromReader))
//                workAroundTailRecIssue
//              }
//              case XMLStreamConstants.END_ELEMENT => {
//                Some(EndXmlElementEvent(getElementQNameFromReader, getLocatorFromReader))
//              }
//              case XMLStreamConstants.CHARACTERS => {
//                Some(AddXmlTextEvent(reader.getText(), getLocatorFromReader))
//              }
//              case XMLStreamConstants.CDATA => {
//                Some(AddXmlTextEvent(reader.getText(), getLocatorFromReader))
//              }
//              case XMLStreamConstants.COMMENT => {
//                Some(AddXmlCommentEvent(reader.getText(), getLocatorFromReader))
//            }
//              case _ =>
//                next
//            }
//          } else {
//            None
//          }
//        }
//      }
//
//      private def workAroundTailRecIssue : Option[XmlEvent] = {
//        Some(AddXmlElementEvent(fetchElementFromReader, getLocatorFromReader))
//      }
//
//      @tailrec
//      private def doEnumerate[V](nextEvent : Option[XmlEvent], i : Iteratee[XmlEvent,V]) : Iteratee[XmlEvent,V] = {
//        i match {
//          case i@Done(_,_) => i
//          case Cont(k) =>
//            if(nextEvent.isDefined) {
//              if(outputBuffer.size < chunkSize) {
//                val event = nextEvent.get
//                outputBuffer += event
//                doEnumerate[V](next, i)
//              } else {
//                val nextI = flushOutputBuffer(k)
//                doEnumerate[V](next, nextI)
//              }
//            } else {
//              flushOutputBuffer(k)
//            }
//        }
//      }
//
//      private def flushOutputBuffer[V](k: (Input[XmlEvent]) => Iteratee[XmlEvent, V]) : Iteratee[XmlEvent, V] = {
//        val buffer = outputBuffer.toList
//        outputBuffer.clear
//        k(El(buffer,Nil))
//      }
//
//      private def getElementQNameFromReader : XsdQName = {
//        val prefix = newXsdNCName(reader.getPrefix())
//        val uri = newXsdAnyURI(reader.getNamespaceURI())
//        val localName = new XsdNCName(reader.getLocalName())
//        new XsdQName(prefix, uri, localName)
//      }
//
//      private def fetchElementFromReader() : XmlElement = {
//        val qName = getElementQNameFromReader
//
//        val attributes = for(i <- 0 until reader.getAttributeCount())
//        yield {
//          val prefix = newXsdNCName(reader.getPrefix())
//          val uri = newXsdAnyURI(reader.getAttributeNamespace(i))
//          val localName = new XsdNCName(reader.getAttributeLocalName(i))
//          val qName = new XsdQName(prefix, uri, localName)
//          val value = reader.getAttributeValue(i)
//          qName -> value
//        }
//
//        val prefixes =
//          for(i <- 0 until reader.getNamespaceCount())
//          yield {
//            val prefix = newXsdNCName(reader.getNamespacePrefix(i))
//            val uri = newXsdAnyURI(reader.getNamespaceURI(i))
//            prefix -> uri
//          }
//
//        // Note: this must come after all the other element parameters are extracted from reader since this call will change the reader's state
////        val value = peekParseElementValue(next, Nil)
//        val value = None
//        XmlElement(qName, value, attributes.toMap, prefixes.toMap)
//      }
//
//      private def newXsdAnyURI(uri: String) = {
//        if(uri == null) {
//          XmlConstants.NULL_NS_URI
//        } else {
//          new XsdAnyURI(uri)
//        }
//      }
//      private def newXsdNCName(name : String) = {
//        if(name == null) {
//          XmlConstants.DEFAULT_NS_PREFIX
//        } else {
//          new XsdNCName(name)
//        }
//      }
//      private def getLocatorFromReader : XmlLocator = {
//        val l = reader.getLocation()
//        XmlLocator(l.getCharacterOffset, l.getColumnNumber, l.getLineNumber, l.getPublicId, l.getSystemId)
//      }
//
//      // Peek at the next few XmlEvents - if it is a string of text events followed by an end event then compress
//      // the sequence by extracting the combined "value" of the text events and throw away the individual
//      // text events
//      @tailrec
//      private def peekParseElementValue(nextEvent : Option[XmlEvent], textEvents : List[AddXmlTextEvent]) : Option[String] = {
//        if(nextEvent.isDefined) {
//          nextEvent.get match {
//            case e:AddXmlTextEvent => {
//              peekParseElementValue(next, e :: textEvents)
//            }
//            case e:EndXmlElementEvent => {
//              inputQueue.enqueue(e)
//              val result = textEvents.foldLeft(new StringBuilder) { (s,textEvent) => s.append(textEvent.text) }
//              Some(result.toString)
//            }
//            case e:XmlEvent  => {
//              textEvents.reverse foreach { inputQueue.enqueue(_) }
//              inputQueue.enqueue(e)
//              None
//            }
//          }
//        } else {
//          textEvents.reverse foreach { inputQueue.enqueue(_) }
//          None
//        }
//      }
//    }
//  }
//}
//
