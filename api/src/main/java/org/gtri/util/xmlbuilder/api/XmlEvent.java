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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gtri.util.xmlbuilder.api;

import org.gtri.util.issue.api.DiagnosticLocator;

/**
 * An interface for an XML event.
 * 
 * @author lance.gatlin@gmail.com
 */
public interface XmlEvent {
  /**
   * Get the diagnostic locator for the event
   * @return the diagnostic locator for the event
   */
  DiagnosticLocator locator();
  /**
   * Push the event to the contract
   * @param contract 
   */
  void pushTo(XmlContract contract);
}
