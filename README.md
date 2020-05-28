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

## Showcase

http://ua--monitoring.ee..corp:8002/

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
{"className":".stats.MyClass","method":"sendMessage","fileName":"MyClass.scala","lineNumber":14},
{"className":".stats.MyClass","init":"apply","fileName":"MyClass.scala","lineNumber":12},
{"className":".stats.Utils","method":"createMessage","fileName":"Utils.scala","lineNumber":123}]

Time is added automatically

Send: `echo -n "<data>" >/dev/udp/<stats-server>/<udp-port>`

Examples:
```
echo -n "metric::::127.0.0.1:4245::mem.heap::153.97" >/dev/udp/127.0.0.1/50123
echo -n 'action::test::101' >/dev/udp/127.0.0.1/50123
echo -n 'measure::search.ts::557::80' >/dev/udp/127.0.0.1/50123
echo -n 'error::TEST::local::Test Exception::[{"className":".stats.handlers.ErrorHandlerTest$$anonfun$1$$anonfun$apply$mcV$sp$1","method":"apply$mcV$sp","fileName":"ErrorHandlerTest.scala","lineNumber":14}]' >/dev/udp/localhost/50123
```

## Output

http://127.0.0.1:8002

## Stats client
Library for sending messages to Stats.
Maven repository: http://ua--nexus01.ee..corp/nexus/content/repositories/releases
```
libraryDependencies += "com.." %% "stats_client" % <version>
```
You can add client functionality as Akka ActorSystem Extension
```
akka {
  extensions = [
    stats.client.ClientExtension
  ]
}
```
It can be configured using conf-file
```
stats.client{
    enabled=<true|false|on|off> #default is false
    remote{
        host=<Stats host> #default is 127.0.0.1
        port=<Stats port> #default is 50123
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

You can send your special metric using method *def stats.client.ClientExtension.sendMetric(param: String, value: Any)*
```
ClientExtension.sendMetric("system.geoip",""+(System.nanoTime-startTime))
```
### Actor's metric
There are two actor's metrics available: Actor's Ping (heartbeat) and Actor's Delta (the time of the last receive execution).
If you want to add heartbeat to the actor the actor have to extend *stats.client.SendPing* trait. The actor will send heartbeat message every 5 seconds: 
```
metric::::127.0.0.1:4245::actorsys.ping.<actor-path>::ping
```
If you want to add sending time of the last receive execution to actor you should: 
1. Extends *stats.client.SendDelta* trait. 
2. Added the code which execution should bw monitored to the *def monitoredReceive: Receive* function.
3. If you wand to add some code which shouldn't be monitored you can add it to function *def unMonitoredReceive: Receive*

**Warning: the method _def receive: Receive_ is final in _stats.client.SendDelta_!**
  
### History
For sending history message use method *def stats.client.ClientExtension.sendHistory(casino: String, user: String, message: String)*
```
ClientExtension.sendHistory("80065","vasya.pupkin","""{"$type":"LoginRequest"}""")
```
### Error
For sending error message use method *def stats.client.ClientExtension.sendError(error: Throwable)*
```
val error = new NullPinterException("Test error message")
ClientExtension.sendError(error)
```
Also you can use logger *stats.client.Logger* in conf
```
akka {
  loggers += .stats.client.Logger
}
```
