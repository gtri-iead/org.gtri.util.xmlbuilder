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
package org.gtri.util.xmlbuilder.api;

import javax.xml.XMLConstants;
import org.gtri.util.xsddatatypes.XsdAnyURI;
import org.gtri.util.xsddatatypes.XsdNCName;

/**
 *
 * @author Lance
 */
public class XmlConstants {
  public static XsdAnyURI NULL_NS_URI = new XsdAnyURI(XMLConstants.NULL_NS_URI);
//  public static XsdNCName DEFAULT_NS_PREFIX = null;
  public static XsdNCName DEFAULT_NS_PREFIX = new XsdNCName(XMLConstants.DEFAULT_NS_PREFIX);
  public static XsdAnyURI XML_NS_URI = new XsdAnyURI(XMLConstants.XML_NS_URI);
  public static XsdNCName XML_NS_PREFIX = new XsdNCName(XMLConstants.XML_NS_PREFIX);
  public static XsdAnyURI XMLNS_ATTRIBUTE_NS_URI = new XsdAnyURI(XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
  public static XsdNCName XMLNS_ATTRIBUTE = new XsdNCName(XMLConstants.XMLNS_ATTRIBUTE);  
  public static XsdAnyURI W3C_XML_SCHEMA_NS_URI = new XsdAnyURI(XMLConstants.W3C_XML_SCHEMA_NS_URI);  
  public static XsdAnyURI W3C_XML_SCHEMA_INSTANCE_NS_URI = new XsdAnyURI(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
  public static XsdAnyURI W3C_XPATH_DATATYPE_NS_URI = new XsdAnyURI(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);  
  public static XsdAnyURI XML_DTD_NS_URI = new XsdAnyURI(XMLConstants.XML_DTD_NS_URI);  
  public static XsdAnyURI RELAXNG_NS_URI = new XsdAnyURI(XMLConstants.RELAXNG_NS_URI);
  public static XsdAnyURI FEATURE_SECURE_PROCESSING = new XsdAnyURI(XMLConstants.FEATURE_SECURE_PROCESSING);
}