/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gtri.util.xmlbuilder.api;

import com.google.common.collect.ImmutableMap;
import org.gtri.util.xsddatatypes.XsdAnyURI;
import org.gtri.util.xsddatatypes.XsdNCName;
import org.gtri.util.xsddatatypes.XsdQName;

/**
 * Interface to communicate a DOM-style XML instance.
 * 
 * Terms:
 * <ol>
 * <li>active element - the most recently added element</li>
 * <li>open element - an element that is not currently the active element 
 * that has not been closed.</li>
 * <li>open element stack - an implicit LIFO stack of open elements. When an 
 * element is added the currently active element is pushed onto the stack. When
 * the active element is closed, the stack is popped and that open element 
 * becomes the new active element.</li>
 * <li>closed element - an active element that has been closed.</li>
 * </ol>
 * 
 * When an element is first added by an addXmlElement method call, it becomes 
 * the active element. If another element is added, it is added as a child node 
 * of the active element, the active element is pushed onto the open element 
 * stack and finally, the child element becomes the new active element. The 
 * active element is closed by a call to the endXmlElement method. Closing the 
 * active element pops the open element stack, setting the active element to the 
 * most recent open element.
 * 
 * Note1: Unless otherwise stated in documentation below, all OPTIONAL 
 * non-collection parameters for methods use the NULL value to indicate the 
 * parameter has not been set.
 * 
 * Note2: All methods with OPTIONAL collection-style parameters use an empty 
 * collection to indicate the parameter has not been set. The convenience fields 
 * EMPTY_ATTRIBUTES and EMPTY_PREFIXES are provided for indicating when "no 
 * attributes set" and "no prefixes set".
 * 
 * 
 * @author Lance
 */
public interface XmlContract {
  
  /**
   * An empty attribute map that should be used when there are no attributes.
   */
  public static final ImmutableMap<XsdQName, String> EMPTY_ATTRIBUTES = ImmutableMap.of();
  
  /**
   * An empty prefix to namespace URI map that should be used when there are no
   * prefixes.
   */
  public static final ImmutableMap<XsdNCName, XsdAnyURI> EMPTY_PREFIXES = ImmutableMap.of();
  
  /**
   * Add a new child element to the active element. The active element is pushed 
   * onto the open element stack and the new child element becomes active 
   * element. If there is no active element, the new child element becomes 
   * the document root. 
   * 
   * @param _name REQUIRED. the QName of the element
   * @param _value OPTIONAL. the single text node value of the element (with 
   * non-meaningful whitespace removed). NULL otherwise
   * @param _attributes OPTIONAL. Attributes for element
   * @param _namespacePrefixes OPTIONAL. Namespace prefixes for element
   */
  void addXmlElement(
          XsdQName _qName,
          String _value,
          ImmutableMap<XsdQName, String> _attributes,
          ImmutableMap<XsdNCName, XsdAnyURI> _prefixToNamespaceURIMap
          );
  
  /**
   * Add a new child comment node to the active element.
   * 
   * @param value REQUIRED. The text value of the comment
   */
  void addXmlComment(
          String _value
          );
  
  /**
   * Add a new child text node to the active element.
   * 
   * @param value REQUIRED. The text value of the comment
   */
  void addXmlText(
          String _value
          );
  
  /**
   * End changes to the active element. Pops the open element stack making the 
   * most recent open element the new active element.
   */
  void endXmlElement();
}
