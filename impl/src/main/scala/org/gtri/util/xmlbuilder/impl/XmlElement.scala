package org.gtri.util.xmlbuilder.impl

import org.gtri.util.xsddatatypes.{XsdAnyURI, XsdNCName, XsdQName}
import org.gtri.util.xsddatatypes.XsdQName.NamespaceURIToPrefixResolver
import org.gtri.util.xmlbuilder.impl.XmlElement.Metadata
import org.gtri.util.iteratee.api.ImmutableDiagnosticLocator

/**
 * Created with IntelliJ IDEA.
 * User: Lance
 * Date: 12/2/12
 * Time: 10:36 PM
 * To change this template use File | Settings | File Templates.
 */
case class XmlElement(
  qName : XsdQName,
  value : Option[String],
  attributesMap : Map[XsdQName, String],
  prefixToNamespaceURIMap : Map[XsdNCName, XsdAnyURI],
  metadata : Option[Metadata] = None
) extends NamespaceURIToPrefixResolver {
  lazy val orderedAttributes : Seq[(XsdQName, String)] = {
    if(metadata.isDefined && metadata.get.attributesOrder.isDefined) {
      metadata.get.attributesOrder.get.map({ 
        qName => attributesMap.get(qName).map { value => (qName,value) }
      }).flatten
    } else {
      attributesMap.toSeq.sortWith { (t1,t2) => t1._1.toString < t2._1.toString }
    }
  }
  lazy val orderedPrefixes : Seq[(XsdNCName, XsdAnyURI)] = {
    if(metadata.isDefined && metadata.get.prefixesOrder.isDefined) {
      metadata.get.prefixesOrder.get.map({
        prefix => prefixToNamespaceURIMap.get(prefix).map { uri => (prefix,uri) }
      }).flatten
    } else {
      prefixToNamespaceURIMap.toSeq.sortWith { (t1,t2) => t1._1.toString < t2._1.toString }
    }
  }
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

object XmlElement {
  case class Metadata(
    rawAttributesOrder: Option[Seq[String]] = None,
    attributesOrder: Option[Seq[XsdQName]] = None,
    prefixesOrder: Option[Seq[XsdNCName]] = None,
    locator : Option[ImmutableDiagnosticLocator] = None
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
    locator : ImmutableDiagnosticLocator
  ) = new XmlElement(
    qName,
    value,
    attributes.toMap,
    prefixes.toMap,
    Some(Metadata(None, Some(attributes.map { _._1 }), Some(prefixes.map { _._1 }), Some(locator)))
  )
}