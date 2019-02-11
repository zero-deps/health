# Stats

## Overview

### Motivation

* Quick way to check nodes status
* Collect JVM stats
* Track user history
* Don’t use debug logging :)

### Solution

```
                                        
+--------+           +-------+          
|        |    UDP    |       |          
| System +---------->+ Stats |          
|        |           |       |          
+--------+           +-+---+-+          
                       |   |            
     +-----------+     |   |     +-----+
     |           | TCP |   |     |     |
     | Dashboard +<----+   +---->+ KVS |
     |           |               |     |
     +-----------+               +-----+
                                        
```

See details in `presentation.pdf`.

## Build

Before you start you need to have `npm`.

One-time setup:
```bash
npm install
npm i -g bower purescript
npm -s run dep
```

To compile code:
```bash
npm -s run com
```

If you need REPL:
```bash
npm -s run rep
```

To see the result do:
```
open resources/index.html
```

## Run

Do `sbt run`

## Deploy

```
sbt
deploySsh cms2
```

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
echo -n 'action::test::101' >/dev/udp/127.0.0.1/50123
echo -n 'error::TEST::local::Test Exception::[{"className":".stats.handlers.ErrorHandlerTest$$anonfun$1$$anonfun$apply$mcV$sp$1","method":"apply$mcV$sp","fileName":"ErrorHandlerTest.scala","lineNumber":14}]' >/dev/udp/localhost/50123
```

## Output

http://127.0.0.1:8002/monitor.html
