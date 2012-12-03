package org.gtri.util.xmlbuilder.impl

import org.gtri.util.xmlbuilder.api

/**
 * Created with IntelliJ IDEA.
 * User: Lance
 * Date: 12/2/12
 * Time: 10:37 PM
 * To change this template use File | Settings | File Templates.
 */
case class XmlFileLocator(charOffset : Int, columnNumber : Int, lineNumber : Int, publicId : String, systemId : String) extends api.XmlFileLocator {

  def getCharOffset = charOffset

  def getColumnNumber = columnNumber

  def getLineNumber = lineNumber

  def getPublicId = publicId

  def getSystemId = systemId

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

