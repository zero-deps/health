# Stats

Stats aggregates and displays some statistics.

## Build

```
npm install --global babel-cli
npm install babel-preset-react
babel --presets react "src/main/jsx" --watch --out-dir "src/main/resources/stats"
```

## Run

Do `sbt run`

## Deploy

Do `sbt deploy`

## Configuration

To change hostname set env variable `HOST_IP`.

## Input

Metric: `metric::<system-name>::<node-name>::<param-name>::<param-value>`

Message: `history::<casino>::<user>::<json>`

Time is added automatically

Send: `echo -n "<data>" >/dev/udp/<stats-server>/<udp-port>`

Examples:
```
echo -n "metric::::127.0.0.1:4245::Heap::153.97" >/dev/udp/127.0.0.1/50123
echo -n 'history::casino1::user1::{"$type":"LoginRequest"}' >/dev/udp/127.0.0.1/50123
```

## Output

http://127.0.0.1:8002/monitor.html
