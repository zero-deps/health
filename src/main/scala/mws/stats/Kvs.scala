package .stats

object Kvs {
  trait Wrapper extends Kvs {
    val kvs: Kvs
    val bucket: String
    def put(key: String, value: String): Unit = kvs.put(s"$bucket.$key", value)
    def get(key: String): Option[String] = kvs.get(s"$bucket.$key")
    def delete(key: String): Unit = kvs.delete(s"$bucket.$key")
    def close(): Unit = kvs.close()
  }
}

trait Kvs {
  def put(key: String, value: String): Unit
  def get(key: String): Option[String]
  def delete(key: String): Unit
  def close(): Unit
}
