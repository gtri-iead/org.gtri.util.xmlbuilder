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
package org.gtri.util.xmlbuilder.impl.events

import org.gtri.util.scala.exelog.noop._
import org.gtri.util.xmlbuilder.impl.XmlElement
import org.gtri.util.issue.api.DiagnosticLocator
import org.gtri.util.xmlbuilder.api.{XmlContract, XmlEvent}
import com.google.common.collect.ImmutableMap
import org.gtri.util.xsddatatypes.{XsdQName, XsdAnyURI, XsdNCName}

object StartXmlElementEvent {
  implicit val thisclass = classOf[StartXmlElementEvent]
  implicit val log = Logger.getLog(thisclass)
}
case class StartXmlElementEvent(element : XmlElement, locator : DiagnosticLocator) extends XmlEvent {
  import StartXmlElementEvent._

  def pushTo(contract: XmlContract) {
    log.block("pushTo", Seq("contract" -> contract)) {
      +"Pushing StartXmlElementEvent to XmlContract"
      ~"Build prefixToNamespaceURIMap"
      val prefixToNamespaceURIMap = {
        val builder = ImmutableMap.builder[XsdNCName, XsdAnyURI]()
        for ((prefix, namespaceUri) <- element.prefixToNamespaceURIMap) {
          builder.put(prefix, namespaceUri)
        }
        builder.build()
      }
      ~"Build attributes"
      val attributes = {
        val builder = ImmutableMap.builder[XsdQName, String]()
        for ((name, value) <- element.attributesMap) {
          builder.put(name, value)
        }
        builder.build()
      }
      val qName = element.qName
      val value = element.value.orNull
      ~s"contract.addXmlElement($qName, $value, $attributes, $prefixToNamespaceURIMap)"
      contract.addXmlElement(qName, value, attributes, prefixToNamespaceURIMap)
    }
  }
}

