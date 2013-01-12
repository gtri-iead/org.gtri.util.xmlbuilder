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

import javax.xml.stream.{XMLStreamConstants, XMLStreamReader}
import org.gtri.util.scala.exelog.sideeffects._
import org.gtri.util.xsddatatypes._
import org.gtri.util.issue.Issues
import org.gtri.util.issue.api.{ Issue, IssueHandlingStrategy }
import org.gtri.util.iteratee.api._
import org.gtri.util.iteratee.impl.iteratees.Chunk
import org.gtri.util.iteratee.impl.ImmutableBufferConversions._
import org.gtri.util.iteratee.impl.enumerators._
import org.gtri.util.xmlbuilder.api.XmlEvent
import org.gtri.util.xmlbuilder.api.XmlFactory.XMLStreamReaderFactory
import org.gtri.util.xmlbuilder.impl.events._
import annotation.tailrec


object XmlReader {
  implicit val classlog = ClassLog(classOf[XmlReader])
}
class XmlReader(
  factory : XMLStreamReaderFactory,
  issueHandlingStrategy : IssueHandlingStrategy,
  val chunkSize : Int = 256
) extends Enumerator[XmlEvent] {
  import XmlReader._
  require(chunkSize > 0)

  def initialState() = {
    implicit val log = enter("initialState")()
    try {
      +"Trying to create reader"
      val result = factory.create()
      +"Created reader"
      Cont(result.reader(), new Progress(0,0,result.totalByteSize)) <~: log
    } catch {
      case e : Exception =>
        log.fatal("Failed to create reader",e)
        val msg : String = e.getMessage
        val issue : Issue = Issues.INSTANCE.fatalError(msg)
        Failure[XmlEvent](
          progress = Progress.empty,
          issues = Chunk(issue)
        ) <~: log
    }
  }

  case class Cont(reader : XMLStreamReader, val progress : Progress) extends Enumerator.State[XmlEvent] {

    def statusCode = StatusCode.CONTINUE

    def step() = {
      implicit val log = enter("step")()
      // TODO: what happens if reader fails?
      +"Filling buffer"
      ~s"Creating buffer of size=$chunkSize"
      val buffer = new collection.mutable.ArrayBuffer[XmlEvent](chunkSize)
      ~s"Filling buffer with nextEvents"
      while(buffer.size < chunkSize && reader.hasNext) {
        buffer ++= nextEvents().reverse
      }
      ~s"Filled buffer#: $buffer"

      +"Calculating nextProgress"
      val nextProgress = {
        if(progress.totalItemCount > 0) {
          val charOffset = reader.getLocation.getCharacterOffset
          ~s"Got charOffset=$charOffset -1 is returned after eoi reached"
          if(charOffset == -1) {
            ~s"At eoi"
            new Progress(0,progress.totalItemCount,progress.totalItemCount)
          } else {
            ~s"Some progress"
            new Progress(0,reader.getLocation.getCharacterOffset,progress.totalItemCount)
          }
        } else {
          ~s"Not possible to calc nextProgress setting to empty"
          Progress.empty
        }
      }
      ~s"nextProgress=$nextProgress"

      +s"If buffer is empty we are done"
      if(buffer.isEmpty) {
        +s"Buffer is empty - close reader and return Success"
        reader.close()

        Success[XmlEvent](
          progress = nextProgress,
          output = Chunk(EndXmlDocumentEvent(getLocatorFromReader))
        ) <~: log
      } else {
        +s"Buffer not empty - make immutable copy and return result"
        val immutableCopyOfBuffer : IndexedSeq[XmlEvent] = buffer.toIndexedSeq
        Result(Cont(reader, nextProgress), immutableCopyOfBuffer) <~: log
      }
    }

    private def nextEvents() : List[XmlEvent] = {
      implicit val log = enter("nextEvents")()
      val eventType = reader.getEventType()
      ~s"MATCH reader.getEventType=$eventType"
      eventType match {
//        case XMLStreamConstants.ATTRIBUTE => {
//          val i = reader.getAttributeCount - 1
//          println("getAttributeCount=" + reader.getAttributeCount)
//          println("getAttributeLocalName=" + reader.getAttributeName(i))
//          println("getAttributeCount=" + reader.getAttributeValue(i))
//          nextEvents()
//        }
//        case XMLStreamConstants.NAMESPACE => {
//          val i = reader.getNamespaceCount - 1
//          println("getNamespaceCount=" + reader.getNamespaceCount)
//          println("getNamespacePrefix=" + reader.getNamespacePrefix(i))
//          println("getNamespaceURI=" + reader.getNamespaceURI(i))
//          nextEvents()
//        }
        case XMLStreamConstants.START_DOCUMENT => {
          ~s"CASE START_DOCUMENT"
          val retv = List(StartXmlDocumentEvent(
            reader.getEncoding,
            reader.getVersion,
            reader.isStandalone,
            reader.getCharacterEncodingScheme,
            getLocatorFromReader
          ))
          ~"reader.next()"
          reader.next()
          retv <~: log
        }
        case XMLStreamConstants.END_DOCUMENT => {
          ~s"CASE END_DOCUMENT"
          val retv = List(EndXmlDocumentEvent(getLocatorFromReader))
          retv <~: log
        }
        case XMLStreamConstants.START_ELEMENT => {
          ~s"CASE START_ELEMENT"
          val (qName, attributes, prefixes) = getElementInfoFromReader()
          val locator = getLocatorFromReader
          ~s"reader.next()"
          reader.next()
          val (value, peekQueue) = peekParseElementValue()
          ~s"Peek parsed value=$value peekQueue=$peekQueue"
          val retv = peekQueue :::
            StartXmlElementEvent(XmlElement(qName, value, attributes, prefixes, locator), locator) :: Nil
          retv <~: log
        }
        case XMLStreamConstants.END_ELEMENT => {
          ~s"CASE END_ELEMENT"
          val retv = List(EndXmlElementEvent(getElementQNameFromReader, getLocatorFromReader))
          ~s"reader.next()"
          reader.next()
          retv <~: log
        }
        case XMLStreamConstants.CHARACTERS => {
          ~s"CASE CHARACTERS"
          val retv = List(AddXmlTextEvent(reader.getText(), getLocatorFromReader))
          ~s"reader.next()"
          reader.next()
          retv <~: log
        }
        case XMLStreamConstants.CDATA => {
          ~s"CASE CDATA"
          val retv = List(AddXmlTextEvent(reader.getText(), getLocatorFromReader))
          ~s"reader.next()"
          reader.next()
          retv <~: log
        }
        case XMLStreamConstants.COMMENT => {
          ~s"CASE COMMENT"
          val retv = List(AddXmlCommentEvent(reader.getText(), getLocatorFromReader))
          ~s"reader.next()"
          reader.next()
          retv <~: log
        }
        case _ =>
          log warn s"Unhandled event: $eventType"
          ~s"reader.next()"
          reader.next()
          ~s"Recursing nextEvents"
          nextEvents()
      }
    }

    private def getElementQNameFromReader : XsdQName = {
      val prefix = newXsdNCName(reader.getPrefix())
      val uri = newXsdAnyURI(reader.getNamespaceURI())
      val localName = new XsdNCName(reader.getLocalName())
      new XsdQName(prefix, uri, localName)
    }

    private def getElementInfoFromReader() : (XsdQName, Seq[(XsdQName, String)], Seq[(XsdNCName, XsdAnyURI)]) = {
      implicit val log = enter(s"getElementInfoFromReader")()

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

      (qName, attributes, prefixes) <~: log
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
      implicit val log = enter("peekParseElementValue") { "peekQueue#" -> peekQueue :: Nil }
      val events = nextEvents()
      ~s"MATCH nextEvents=$events"
      events match {
        case List(e:AddXmlTextEvent) => {
          ~"CASE List(e:AddXmlTextEvent)"
          ~s"Got an events list with exactly one AddXmlTextEvent=$e"
          peekParseElementValue(e :: peekQueue)
        }
        case List(e:EndXmlElementEvent) => {
          ~"CASE List(e:EndXmlElementEvent)"
          ~s"Got an events list with exactly one EndXmlElementEvent=$e"
          if(peekQueue.nonEmpty) {
            val result = peekQueue.foldLeft(new StringBuilder) {
              (s,event) =>
                event match {
                  case AddXmlTextEvent(text,_) => s.append(text)
                }
            }
            (Some(result.toString), events) <~: log
          } else {
            (None, events) <~: log
          }
        }
        case _ => {
          ~"CASE _"
          (None, events ::: peekQueue) <~: log
        }
      }
    }
  }
}