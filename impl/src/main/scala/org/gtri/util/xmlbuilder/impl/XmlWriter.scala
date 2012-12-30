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

import org.gtri.util.iteratee.api._
import org.gtri.util.iteratee.impl.Iteratees._
import org.gtri.util.xsddatatypes.XsdQName.NamespaceURIToPrefixResolver
import org.gtri.util.xsddatatypes.{XsdNCName, XsdAnyURI}
import annotation.tailrec
import org.gtri.util.xmlbuilder.api.XmlEvent
import org.gtri.util.xmlbuilder.api.XmlFactory.XMLStreamWriterFactory
import javax.xml.stream.XMLStreamWriter
import javax.xml.XMLConstants
import org.gtri.util.iteratee.impl.ImmutableBufferConversions._
import org.gtri.util.iteratee.impl.Iteratees.Result

/**
 * Created with IntelliJ IDEA.
 * User: Lance
 * Date: 11/12/12
 * Time: 9:05 PM
 * To change this template use File | Settings | File Templates.
 */
class XmlWriter(factory : XMLStreamWriterFactory, issueHandlingCode : IssueHandlingCode = IssueHandlingCode.NORMAL) extends Iteratee[XmlEvent, Unit] {
  def initialState =  Cont(factory.create(), Nil)

  case class Cont(writer : XMLStreamWriter, stack : List[XmlElement]) extends SingleItemCont[XmlEvent, Unit] {

    def apply(xmlEvent: XmlEvent) = {
      val (newStack, issues) = writeXmlEvent(xmlEvent, stack)
      Result(next = Cont(writer, newStack), issues = issues)
    }

    def endOfInput() = {
      writer.flush()
      writer.close()
      Success()
    }

    private def writeXmlEvent(xmlEvent : XmlEvent, stack : List[XmlElement]) : (List[XmlElement], List[Issue]) = {
      xmlEvent match {
        case e:StartXmlDocumentEvent => {
          writer.writeStartDocument()
          (stack, Nil)
        }
        case e:EndXmlDocumentEvent => {
          writer.writeEndDocument()
          (stack, Nil)
        }
        case e:AddXmlCommentEvent => {
          writer.writeComment(e.comment)
          (stack, Nil)
        }
        case e:AddXmlElementEvent => {
          val newStack = e.element :: stack
          // Start element
          val qName = e.element.qName
          val localName = qName.getLocalName.toString
          val nsURI = qName.getNamespaceURI.toString
          val optionPrefix = Option(qName.resolvePrefix(getNamespaceURIToPrefixResolver(newStack))).map { _.toString }
          val prefix = optionPrefix.getOrElse { XMLConstants.DEFAULT_NS_PREFIX }
          writer.writeStartElement(prefix, localName, nsURI)

          // Write namespace prefixes
          for((namespacePrefix, namespaceURI) <- e.element.prefixToNamespaceURIMap) {
            // Skip the prefix for the element
            if(prefix != namespacePrefix.toString) {
              writer.writeNamespace(namespacePrefix.toString, namespaceURI.toString)
            }
          }

          // Write attributes
          {
            // Note: order of attributes is meaningless however, to achieve a stable output, attributes are sorted
            val sortedAttributes = e.element.attributes.keySet.toList.sortWith( _.getLocalName.toString < _.getLocalName.toString)
            for(qName <- sortedAttributes) {
              val localName = qName.getLocalName.toString
              val nsURI = qName.getNamespaceURI.toString
              val optionPrefix = Option(qName.resolvePrefix(getNamespaceURIToPrefixResolver(newStack))).map { _.toString }
              val prefix = optionPrefix.getOrElse { XMLConstants.DEFAULT_NS_PREFIX }
              val value = e.element.attributes(qName)
              writer.writeAttribute(prefix, nsURI, localName, value)
            }
          }

          // Write value (if any)
          val value = e.element.value
          if(value.isDefined) {
            writer.writeCharacters(value.get)
          }
          (newStack, Nil)
        }
        case e:EndXmlElementEvent => {
          writer.writeEndElement()
          (stack, Nil)
        }
        case e:AddXmlTextEvent => {
          writer.writeCharacters(e.text)
          (stack, Nil)
        }
        case e:XmlEvent => {
          val issue = Issues.inputWarning("Ignoring invalid XmlEvent '" + e.toString + "'", e.locator)
          (stack, List(issue))
        }
      }
    }

    private def getNamespaceURIToPrefixResolver(stack : List[XmlElement]) = new NamespaceURIToPrefixResolver {
      def isValidPrefixForNamespaceURI(prefix: XsdNCName, namespaceURI: XsdAnyURI) = {
        doIsValidPrefixForNamespaceURI(stack, prefix, namespaceURI)
      }

      def getPrefixForNamespaceURI(namespaceURI: XsdAnyURI) : XsdNCName = {
        doGetPrefixForNamespaceURI(stack, namespaceURI)
      }
    }

    @tailrec
    private def doIsValidPrefixForNamespaceURI(stack : List[XmlElement], prefix: XsdNCName, namespaceURI: XsdAnyURI) : Boolean = {
      if(stack.isEmpty) {
        false
      } else {
        val head :: tail = stack
        if(head.isValidPrefixForNamespaceURI(prefix, namespaceURI)) {
          true
        } else {
          doIsValidPrefixForNamespaceURI(tail, prefix, namespaceURI)
        }
      }
    }

    @tailrec
    private def doGetPrefixForNamespaceURI(stack : List[XmlElement], namespaceURI: XsdAnyURI) : XsdNCName = {
      if(stack.isEmpty) {
        null
      } else {
        val head :: tail = stack
        val result = head.getPrefixForNamespaceURI(namespaceURI)
        if(result != null) {
          result
        } else {
          doGetPrefixForNamespaceURI(tail, namespaceURI)
        }
      }
    }
  }
}