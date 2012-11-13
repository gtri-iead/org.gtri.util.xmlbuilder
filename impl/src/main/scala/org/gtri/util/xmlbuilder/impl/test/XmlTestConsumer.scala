package org.gtri.util.xmlbuilder.impl.test

import org.gtri.util.iteratee.impl.Iteratee._
import org.gtri.util.iteratee.impl.Consumer
import org.gtri.util.iteratee.api.Issue
import org.gtri.util.xmlbuilder.impl.XmlEvent

/**
 * Created with IntelliJ IDEA.
 * User: Lance
 * Date: 11/13/12
 * Time: 12:35 PM
 * To change this template use File | Settings | File Templates.
 */
class XmlTestConsumer extends Consumer[XmlEvent] {
  def iteratee = {
    def step(issues : List[Issue]) : (Input[XmlEvent]) => Iteratee[XmlEvent, Unit] = {
      case El(chunk, newIssues) =>
        for(item <- chunk) {
          println(item)
        }
        Cont(step(newIssues ::: issues))
      case Empty() =>
        Cont(step(issues))
      case EOF() =>
        Success((), issues, EOF[XmlEvent])
    }
    Cont(step(Nil))
  }
}
