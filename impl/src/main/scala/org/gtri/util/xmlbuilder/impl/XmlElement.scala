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

import org.gtri.util.scala.exelog.sideeffects._
import org.gtri.util.xsddatatypes.{XsdAnyURI, XsdNCName, XsdQName}
import org.gtri.util.xsddatatypes.XsdQName.NamespaceURIToPrefixResolver
import org.gtri.util.xmlbuilder.impl.XmlElement.Metadata
import org.gtri.util.issue.api.DiagnosticLocator


object XmlElement {
  implicit val classlog = ClassLog(classOf[XmlElement])
  case class Metadata(
                       rawAttributesOrder: Option[Seq[String]] = None,
                       attributesOrder: Option[Seq[XsdQName]] = None,
                       prefixesOrder: Option[Seq[XsdNCName]] = None,
                       locator : Option[DiagnosticLocator] = None
                       )

  def apply(
             qName : XsdQName,
             value : Option[String],
             attributes : Seq[(XsdQName, String)],
             prefixes : Seq[(XsdNCName, XsdAnyURI)]
             ) = new XmlElement(
    qName,
    value,
    attributes.toMap,
    prefixes.toMap,
    Some(Metadata(None, Some(attributes.map { _._1 }), Some(prefixes.map { _._1 }), None))
  )

  def apply(
             qName : XsdQName,
             value : Option[String],
             attributes : Seq[(XsdQName, String)],
             prefixes : Seq[(XsdNCName, XsdAnyURI)],
             locator : DiagnosticLocator
             ) = new XmlElement(
    qName,
    value,
    attributes.toMap,
    prefixes.toMap,
    Some(Metadata(None, Some(attributes.map { _._1 }), Some(prefixes.map { _._1 }), Some(locator)))
  )
}
case class XmlElement(
  qName : XsdQName,
  value : Option[String],
  attributesMap : Map[XsdQName, String],
  prefixToNamespaceURIMap : Map[XsdNCName, XsdAnyURI],
  metadata : Option[Metadata] = None
) extends NamespaceURIToPrefixResolver {
  import XmlElement._

  lazy val orderedAttributes : Seq[(XsdQName, String)] = {
    implicit val log = enter("orderAttributes")()
    +"Order attributes by attributesOrder metadata (if defined) or sort lexographically by name"
    if(metadata.isDefined && metadata.get.attributesOrder.isDefined) {
      ~"Sort by metadata attributesOrder"
      metadata.get.attributesOrder.get.map({ 
        qName => attributesMap.get(qName).map { value => (qName,value) }
      }).flatten
    } else {
      ~"Sort by name lexographically"
      attributesMap.toSeq.sortWith { (t1,t2) => t1._1.toString < t2._1.toString }
    }<~: log
  }

  lazy val orderedPrefixes : Seq[(XsdNCName, XsdAnyURI)] = {
    implicit val log = enter("orderedPrefixes")()
    +"Order prefixes by prefixOrder metadata (if defined) or sort lexographically name"
    if(metadata.isDefined && metadata.get.prefixesOrder.isDefined) {
      ~"Sort by metadata prefixOrder"
      metadata.get.prefixesOrder.get.map({
        prefix => prefixToNamespaceURIMap.get(prefix).map { uri => (prefix,uri) }
      }).flatten
    } else {
      ~"Sort by name lexographically"
      prefixToNamespaceURIMap.toSeq.sortWith { (t1,t2) => t1._1.toString < t2._1.toString }
    } <~: log
  }

  lazy val namespaceURIToPrefixMap = prefixToNamespaceURIMap.map(_.swap)

  def isValidPrefixForNamespaceURI(prefix: XsdNCName, namespaceURI: XsdAnyURI) = {
    implicit val log = enter("isValidPrefixForNamespaceURI") { "prefix" -> prefix :: "namespaceURI" -> namespaceURI :: Nil }
    +"TRUE if prefix is defined with the given namespaceURI otherwise FALSE"
    val optionNsURI = prefixToNamespaceURIMap.get(prefix)
    if(optionNsURI.isDefined) {
      optionNsURI.get == namespaceURI
    } else {
      false
    } <~: log
  }

  def getPrefixForNamespaceURI(namespaceURI: XsdAnyURI) = {
    implicit val log = enter("getPrefixForNamespaceURI"){ "namespaceURI" -> namespaceURI :: Nil }
    namespaceURIToPrefixMap.get(namespaceURI).orNull <~: log
  }
}