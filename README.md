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

UDP port: `50123`

HTTP port: `9010`

## Input

Format: `<node-name>#<param-name>#<timestamp>#<param-value>`

Example: `127.0.0.1:8080#cpu#1423219008203#1.09`

Send: `echo -n "<data>" >/dev/udp/<stats-server>/<udp-port>`

Example: `echo -n "127.0.0.1:4244#cpu#1423219008203^1.09" >/dev/udp/localhost/50123`

## Output

GET request: `<stats-server>:9010/get`

Response example: `{"127.0.0.1:4244":{"heap":{"time":"1423219008203","value":"62.57"},"cpu":{"time":"1423219008203","value":"1.09"}}}`
