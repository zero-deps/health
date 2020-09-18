package .stats

object keys {
  val `action.live`           = str_to_bytes("action.live")
  val `cpu_mem.live`          = str_to_bytes("cpu_mem.live")
  val `cpu.hour`              = str_to_bytes("cpu.hour")
  val `errors`                = str_to_bytes("errors")
  val `kvs.size.year`         = str_to_bytes("kvs.size.year")
  val `metrics`               = str_to_bytes("metrics")
  val `reindex.all.latest`    = str_to_bytes("reindex.all.latest")
  val `search.fs.latest`      = str_to_bytes("search.fs.latest")
  val `search.ts.latest`      = str_to_bytes("search.ts.latest")
  val `search.wc.latest`      = str_to_bytes("search.wc.latest")
  val `static.gen.latest`     = str_to_bytes("static.gen.latest")
  val `static.gen.year`       = str_to_bytes("static.gen.year")
  val `feature`               = str_to_bytes("feature")
}