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
import org.apache.xml.serialize.XMLSerializer
import org.xml.sax.ext.{LexicalHandler, Attributes2Impl}
import org.gtri.util.xsddatatypes.XsdQName.NamespaceURIToPrefixResolver
import org.gtri.util.xsddatatypes.{XsdNCName, XsdAnyURI}
import annotation.tailrec
import org.gtri.util.xmlbuilder.api

/**
 * Created with IntelliJ IDEA.
 * User: Lance
 * Date: 11/12/12
 * Time: 9:05 PM
 * To change this template use File | Settings | File Templates.
 */
class XmlWriter(serializer : XMLSerializer) extends Consumer[XmlEvent] {
  private val contentHandler = serializer.asContentHandler
  private val lexicalHandler : LexicalHandler = serializer
  /*
FileOutputStream fos = new FileOutputStream(filename);
// XERCES 1 or 2 additionnal classes.
OutputFormat of = new OutputFormat("XML","ISO-8859-1",true);
of.setIndent(1);
of.setIndenting(true);
of.setDoctype(null,"users.dtd");
XMLSerializer serializer = new XMLSerializer(fos,of);
// SAX2.0 ContentHandler.
ContentHandler hd = serializer.asContentHandler();
hd.startDocument();
// Processing instruction sample.
//hd.processingInstruction("xml-stylesheet","type=\"text/xsl\" href=\"users.xsl\"");
// USER attributes.
AttributesImpl atts = new AttributesImpl();
// USERS tag.
hd.startElement("","","USERS",atts);
// USER tags.
String[] id = {"PWD122","MX787","A4Q45"};
String[] type = {"customer","manager","employee"};
String[] desc = {"Tim@Home","Jack&Moud","John D'oé"};
for (int i=0;i<id.length;i++)
{
  atts.clear();
  atts.addAttribute("","","ID","CDATA",id[i]);
  atts.addAttribute("","","TYPE","CDATA",type[i]);
  hd.startElement("","","USER",atts);
  hd.characters(desc[i].toCharArray(),0,desc[i].length());
  hd.endElement("","","USER");
}
hd.endElement("","","USERS");
hd.endDocument();
fos.close();
   */
  def iteratee = {
    def step(issues : List[Issue], stack : List[XmlElement]) : (Input[XmlEvent]) => Iteratee[XmlEvent, Unit] = {
      case El(chunk, moreIssues) =>
        val (newIssues, newStack) = chunk.foldLeft((issues, stack)) { writeXmlEvent(_,_) }
        Cont(step(moreIssues ::: newIssues, newStack))
      case EOF() =>
        Success((), issues, EOF[XmlEvent])
      case Empty() =>
        Cont(step(issues, stack))
    }

    def getNamespaceURIToPrefixMap(stack : List[XmlElement]) = new NamespaceURIToPrefixResolver {
      def isValidPrefixForNamespaceURI(prefix: XsdNCName, namespaceURI: XsdAnyURI) = {
        doIsValidPrefixForNamespaceURI(stack, prefix, namespaceURI)
      }

      def getPrefixForNamespaceURI(namespaceURI: XsdAnyURI) : XsdNCName = {
        doGetPrefixForNamespaceURI(stack, namespaceURI)
      }
    }


    def writeXmlEvent(tuple : (List[Issue], List[XmlElement]), xmlEvent: XmlEvent) : (List[Issue], List[XmlElement]) = {
      val (issues, stack) = tuple
      xmlEvent match {
        case e:StartXmlDocumentEvent => {
          contentHandler.startDocument()
          tuple
        }
        case e:EndXmlDocumentEvent => {
          contentHandler.endDocument()
          tuple
        }
        case e:AddXmlCommentEvent => {
          lexicalHandler.comment(e.comment.toArray,0,e.comment.length)
          tuple
        }
        case e:AddXmlElementEvent => {
          for((prefix, namespaceURI) <- e.element.prefixToNamespaceURIMap) {
            contentHandler.startPrefixMapping(prefix.toString, namespaceURI.toString)
          }

          val saxAttributes = new Attributes2Impl
          for((qName, value) <- e.element.attributes) {
            saxAttributes.addAttribute(
              qName.getNamespaceURI.toString,
              qName.getLocalName.toString,
              qName.toStringWithPrefix(getNamespaceURIToPrefixMap(stack)),
              null,
              value
            )
          }
          val qName = e.element.qName
          contentHandler.startElement(
            qName.getNamespaceURI.toString,
            qName.getLocalName.toString,
            qName.toStringWithPrefix(getNamespaceURIToPrefixMap(stack)),
            saxAttributes
          )

          val value = e.element.value
          if(value.isDefined) {
            val strValue = value.get
            contentHandler.characters(strValue.toArray, 0, strValue.length)
          }
          (issues, e.element :: stack)
        }
        case e:EndXmlElementEvent => {
          val qName = e.qName
          contentHandler.endElement(
            qName.getNamespaceURI.toString,
            qName.getLocalName.toString,
            qName.toStringWithPrefix(getNamespaceURIToPrefixMap(stack))
          )
          (issues, stack.tail)
        }
        case e:AddXmlTextEvent => {
          contentHandler.characters(e.text.toArray, 0, e.text.length)
          tuple
        }
        case e:XmlEvent => {
          val issue = Warning("Ignoring invalid XmlEvent '" + e.toString + "'", e.locator)
          (issue :: issues, stack)
        }
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

    Cont(step(Nil, Nil))
  }
}
