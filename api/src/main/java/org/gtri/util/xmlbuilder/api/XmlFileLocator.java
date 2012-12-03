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

import org.gtri.util.iteratee.api.ImmutableDiagnosticLocator;

/**
 * An interface for the location of an event within an XML file.
 * 
 * @author lance.gatlin@gmail.com
 */
public interface XmlFileLocator extends ImmutableDiagnosticLocator {
  /**
   * Get the character offset
   * @return the character offset
   */
  int getCharOffset();
  /**
   * Get the column number
   * @return the column number
   */
  int getColumnNumber();
  /**
   * Get the line number
   * @return the line number
   */
  int getLineNumber();
  /**
   * Get the public identifier
   * @return the public identifier
   */
  String getPublicId();
  /**
   * Get the system identifier
   * @return the system identifier
   */
  String getSystemId();
}
