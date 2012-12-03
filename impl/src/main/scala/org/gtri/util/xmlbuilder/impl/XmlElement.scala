package org.gtri.util.xmlbuilder.impl

import org.gtri.util.xsddatatypes.{XsdAnyURI, XsdNCName, XsdQName}
import org.gtri.util.xsddatatypes.XsdQName.NamespaceURIToPrefixResolver

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

