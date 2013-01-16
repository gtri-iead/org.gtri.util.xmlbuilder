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
import org.gtri.util.scala.exelog.noop._
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

object XmlWriter {
  implicit val thisclass =  classOf[XmlWriter]
  implicit val log =        Logger.getLog(thisclass)
}

class XmlWriter(
  factory :                 XMLStreamWriterFactory,
  issueHandlingStrategy :   IssueHandlingStrategy
) extends Iteratee[XmlEvent, Unit] {

  import XmlWriter._

  def initialState =  {
    log.block("initialState"){
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
    }
  }

  case class Cont(writer : XMLStreamWriter, stack : List[XmlElement]) extends SingleItemCont[XmlEvent, Unit] {

    def apply(xmlEvent: XmlEvent) = {
      log.block("apply", Seq("xmlEvent" -> xmlEvent)){
        +"Writing XmlEvent & appending to active element stack"
        val (newStack, issues) = writeXmlEvent(xmlEvent, stack)
        Result(next = Cont(writer, newStack), issues = issues)
      }
    }

    def endOfInput() = {
      log.block("endOfInput") {
        +"Flush and close writer, return success"
        writer.flush()
        writer.close()
        Success()
      }
    }

    private def writeXmlEvent(xmlEvent : XmlEvent, stack : List[XmlElement]) : (List[XmlElement], List[Issue]) = {
      log.block("writeXmlEvent", Seq("xmlEvent" -> xmlEvent, "stack" -> stack)) {
        ~"Match event"
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

            ~"Start an element"
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
      }
    }

    private def getNamespaceURIToPrefixResolver(stack : List[XmlElement]) = new NamespaceURIToPrefixResolver {
      def isValidPrefixForNamespaceURI(prefix: XsdNCName, namespaceURI: XsdAnyURI) = {
        log.block("isValidPrefixForNamespaceURIToPrefixResolver", Seq("stack" -> stack, "prefix" -> prefix, "namespaceURI" -> namespaceURI)) {
          doIsValidPrefixForNamespaceURI(stack, prefix, namespaceURI)
        }
      }

      def getPrefixForNamespaceURI(namespaceURI: XsdAnyURI) : XsdNCName = {
        log.block("getPrefixForNamespaceURI",Seq("stack" -> stack,"namespaceURI" -> namespaceURI)) {
          doGetPrefixForNamespaceURI(stack, namespaceURI)
        }
      }
    }

    @tailrec
    private def doIsValidPrefixForNamespaceURI(stack : List[XmlElement], prefix: XsdNCName, namespaceURI: XsdAnyURI) : Boolean = {
      log.begin("doIsValidPrefixForNamespaceURI", Seq("prefix" -> prefix, "namespaceURI" -> namespaceURI))
      ~"Stack empty?"
      if(stack.isEmpty) {
        ~"Yes, didn't find prefix for namespace"
        val retv = false
        log.end("doIsValidPrefixForNamespaceURI", retv)
        retv
      } else {
        ~"No, extract head/tail from stack"
        val head :: tail = stack
        ~"Does head contain prefix=namespaceURI?"
        if(head.isValidPrefixForNamespaceURI(prefix, namespaceURI)) {
          ~"Yes, found prefix for namespace"
          val retv = true
          log.end("doIsValidPrefixForNamespaceURI", retv)
          retv
        } else {
          ~"No, recurse on tail"
          doIsValidPrefixForNamespaceURI(tail, prefix, namespaceURI)
        }
      }
    }

    @tailrec
    private def doGetPrefixForNamespaceURI(stack : List[XmlElement], namespaceURI: XsdAnyURI) : XsdNCName = {
      log.begin("doGetPrefixForNamespaceURI", Seq("stack" -> stack, "namespaceURI" -> namespaceURI))
        ~"Stack empty?"
        if(stack.isEmpty) {
          ~"Yes, didn't find prefix for namespace"
          val retv = null
          log.end("doGetPrefixForNamespaceURI", retv)
          retv
        } else {
          ~"No, extract head/tail from stack"
          val head :: tail = stack
          ~"Does head have prefix for namespaceURI?"
          val result = head.getPrefixForNamespaceURI(namespaceURI)
          if(result != null) {
            ~"Yes, found prefix for namespace"
            val retv = result
            log.end("doGetPrefixForNamespaceURI", retv)
            retv
          } else {
            ~"No, recurse on tail"
            doGetPrefixForNamespaceURI(tail, namespaceURI)
          }
        }
    }
  }
}