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

import org.gtri.util.scala.exelog.sideeffects._
import org.gtri.util.xsddatatypes.XsdQName
import org.gtri.util.issue.api.DiagnosticLocator
import org.gtri.util.xmlbuilder.api.{XmlContract, XmlEvent}

object EndXmlElementEvent {
  implicit val classlog = ClassLog(classOf[EndXmlElementEvent])
}
case class EndXmlElementEvent(qName : XsdQName, locator : DiagnosticLocator) extends XmlEvent {
  import EndXmlElementEvent._
  def pushTo(contract: XmlContract) {
    implicit val log = enter("pushTo") { "contract" -> contract :: Nil }
    +"Pushing EndXmlElementEvent to XmlContract"
    ~"contract.endXmlElement()"
    contract.endXmlElement()
  }
}

