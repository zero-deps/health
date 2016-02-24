# Stats

Stats aggregates and displays some statistics.

## Build

Do `sbt twirlCompileTemplates` to compile templates.

## Run

Do `sbt run`

## Deploy

Do `sbt deploy`

## Configuration

Default ports are:

UDP: `50123`

HTTP: `8001`

To change hostname set env variable `HOST_IP`.

## Input

Metric: `metric::<system-name>::<node-name>::<param-name>::<param-value>`

Message: `history::<casino>::<user>::<msg>::<correlation-id>`

Time is added automatically

Send: `echo -n "<data>" >/dev/udp/<stats-server>/<udp-port>`

Examples:
```
echo -n "metric::::127.0.0.1:4245::Heap::153.97" >/dev/udp/127.0.0.1/50123
echo -n "history::casino1::user1::LoginRequest::1" >/dev/udp/127.0.0.1/50123
```

## Output

To get data open WebSocket connection to `/websocket`. You will receive data in same format.
