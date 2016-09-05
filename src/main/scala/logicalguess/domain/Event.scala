package logicalguess.domain

import scala.util.Random
import java.util.Date

object Category extends Enumeration {
  val info, warning, error = Value
}

/**Event model */
case class Event(guid: String, category: Category.Value, action: String, timestamp: String)

/**Random event generation routines */
object RandomEvent {
  val categoryCount: Int = Category.values.size
  def apply() = Event(randomGiud, randomCategory, randomAction, timestamp)
  def randomGiud = java.util.UUID.randomUUID.toString
  def timestamp = new Date().toString()
  def randomCategory = Category(random.nextInt(categoryCount))
  def randomAction = actions(random.nextInt(actions.size))
  val random = new Random
  val actions = Seq("POST /event", "GET /events", "GET /")
}