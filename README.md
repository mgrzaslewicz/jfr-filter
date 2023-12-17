# Filter for JFR files

## Parsing JFR files is hard if you want to parse all event types.
Filtering is implemented based on 2 existing projects:

- `jdk.jfr.consumer` from JDK 11 which parses JFR files and provides API to access events, but no way to access read
  bytes for chunk header, metadata, constant pool and events.
- JMC parser which reads bytes and parses them, but it does not understand event types and constant pool.
- This project combines both approaches to provide API to access all event types and read bytes for all structures.
- This way you can filter events and write bytes associated with each structure.

## Original implementation
I've made first implementation with Maciej Kwidziński when we were tuning up Jira performance by finding bottleneck in Jira runtime.

# Resources
- Original `JfrFilter` used for Jira performance tune up in repository: https://github.com/atlassian/report
- Maciej Kwidziński's github account: https://github.com/dagguh
- `JMC` writer: https://github.com/openjdk/jmc/tree/master/core/org.openjdk.jmc.flightrecorder.writer


# async-profiler pitfalls

## Intellij
### Workaround for bug https://youtrack.jetbrains.com/issue/IDEA-320011
Provide `-i <interval>` to make intellij display flame graph correctly, with all events included. Without that, only first 10 000 events are displayed.

### You have to include `jdk.ActiveSetting` events to display `Total Time` selector in flame graph
Take a look at the `JdkActiveSettingPredicate`. Without it you're going to see only `CPU Time` selector.
