package zero.ws

// import zio._

class WebServer {
  // val app: ZIO[ActorSystem, Any, Unit] = {
  //   (for {
  //     q    <- Queue.unbounded[NewEvent]
  //     _    <- workerLoop(q).forever.fork
  //     addr <- SocketAddress.inetSocketAddress(8001)
  //     kvs  <- ZIO.access[Kvs](_.get)
  //     bl   <- ZIO.access[Blocking](_.get)
  //     _    <- httpServer.bind(
  //               addr,
  //               IO.succeed(req => httpHandler(req).provideLayer(ZLayer.succeed(kvs))),
  //               IO.succeed(msg => wsHandler(q)(msg).provideSomeLayer[WsContext](ZLayer.succeed(kvs) ++ ZLayer.succeed(bl)))
  //             )
  //   } yield ()).provideLayer(Kvs.live ++ Blocking.live)
  // }

  // val runtime = Runtime.default
  // runtime.unsafeRun(app.provideLayer(system).fold(
  //   err => { println(err); 1 },
  //   _   => {               0 }
  // ))
}