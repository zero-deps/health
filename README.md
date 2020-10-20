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
brew install bower purescript
```

To compile code:
```bash
./com
```

If you need REPL:
```bash
npx spago repl
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
deploySsh mon
```

## Input

Metric: `metric::<system-name>::<node-name>::<param-name>::<param-value>`

Message: `history::<casino>::<user>::<json>`

Error: `'error::<system-name>::<node-name>::<error-message>::<stack-trace>`
<stack-trace> is a stack trace list in JSON-format. For example,
[
{"className":"metrics.MyClass","method":"sendMessage","fileName":"MyClass.scala","lineNumber":14},
{"className":"metrics.MyClass","init":"apply","fileName":"MyClass.scala","lineNumber":12},
{"className":"metrics.Utils","method":"createMessage","fileName":"Utils.scala","lineNumber":123}]

Time is added automatically

```bash
nc -u 127.0.0.1 50123
echo -n "<data>" >/dev/udp/<stats-server>/<udp-port>
```

## Output

http://127.0.0.1:8002

### UI

Demo: https://demos.creative-tim.com/black-dashboard/examples/dashboard.html  
GitHub: https://github.com/creativetimofficial/black-dashboard

## Stats client

Library for sending messages to Stats.

It can be configured using conf-file
```
stats.client {
  enabled = <true|false|on|off> # default is false
  remote {
    host = <Stats host> # default is 127.0.0.1
    port = <Stats port> # default is 50123
  }
}
``` 

### Metric

Some of Metric's functionality is added automatically when you add ActorSystem Extension. See table:

| Metric                   | Metric alias | Period |
| ------------------------ | ------------ | ------ |
| Boot time                | uptime       | 5 sec  |
| CPU load                 | cpu.load     | 15 sec |
| Used memory              | mem.used     | 1 min  |
| Free memory              | mem.free     | 1 min  |
| Total memory             | mem.total    | 1 min  |
| File storage used space  | root./.used  | 1 hour |
| File storage free space  | root./.free  | 1 hour |
| File storage total space | root./.total | 1 hour |

File storage metrics is sent only for Linux or MacOs file system.

You can send your special metric using method `def stats.client.ClientExtension.sendMetric(param: String, value: String)`:

```
stats.metric("name", "value")
```

### Error

For sending error message use method `def stats.client.Stats.error(msg: Option[String], cause: Throwable)`:

```
stats.error("test ex", new Exception("err"))
```

Also you can use logger `stats.client.Logger` in application.conf:

```
akka {
  loggers += metrics.client.Logger
}
```
