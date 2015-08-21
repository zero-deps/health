# Stats

Stats aggregates and displays some statistics.

## Build

Do `sbt twirlCompileTemplates` to compile templates.

## Run

Do `sbt run`

## Deploy

Do `sbt deploy` to deploy to `nb-` server.

## Configuration

Default ports are:

UDP: `50123`

HTTP: `9010`

To change hostname set env variable `HOST_IP`.

## Input

Metric: `metric::<system-name>::<node-name>::<param-name>::<timestamp>::<param-value>`

Message: `message::<casino>::<user>::<msg>::<timestamp>`

Send: `echo -n "<data>" >/dev/udp/<stats-server>/<udp-port>`

Examples:
```
echo -n "metric::::127.0.0.1:4245::Heap::1439988973155::153.97" >/dev/udp/localhost/50123
echo -n "message::casino1::user1::LoginRequest::1439988973155" >/dev/udp/localhost/50123
```

## Output

To get data open WebSocket connection to `/websocket`. You will receive data in same format.
