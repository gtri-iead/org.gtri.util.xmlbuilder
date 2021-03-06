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
import org.gtri.util.issue.api.DiagnosticLocator
import org.gtri.util.xmlbuilder.api.{XmlContract, XmlEvent}


object EndXmlDocumentEvent {
  implicit val thisclass = classOf[EndXmlDocumentEvent]
  implicit val log = Logger.getLog(thisclass)
}
case class EndXmlDocumentEvent(locator : DiagnosticLocator) extends XmlEvent {
  import EndXmlDocumentEvent._
  def pushTo(contract: XmlContract) {
    log.block("pushTo", Seq("contract" -> contract )) {
      +"noop"
    }
  }
}
