package .stats.kvs

import scala.collection.immutable

trait Kvs {
  def put(key: String, str: String): Unit
  def get(key: String): Option[String]
  def get(): immutable.Seq[(String, String)]
  def delete(key: String): Unit
}
