package org.gtri.util.xmlbuilder.impl

import org.gtri.util.iteratee.impl._
import org.gtri.util.iteratee.impl.Iteratee._
import annotation.tailrec
import collection.mutable.ArrayBuffer
import collection.mutable.Queue
import javax.xml.stream.{XMLStreamReader, XMLStreamConstants}
import org.gtri.util.xsddatatypes._
import org.gtri.util.xmlbuilder.api

/**
 * Created with IntelliJ IDEA.
 * User: Lance
 * Date: 11/4/12
 * Time: 6:49 PM
 * To change this template use File | Settings | File Templates.
 */
class XmlReader(val reader : XMLStreamReader, val chunkSize : Int = 8) extends Producer[XmlEvent] {

  def enumerator : Enumerator[XmlEvent] = {
    new Enumerator[XmlEvent] {

      private val outputBuffer = new ArrayBuffer[XmlEvent](chunkSize)
      private val inputQueue = new Queue[XmlEvent]()

      def enumerate[V](i : Iteratee[XmlEvent,V]) : Iteratee[XmlEvent,V] = {
        doEnumerate(next, i)
      }

      @tailrec
      private def next : Option[XmlEvent] = {
        if(inputQueue.nonEmpty) {
          Some(inputQueue.dequeue)
        } else {
          if(reader.hasNext) {
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
//                val value = peekParseElementValue(next, Nil)
//                Some(AddXmlElementEvent(getElementFromReader(value), getLocatorFromReader))
                workAroundTailRecIssue
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
              case _ =>
                next
            }
          } else {
            None
          }
        }
      }

      private def workAroundTailRecIssue : Option[XmlEvent] = {
        Some(AddXmlElementEvent(fetchElementFromReader, getLocatorFromReader))
      }

      @tailrec
      private def doEnumerate[V](nextEvent : Option[XmlEvent], i : Iteratee[XmlEvent,V]) : Iteratee[XmlEvent,V] = {
        i match {
          case i@Done(_,_) => i
          case Cont(k) =>
            if(nextEvent.isDefined) {
              if(outputBuffer.size < chunkSize) {
                outputBuffer += nextEvent.get
                doEnumerate[V](next, i)
              } else {
                val nextI = flushOutputBuffer(k)
                doEnumerate[V](next, nextI)
              }
            } else {
              i
            }
        }
      }

      private def flushOutputBuffer[V](k: (Input[XmlEvent]) => Iteratee[XmlEvent, V]) : Iteratee[XmlEvent, V] = {
        val buffer = outputBuffer.toList
        outputBuffer.clear
        k(El(buffer,Nil))
      }

      private def getElementQNameFromReader : XsdQName = {
        val prefix = newXsdNCName(reader.getPrefix())
        val uri = newXsdAnyURI(reader.getNamespaceURI())
        val localName = new XsdNCName(reader.getLocalName())
        new XsdQName(prefix, uri, localName)
      }

      private def fetchElementFromReader() : XmlElement = {
        val qName = getElementQNameFromReader

        val attributes = for(i <- 0 until reader.getAttributeCount())
        yield {
          val prefix = newXsdNCName(reader.getPrefix())
          val uri = newXsdAnyURI(reader.getAttributeNamespace(i))
          val localName = new XsdNCName(reader.getAttributeLocalName(i))
          val qName = new XsdQName(prefix, uri, localName)
          val value = reader.getAttributeValue(i)
          qName -> value
        }

        val prefixes =
          for(i <- 0 until reader.getNamespaceCount())
          yield {
            val prefix = newXsdNCName(reader.getNamespacePrefix(i))
            val uri = newXsdAnyURI(reader.getNamespaceURI(i))
            prefix -> uri
          }

        // Note: this must come after all the other element parameters are extracted from reader since this call will change the reader's state
        val value = peekParseElementValue(next, Nil)

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
      private def getLocatorFromReader : XmlLocator = {
        val l = reader.getLocation()
        XmlLocator(l.getCharacterOffset, l.getColumnNumber, l.getLineNumber, l.getPublicId, l.getSystemId)
      }
      
      // Peek at the next few XmlEvents - if it is a string of text events followed by an end event then compress
      // the sequence by extracting the combined "value" of the text events and throw away the individual
      // text events
      @tailrec
      private def peekParseElementValue(nextEvent : Option[XmlEvent], textEvents : List[AddXmlTextEvent]) : Option[String] = {
        if(nextEvent.isDefined) {
          nextEvent.get match {
            case e:AddXmlTextEvent => {
              peekParseElementValue(next, e :: textEvents)
            }
            case e:EndXmlElementEvent => {
              inputQueue += e
              val result = textEvents.foldLeft(new StringBuilder) { (s,textEvent) => s.append(textEvent.text) }
              Some(result.toString)
            }
            case e:XmlEvent  => {
              textEvents.reverse foreach { inputQueue.enqueue(_) }
              inputQueue.enqueue(e)
              None
            }
          }
        } else {
          textEvents.reverse foreach { inputQueue.enqueue(_) }
          None
        }
      }
    }
  }
}

