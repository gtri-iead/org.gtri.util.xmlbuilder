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

import org.gtri.util.xsddatatypes._
import org.gtri.util.iteratee.api.ImmutableDiagnosticLocator
import org.gtri.util.xmlbuilder.api.XmlContract
import com.google.common.collect.ImmutableMap
import scala.collection.JavaConversions._
import org.gtri.util.xsddatatypes.XsdQName.NamespaceURIToPrefixResolver
import org.gtri.util.xmlbuilder.api.XmlEvent

/**
 * Created with IntelliJ IDEA.
 * User: Lance
 * Date: 11/10/12
 * Time: 4:45 AM
 * To change this template use File | Settings | File Templates.
 */
case class XmlLocator(charOffset : Int, columnNumber : Int, lineNumber : Int, publicId : String, systemId : String) extends ImmutableDiagnosticLocator {
  override def toString = {
    val s = new StringBuilder
    s.append('[')
    if (publicId != null) {
      s.append(publicId)
      s.append(' ')
    }
    if (systemId != null) {
      s.append(systemId)
      s.append(' ')
    }
    s.append(lineNumber)
    s.append(':')
    s.append(columnNumber)
    s.append(']')
    s.toString
  }
}

case class XmlElement(
                       qName : XsdQName,
                       value : Option[String],
                       attributes : Map[XsdQName, String],
                       prefixToNamespaceURIMap : Map[XsdNCName, XsdAnyURI]
                       ) extends NamespaceURIToPrefixResolver {
  lazy val namespaceURIToPrefixMap = prefixToNamespaceURIMap.map(_.swap)

  def isValidPrefixForNamespaceURI(prefix: XsdNCName, namespaceURI: XsdAnyURI) = {
    val optionNsURI = prefixToNamespaceURIMap.get(prefix)
    if(optionNsURI.isDefined) {
      optionNsURI.get == namespaceURI
    } else {
      false
    }
  }

  def getPrefixForNamespaceURI(namespaceURI: XsdAnyURI) = {
    namespaceURIToPrefixMap.get(namespaceURI).orNull
  }
}

//sealed trait XmlEvent {
//  def locator : XmlLocator
//  def pushTo(contract : XmlContract)
//}

case class StartXmlDocumentEvent(encoding : String, version : String, isStandAlone : Boolean, characterEncodingScheme : String, locator : XmlLocator) extends XmlEvent {
  def pushTo(p1: XmlContract) { }
}

case class EndXmlDocumentEvent(locator : XmlLocator) extends XmlEvent {
  def pushTo(p1: XmlContract) {}
}

case class AddXmlCommentEvent(comment : String, locator : XmlLocator) extends XmlEvent {
  def pushTo(contract: XmlContract) {
    contract.addXmlComment(comment)
  }
}

case class AddXmlElementEvent(element : XmlElement, locator : XmlLocator) extends XmlEvent {
  def pushTo(contract: XmlContract) {
    val prefixToNamespaceURIMap = {
      val builder = ImmutableMap.builder[XsdNCName, XsdAnyURI]()
      for ((prefix, namespaceUri) <- element.prefixToNamespaceURIMap) {
        builder.put(prefix, namespaceUri)
      }
      builder.build()
    }
    val attributes = {
      val builder = ImmutableMap.builder[XsdQName, String]()
      for ((name, value) <- element.attributes) {
        builder.put(name, value)
      }
      builder.build()
    }
    contract.addXmlElement(element.qName, element.value.orNull, attributes, prefixToNamespaceURIMap)

  }
}
case class EndXmlElementEvent(qName : XsdQName, locator : XmlLocator) extends XmlEvent {
  def pushTo(contract: XmlContract) {
    contract.endXmlElement()
  }

}
case class AddXmlTextEvent(text : String, locator : XmlLocator) extends XmlEvent {
  def pushTo(contract: XmlContract) {
    contract.addXmlText(text)
  }

}
