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

Data format: `<system-name>#<node-name>#<param-name>#<timestamp>#<param-value>`

Data example: `#127.0.0.1:8080#CPU#1423219008203#0.04`

Send: `echo -n "<data>" >/dev/udp/<stats-server>/<udp-port>`

Send example: `echo -n "#127.0.0.1:4244#CPU#1423219008203#0.04" >/dev/udp/localhost/50123`

## Output

To get data open WebSocket connection to `/websocket`. You will receive data in same format.
