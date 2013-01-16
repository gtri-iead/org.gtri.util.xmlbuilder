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

import org.gtri.util.xmlbuilder.api

case class XmlFileLocator(
  charOffset :    Int,
  columnNumber :  Int,
  lineNumber :    Int,
  publicId :      String,
  systemId :      String
) extends api.XmlFileLocator {

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

