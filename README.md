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

Error: `'error::<system-name>::<node-name>::<error-message>::<stack-trace>`
<stack-trace> is a stack trace list in JSON-format. For example,
[
{"className":".stats.MyClass","method":"sendMessage","fileName":"MyClass.scala","lineNumber":14},
{"className":".stats.MyClass","init":"apply","fileName":"MyClass.scala","lineNumber":12},
{"className":".stats.Utils","method":"createMessage","fileName":"Utils.scala","lineNumber":123}]

Time is added automatically

Send: `echo -n "<data>" >/dev/udp/<stats-server>/<udp-port>`

Examples:
```
echo -n "metric::::127.0.0.1:4245::mem.heap::153.97" >/dev/udp/127.0.0.1/50123
echo -n 'history::casino1::user1::{"$type":"LoginRequest"}' >/dev/udp/127.0.0.1/50123
echo -n 'error::TEST::local::Test Exception::[{"className":".stats.handlers.ErrorHandlerTest$$anonfun$1$$anonfun$apply$mcV$sp$1","method":"apply$mcV$sp","fileName":"ErrorHandlerTest.scala","lineNumber":14}]' >/dev/udp/localhost/50123
```

## Output

http://127.0.0.1:8002/monitor.html
