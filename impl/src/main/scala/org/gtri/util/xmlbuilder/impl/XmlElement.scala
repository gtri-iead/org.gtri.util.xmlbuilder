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

import org.gtri.util.scala.exelog.noop._
import org.gtri.util.xsddatatypes.{XsdAnyURI, XsdNCName, XsdQName}
import org.gtri.util.xsddatatypes.XsdQName.NamespaceURIToPrefixResolver
import org.gtri.util.xmlbuilder.impl.XmlElement.Metadata
import org.gtri.util.issue.api.DiagnosticLocator


object XmlElement {
  implicit val thisclass =    classOf[XmlElement]
  implicit val log : Log =    Logger.getLog(thisclass)

  case class Metadata(
    optRawAttributesOrder:    Option[Seq[String]] = None, // Note: this includes xmlns:XXX
    optAttributesOrder:       Option[Seq[XsdQName]] = None, // This does *not* include xmlns
    optPrefixesOrder:         Option[Seq[XsdNCName]] = None,
    optLocator :              Option[DiagnosticLocator] = None
  )

  def apply(
    qName :            XsdQName,
    value :            Option[String],
    attributes :       Seq[(XsdQName, String)],
    prefixes :         Seq[(XsdNCName, XsdAnyURI)],
    optLocator :       Option[DiagnosticLocator] = None,
    optRawAttributes : Option[Seq[(String,String)]] = None
  ) = new XmlElement(
    qName,
    value,
    attributes.toMap,
    prefixes.toMap,
    Some(Metadata(
      optRawAttributesOrder =  optRawAttributes map { _ map { _._1 } },
      optAttributesOrder =     Some(attributes map { _._1 }),
      optPrefixesOrder =       Some(prefixes map { _._1 }),
      optLocator =             optLocator
    ))
  )
}
case class XmlElement(
  qName :                     XsdQName,
  value :                     Option[String],
  attributesMap :             Map[XsdQName, String],
  prefixToNamespaceURIMap :   Map[XsdNCName, XsdAnyURI],
  metadata :                  Option[Metadata] = None
)extends NamespaceURIToPrefixResolver {
  import XmlElement._

  lazy val orderedAttributes : Seq[(XsdQName, String)] = {
    log.block("orderAttributes") {
      +"Order attributes by attributesOrder metadata (if defined) or sort lexographically by name"
      if(metadata.isDefined && metadata.get.optAttributesOrder.isDefined) {
        ~"Sort by metadata attributesOrder"
        metadata.get.optAttributesOrder.get.map({
          qName => attributesMap.get(qName).map { value => (qName,value) }
        }).flatten
      } else {
        ~"Sort by name lexographically"
        attributesMap.toSeq.sortWith { (t1,t2) => t1._1.toString < t2._1.toString }
      }
    }
  }

  lazy val orderedPrefixes : Seq[(XsdNCName, XsdAnyURI)] = {
    log.block("orderedPrefixes") {
      +"Order prefixes by prefixOrder metadata (if defined) or sort lexographically name"
      if(metadata.isDefined && metadata.get.optPrefixesOrder.isDefined) {
        ~"Sort by metadata prefixOrder"
        metadata.get.optPrefixesOrder.get.map({
          prefix => prefixToNamespaceURIMap.get(prefix).map { uri => (prefix,uri) }
        }).flatten
      } else {
        ~"Sort by name lexographically"
        prefixToNamespaceURIMap.toSeq.sortWith { (t1,t2) => t1._1.toString < t2._1.toString }
      }
    }
  }

  lazy val namespaceURIToPrefixMap = prefixToNamespaceURIMap.map(_.swap)

  def isValidPrefixForNamespaceURI(prefix: XsdNCName, namespaceURI: XsdAnyURI) = {
    log.block("isValidPrefixForNamespaceURI", Seq("prefix" -> prefix, "namespaceURI" -> namespaceURI)) {
      +"TRUE if prefix is defined with the given namespaceURI otherwise FALSE"
      val optionNsURI = prefixToNamespaceURIMap.get(prefix)
      if(optionNsURI.isDefined) {
        optionNsURI.get == namespaceURI
      } else {
        false
      }
    }
  }

  def getPrefixForNamespaceURI(namespaceURI: XsdAnyURI) = {
    log.block("getPrefixForNamespaceURI", Seq("namespaceURI" -> namespaceURI)) {
      namespaceURIToPrefixMap.get(namespaceURI).orNull
    }
  }
}