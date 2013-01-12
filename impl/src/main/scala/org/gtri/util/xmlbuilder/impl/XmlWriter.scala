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

import annotation.tailrec
import javax.xml.stream.XMLStreamWriter
import javax.xml.XMLConstants
import org.gtri.util.scala.exelog._
import org.gtri.util.issue.api.{Issue, IssueHandlingStrategy}
import org.gtri.util.issue.Issues
import org.gtri.util.iteratee.impl.ImmutableBufferConversions._
import org.gtri.util.iteratee.api._
import org.gtri.util.iteratee.impl.iteratees._
import org.gtri.util.xsddatatypes.XsdQName.NamespaceURIToPrefixResolver
import org.gtri.util.xsddatatypes.{XsdNCName, XsdAnyURI}
import org.gtri.util.xmlbuilder.api.XmlEvent
import org.gtri.util.xmlbuilder.api.XmlFactory.XMLStreamWriterFactory
import org.gtri.util.xmlbuilder.impl.events._
import sideeffects._

object XmlWriter {
  implicit val classlog = ClassLog(classOf[XmlWriter])
}
class XmlWriter(factory : XMLStreamWriterFactory, issueHandlingStrategy : IssueHandlingStrategy) extends Iteratee[XmlEvent, Unit] {
  import XmlWriter._

  def initialState =  {
    implicit val log = enter("initialState")()
    val retv : Iteratee.State[XmlEvent, Unit] =
      try {
        +"Trying to create writer"
        val result = factory.create()
        +"Created writer"
        Cont(factory.create(), Nil)
      } catch {
        case e : Exception =>
          log.fatal("Failed to create writer",e)
          val msg : String = e.getMessage
          val issue : Issue = Issues.INSTANCE.fatalError(msg)
          Failure(issues = Chunk(issue))
      }
    retv <~: log
  }

  object Cont {
    implicit val classlog = ClassLog(classOf[Cont])
  }
  case class Cont(writer : XMLStreamWriter, stack : List[XmlElement]) extends SingleItemCont[XmlEvent, Unit] {
    import Cont._

    def apply(xmlEvent: XmlEvent) = {
      implicit val log = enter("apply") { "xmlEvent" -> xmlEvent :: Nil }
      +"Writing XmlEvent & appending to active element stack"
      val (newStack, issues) = writeXmlEvent(xmlEvent, stack)
      Result(next = Cont(writer, newStack), issues = issues) <~: log
    }

    def endOfInput() = {
      implicit val log = enter("endOfInput")()
      +"Flush and close writer, return success"
      writer.flush()
      writer.close()
      val retv : Iteratee.State.Result[XmlEvent,Unit] = Success()
      retv <~: log
    }

    private def writeXmlEvent(xmlEvent : XmlEvent, stack : List[XmlElement]) : (List[XmlElement], List[Issue]) = {
      implicit val log = enter("writeXmlEvent") { "xmlEvent" -> xmlEvent :: "stack" -> stack :: Nil}
      ~"Match event"
      val retv =
        xmlEvent match {
          case e:StartXmlDocumentEvent => {
            ~"Write start document, no change to stack"
            writer.writeStartDocument()
            (stack, Nil)
          }
          case e:EndXmlDocumentEvent => {
            ~"Write end document, no change to stack"
            writer.writeEndDocument()
            (stack, Nil)
          }
          case e:AddXmlCommentEvent => {
            ~"Write comment, no change to stack"
            writer.writeComment(e.comment)
            (stack, Nil)
          }
          case e:StartXmlElementEvent => {
            ~"Write start element, append element to stack"
            val newStack = e.element :: stack
            // Start element
            val qName = e.element.qName
            val localName = qName.getLocalName.toString
            val nsURI = qName.getNamespaceURI.toString
            val optionPrefix = Option(qName.resolvePrefix(getNamespaceURIToPrefixResolver(newStack))).map { _.toString }
            val prefix = optionPrefix.getOrElse { XMLConstants.DEFAULT_NS_PREFIX }
            ~s"writer.writeStartElement($prefix, $localName, $nsURI)"
            writer.writeStartElement(prefix, localName, nsURI)

            ~"Write namespace prefixes"
            for((namespacePrefix, namespaceURI) <- e.element.orderedPrefixes) {
              // Skip the prefix for the element
              if(prefix != namespacePrefix.toString) {
                val namespacePrefixString = namespacePrefix.toString
                val namespaceURIString = namespaceURI.toString
                ~s"writer.writeNamespace(namespacePrefix=$namespacePrefixString, namespaceURI=$namespaceURIString)"
                writer.writeNamespace(namespacePrefixString, namespaceURIString)
              }
            }

            ~"Write attributes"
            for((qName,value) <- e.element.orderedAttributes) {
              val localName = qName.getLocalName.toString
              val nsURI = qName.getNamespaceURI.toString
              val optionPrefix = Option(qName.resolvePrefix(getNamespaceURIToPrefixResolver(newStack))).map { _.toString }
              val prefix = optionPrefix.getOrElse { XMLConstants.DEFAULT_NS_PREFIX }
              ~s"writer.writeAttribute(prefix=$prefix, nsURI=$nsURI, localName=$localName, value=$value)"
              writer.writeAttribute(prefix, nsURI, localName, value)
            }

            ~"Write value (if any)"
            val value = e.element.value
            if(value.isDefined) {
              val v = value.get
              ~s"writer.writeCharacters($v)"
              writer.writeCharacters(v)
            }
            (newStack, Nil)
          }
          case e:EndXmlElementEvent => {
            ~"Write end element, no change to stack"
            writer.writeEndElement()
            (stack, Nil)
          }
          case e:AddXmlTextEvent => {
            ~"Write characters, no change to stack"
            val text = e.text
            ~s"writer.writeCharacters($text)"
            writer.writeCharacters(e.text)
            (stack, Nil)
          }
          case e:XmlEvent => {
            val error = Issues.INSTANCE.recoverableError(s"Invalid XmlEvent: $e")
            if(issueHandlingStrategy.canContinue(error)) {
              val warn = Issues.INSTANCE.warning(s"Ignoring invalid XmlEvent: $e")
              (stack, error :: warn :: Nil)
            }
            (stack, error :: Nil)
          }
        }
      retv <~: log
    }

    private def getNamespaceURIToPrefixResolver(stack : List[XmlElement]) = new NamespaceURIToPrefixResolver {
      def isValidPrefixForNamespaceURI(prefix: XsdNCName, namespaceURI: XsdAnyURI) = {
        implicit val log = enter("isValidPrefixForNamespaceURIToPrefixResolver") { "stack" -> stack :: "prefix" -> prefix :: "namespaceURI" -> namespaceURI :: Nil }
        doIsValidPrefixForNamespaceURI(stack, prefix, namespaceURI) <~: log
      }

      def getPrefixForNamespaceURI(namespaceURI: XsdAnyURI) : XsdNCName = {
        implicit val log = enter("getPrefixForNamespaceURI") { "stack" -> stack :: "namespaceURI" -> namespaceURI :: Nil }
        doGetPrefixForNamespaceURI(stack, namespaceURI) <~: log
      }
    }

    @tailrec
    private def doIsValidPrefixForNamespaceURI(stack : List[XmlElement], prefix: XsdNCName, namespaceURI: XsdAnyURI) : Boolean = {
      implicit val log = enter("doIsValidPrefixForNamespaceURI") { "prefix" -> prefix :: "namespaceURI" -> namespaceURI :: Nil }
      ~"Stack empty?"
      if(stack.isEmpty) {
        ~"Yes"
        false <~: log
      } else {
        ~"No, extract head/tail from stack"
        val head :: tail = stack
        ~"Does head contain prefix=namespaceURI?"
        if(head.isValidPrefixForNamespaceURI(prefix, namespaceURI)) {
          ~"Yes"
          true <~: log
        } else {
          ~"No, recurse on tail"
          doIsValidPrefixForNamespaceURI(tail, prefix, namespaceURI)
        }
      }
    }

    @tailrec
    private def doGetPrefixForNamespaceURI(stack : List[XmlElement], namespaceURI: XsdAnyURI) : XsdNCName = {
      implicit val log = enter("doGetPrefixForNamespaceURI") { "stack" -> stack :: "namespaceURI" -> namespaceURI :: Nil }
      ~"Stack empty?"
      if(stack.isEmpty) {
        ~"Yes"
        null <~: log
      } else {
        ~"No, extract head/tail from stack"
        val head :: tail = stack
        ~"Does head have prefix for namespaceURI?"
        val result = head.getPrefixForNamespaceURI(namespaceURI)
        if(result != null) {
          ~"Yes"
          result <~: log
        } else {
          ~"No, recurse on tail"
          doGetPrefixForNamespaceURI(tail, namespaceURI)
        }
      }
    }
  }
}