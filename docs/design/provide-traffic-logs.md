### 仅提供经过匿名处理的流量信息
```
[$time,$remote_addr,$request_path,$user_agent,$range_start,$range_end]
```

- time: unix 时间戳
- remote_addr: 匿名处理后的ip
- user_agent: (nullable)

example
```json
[1656420287342,"2a09:bac0:464::","/paper/1.19/39/paper-1.19-39.jar","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36",0,38117040]
```
### remote_addr 处理规则
在保证匿名的前提下保留研究价值
#### IPV4
保留 /24 位，抹除后 /8 位的数据

处理示例：112.112.101.33 => 112.112.101.0

#### IPV6
保留 /48 位，抹除后 /80 位的数据

处理示例：2a09:bac0:464::82f:f02 => 2a09:bac0:464::