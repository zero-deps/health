package .stats.kvs

import scala.collection.immutable

trait Kvs {
  def put(key: String, str: String): Unit
  def get(key: String): Option[String]
  def index(key: String, value: String): Unit
  def indexes(): immutable.Seq[(String, String)]
  def delete(key: String): Unit
}
