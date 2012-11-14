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

import org.gtri.util.iteratee.impl.Consumer
import org.gtri.util.iteratee.impl.Iteratee._
import org.gtri.util.iteratee.impl.Issues.Warning
import org.gtri.util.iteratee.api.Issue
import org.gtri.util.xsddatatypes.XsdQName.NamespaceURIToPrefixResolver
import org.gtri.util.xsddatatypes.{XsdNCName, XsdAnyURI}
import annotation.tailrec
import org.gtri.util.xmlbuilder.api.XmlEvent
import org.gtri.util.xmlbuilder.api.XmlFactory.XMLStreamWriterFactory
import javax.xml.stream.XMLStreamWriter
import javax.xml.XMLConstants

/**
 * Created with IntelliJ IDEA.
 * User: Lance
 * Date: 11/12/12
 * Time: 9:05 PM
 * To change this template use File | Settings | File Templates.
 */
class XmlWriter(factory : XMLStreamWriterFactory) extends Consumer[XmlEvent] {
  private case class State( writer : XMLStreamWriter, issues : List[Issue], stack : List[XmlElement])

  def iteratee = {
    def step(state : State) : (Input[XmlEvent]) => Iteratee[XmlEvent, Unit] = {
      case El(chunk, moreIssues) =>
        val newState = chunk.foldLeft(state) { writeXmlEvent(_,_) }
        if(moreIssues.nonEmpty) {
          Cont(step(newState.copy(issues = moreIssues ::: newState.issues)))
        } else {
          Cont(step(newState))
        }
      case EOF() =>
        Success((), state.issues, EOF[XmlEvent])
      case Empty() =>
        Cont(step(state))
    }


    def writeXmlEvent(state : State, xmlEvent: XmlEvent) : State = {

      xmlEvent match {
        case e:StartXmlDocumentEvent => {
          state.writer.writeStartDocument()
          state
        }
        case e:EndXmlDocumentEvent => {
          state.writer.writeEndDocument()
          state.writer.flush()
          state.writer.close()
          state.copy(writer = factory.create())
        }
        case e:AddXmlCommentEvent => {
          state.writer.writeComment(e.comment)
          state
        }
        case e:AddXmlElementEvent => {
          val newStack = e.element :: state.stack

          // Start element
          val qName = e.element.qName
          val localName = qName.getLocalName.toString
          val nsURI = qName.getNamespaceURI.toString
          val optionPrefix = Option(qName.resolvePrefix(getNamespaceURIToPrefixResolver(newStack))).map { _.toString }
          val prefix = optionPrefix.getOrElse { XMLConstants.DEFAULT_NS_PREFIX }
          state.writer.writeStartElement(prefix, localName, nsURI)

          // Write namespace prefixes
          for((namespacePrefix, namespaceURI) <- e.element.prefixToNamespaceURIMap) {
            // Skip the prefix for the element
            if(prefix != namespacePrefix.toString) {
              state.writer.writeNamespace(namespacePrefix.toString, namespaceURI.toString)
            }
          }

          // Write attributes
          {
            // Note: order of attributes is meaningless however, to achieve a stable output, attributes are sorted
            val sortedAttributes = e.element.attributes.keySet.toList.sortWith( _.getLocalName.toString < _.getLocalName.toString)
            for(qName <- sortedAttributes) {
              val localName = qName.getLocalName.toString
              val nsURI = qName.getNamespaceURI.toString
              val optionPrefix = Option(qName.resolvePrefix(getNamespaceURIToPrefixResolver(state.stack))).map { _.toString }
              val prefix = optionPrefix.getOrElse { XMLConstants.DEFAULT_NS_PREFIX }
              val value = e.element.attributes(qName)
              state.writer.writeAttribute(prefix, nsURI, localName, value)
            }
          }

          // Write value (if any)
          val value = e.element.value
          if(value.isDefined) {
            state.writer.writeCharacters(value.get)
          }

          state.copy(stack = newStack)
        }
        case e:EndXmlElementEvent => {
          state.writer.writeEndElement()
          state.copy(stack = state.stack.tail)
        }
        case e:AddXmlTextEvent => {
          state.writer.writeCharacters(e.text)
          state
        }
        case e:XmlEvent => {
          val issue = Warning("Ignoring invalid XmlEvent '" + e.toString + "'", e.locator)
          state.copy(issues = issue :: state.issues)
        }
      }
    }

    def getNamespaceURIToPrefixResolver(stack : List[XmlElement]) = new NamespaceURIToPrefixResolver {
      def isValidPrefixForNamespaceURI(prefix: XsdNCName, namespaceURI: XsdAnyURI) = {
        doIsValidPrefixForNamespaceURI(stack, prefix, namespaceURI)
      }

      def getPrefixForNamespaceURI(namespaceURI: XsdAnyURI) : XsdNCName = {
        doGetPrefixForNamespaceURI(stack, namespaceURI)
      }
    }

    @tailrec
    def doIsValidPrefixForNamespaceURI(stack : List[XmlElement], prefix: XsdNCName, namespaceURI: XsdAnyURI) : Boolean = {
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
    def doGetPrefixForNamespaceURI(stack : List[XmlElement], namespaceURI: XsdAnyURI) : XsdNCName = {
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

    Cont(step(State(factory.create(), Nil, Nil)))
  }
}
