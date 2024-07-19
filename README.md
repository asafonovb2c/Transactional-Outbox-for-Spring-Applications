# Transactional Outbox

## Purpose of this Library

The Transactional Outbox is designed to ensure atomicity of operations that require multiple actions to be performed simultaneously, such as database (DB) writes, API requests, and event publishing to a messaging system (e.g., RabbitMQ, Kafka).

In a typical microservice architecture, you often need to save a record in the database, send a request to another microservice through an API, and publish an event to RabbitMQ, all in a specific sequence. However, under normal circumstances, the lack of transactional support for some of these operations—specifically the API call and the RabbitMQ event—means you can't ensure one complete successfully as a single unit. For instance, you might successfully save the record in the database and publish the event to RabbitMQ, but then the API request could fail if the service is down.

By using a Transactional Outbox, you can ensure that your actions are atomic. This means that you can save a new entity and two event commands in the database within a single transaction: one for making the API request and another for publishing the event. The library then processes those event commands asynchronously, handling the API request and sending the message to RabbitMQ. This approach guarantees that either all actions are completed or none of them are.

Another use case for the Transactional Outbox is breaking complex calculations into separate events, similar to the MapReduce principle, which creates save points for each step of the calculation. This approach allows you to resume the calculation process from any of these points if an error occurs. For example, when processing an event from RabbitMQ, you may need to create and save multiple database records and make API requests for each one. If you perform all these actions synchronously, it could greatly reduce the system's throughput and slow down RabbitMQ event processing.

Using the Transactional Outbox allows you to save the data received from RabbitMQ and send commands to perform calculations simultaneously. This eliminates bottlenecks by separating data saving from messaging system and API requests. It also simplifies changes and extensions to the calculation logic by enabling the creation of event chains and improving load distribution within the application.

If your application architecture follows Domain-Driven Design and includes multiple contexts in the model, the Transactional Outbox can help manage interactions between domain contexts without mixing code service and entities or leaking logic from one context to another. Each context can notify others about its changes while maintaining its domain boundaries, ensuring overall system integrity and consistency.

One of the significant advantages of using the Transactional Outbox is its ability to reprocess commands. For instance, if a neighboring module is unavailable and the API request fails, the retry mechanism allows you to reprocess the request as many times as needed by simply setting the retry parameters and the delay time between attempts.

For each event type, a separate thread pool is created with adjustable parameters, such as the number of threads, the number of coroutines per thread, and the maximum task execution time.

### Unsuitable Scenarios 

The Transactional Outbox is not suitable for scenarios that require synchronous responses. For example, if a user's request involves contacting a neighboring module through an API and returning a response, it's better to handle these actions synchronously. This way, the user can quickly understand the result of their actions.

## How does it work?

When this library is injected, it creates a schema named 'outbox' and a table 'outbox.event 'in the database to store events.
You can find sql migrations in (V1__init.sql)

When the service starts, a scheduler is created for each class that implements the OutboxEventHandleStrategy<T extends OutboxEventDto> interface.

Each scheduler periodically checks the outbox.event table for events of a specified type (OutboxEventType.name()) and processes them using the logic defined in the OutboxEventHandleStrategy<T>.handleEvent(@NotNull T eventDto) method.

### Event Processing Outcomes

Depending on the processing result, there are several possible outcomes for each event:

1. Successful Processing: The event is processed successfully and is no longer needed. In this case, it is deleted from the outbox.event table.

2. Failed Lock Acquisition: If the event cannot acquire a lock for processing, it will be reprocessed until it successfully obtains the lock.

3. Unsuccessful Processing: If the event acquires a lock but fails to achieve the desired result, the attempt counter increases by 1, and the event will be reprocessed after Instant.now().plusMillis((attempts * attempts * nextDelay)).

4. Exceeded Maximum Attempts: If the event fails to process successfully and the number of attempts exceeds the maximum value defined by outbox.attempts.max, the event will either be deleted (if outbox.delete.attempts.max is set to true) or will remain in the database without further processing.

5. Processing Error: If an error occurs while processing the event, its status will be set to DISABLED, meaning it will no longer be processed, but it will not be deleted. The error details will be recorded in the outbox.event.fail_reason field.


## How to Use the Library in Your Application Code

This library is tailored for Spring Boot version 2.7.18

A relational database, such as PostgreSQL, is required for operation.

To ensure the application works effectively when scaled and operating with more than one instance, distributed locks must be used. Redis (or its equivalent, KeyDB) is used as the locking mechanism.

1. To create new events, you can use two methods of the OutboxEventDbPort class:

``` java
     /**
      * Save a new event to the queue
      * @param eventType type of events
      * @param eventDto the event to save
      * @param runtime the time when the event needs to be processed
      */
     saveNewEvent(OutboxEventType eventType, T eventDto, Instant runtime)

     /**
      * Save new events to the queue
      * @param type type of events
      * @param runtime the time when the events need to be processed
      * @param events the events to save
      */
     saveNewEvents(OutboxEventType type, Instant runtime, Set<T> events)
    
      @Slf4j
      @Service
      @RequiredArgsConstructor
      public class TestEventSaver {
      
          private final OutboxEventDbPort queueEventDbPort;

          public void insertTestEvent() {
              log.info("inserting events");
              queueEventDbPort.saveNewEvent(TestQueueEventType.TEST_QUEUE_EVENT_TYPE,
                      new TestEvent(UUID.randomUUID().toString()), Instant.now());
          }
      }
```    

2. Implement the library's contracts

Event Typing Contract: The simplest way is to create an enum.

``` java
      public enum TestQueueEventType implements OutboxEventType {
          TEST_QUEUE_EVENT_TYPE,
          TEST_QUEUE_EVENT_TYPE_SECOND;

          // This value will be the name of the event
          // The library will identify the event by this name
          // and find it in the DB in outbox.event.event_type
          // The name must be unique!
          @NotNull
          @Override
          public String getName() {
              return this.name();
          }
      }
```    

Event Contract:

``` java
      @Getter
      @Setter
      @AllArgsConstructor
      @NoArgsConstructor
      public class TestEvent implements OutboxEventDto {
      
          private String uuid;

          // Be sure to specify the lock key when processing the event
          // to avoid database deadlocks and overall data inconsistency
          @NotNull
          @Override
          public String provideLockKey() {
              return uuid;
          }
      }
```    

Event Handler Contract:

``` java     
      @Slf4j
      @Service
      @RequiredArgsConstructor
      public class NewOutboxStrategy implements OutboxEventHandleStrategy<TestEvent> {
      
          private final TestDAO testDAO;
      
          @NotNull
          @Override
          public OutboxEventType getEventType() {
              return TestQueueEventType.TEST_QUEUE_EVENT_TYPE;
          }
      
          @NotNull
          @Override
          public Class<TestEvent> getEventClass() {
              return TestEvent.class;
          }
      
          @NotNull
          @Override
          public OutboxEventResult handleEvent(@NotNull TestEvent eventDto) {
              log.info("TEST_QUEUE_EVENT_TYPE");
              testDAO.saveOrUpdate(new TestEventSecond(eventDto.getUuid(), 0));
              return OutboxEventResultKt.createEventProcessed();
          }
      }
```

If an exception is thrown in the handleEvent method, the event's status will be set to DISABLED, meaning it will no longer be processed, but it will not be deleted. The stack trace will be recorded in the outbox.event.fail_reason field.

3. Methods for Creating OutboxEventResult

The following methods are provided to create an OutboxEventResult, which should be returned by the handleEvent method:

- OutboxEventResultKt.createEventProcessed: Creates a result indicating that the event was processed successfully.
- OutboxEventResultKt.createEventWithFailedResult(String sleepReason): Creates a result with a reason for unsuccessful processing; the event will be processed again.
- OutboxEventResultKt.createEventFailedWithException(String sleepReason, Throwable e): Creates a result that includes a reason and an exception; the event will be processed again.
- OutboxEventResultKt.createEventLockBusyResult(String lockKey): Creates a result indicating that the lock key is busy; the event will be processed again.


4. Queue Operation Metrics

You can get the metrics at the following address: http://localhost:yourport/actuator/prometheus. Here are some examples of the metrics:

Queue Processing Speed:

      HELP outbox_event_processed_seconds_max
      TYPE outbox_event_processed_seconds_max gauge
      outbox_event_processed_seconds_max{groupType="outbox",type="test_queue_event_type_second",} 1.2243629
      outbox_event_processed_seconds_max{groupType="outbox",type="test_queue_event_type",} 1.4296554
      HELP outbox_event_processed_seconds
      TYPE outbox_event_processed_seconds summary
      outbox_event_processed_seconds_count{groupType="outbox",type="test_queue_event_type_second",} 242.0
      outbox_event_processed_seconds_sum{groupType="outbox",type="test_queue_event_type_second",} 180.6041639
      outbox_event_processed_seconds_count{groupType="outbox",type="test_queue_event_type",} 231.0
      outbox_event_processed_seconds_sum{groupType="outbox",type="test_queue_event_type",} 199.4213312

Number of Events in outbox.event in the DB:

      HELP outbox_event_size
      TYPE outbox_event_size gauge
      outbox_event_size{type="test_queue_event_type_second",} 183.0
      outbox_event_size{type="test_queue_event_type",} 1162.0

You can also set custom tags for queue operations by overriding the default method in OutboxEventHandleStrategy:

``` java
    /**
     * Returns a set of metric tags for monitoring event processing
     * @return a list of tags for the metric
     */
    @Override
    public Collection<Tag> getMetricTags() {
        return List.of(Tag.of(EventProcessingMetrics.TAG_EVENT_TYPE_NAME, this.getEventType().getName().toLowerCase()),
                       Tag.of(EventProcessingMetrics.TAG_EVENT_GROUP_NAME, "outbox"));
    }
``` 

### How to Connect the Library

Mandatory Steps:

1. Add to pom.xml:

``` xml
    <dependency>
        <groupId>com.asafonov</groupId>
        <artifactId>outbox</artifactId>
        <version>version</version>
    </dependency>
```

2. Specify the properties in application.properties to define the type of locks used: outbox.key.lockType=[REDIS or LOCAL].

_*IF THE APPLICATION HAS MORE THAN ONE INSTANCE - USING REDIS IS MANDATORY*_

When choosing the REDIS option, specify the connection parameters to Redis:

``` properties
      spring.redis.database = 3
      spring.redis.host = dev.keydb.service.tech
      spring.redis.port = 6397
      spring.redis.timeout = 15000
      spring.redis.password: "{{ credentials['redis']['password'] }}"
      spring.redis.username: "{{ credentials['redis']['username'] }}"
```


If the service uses Flyway for migrations, you need to define _your_ Flyway bean in the config:

``` java
        @Bean(initMethod = "migrate")
        public Flyway flyway(DataSource dataSource) {
            return Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:/migrations/public") // Your path to migrations
                    .schemas("public") // Your schema name
                    .load();
        }
```

4. Remove @EnableScheduling from your application.

### Additional Configuration

Each of the properties listed below has default values, but you can specify your own:

The standard Spring configurations with the prefix spring.task.scheduling are used. If needed, you can override these parameters in your application.properties file.

The default values are set as follows:

``` properties
    spring.task.scheduling.pool.size: This is set to the number of classes with the @Scheduled annotation plus the number of classes implementing the interface for working with the library.
    spring.task.scheduling.shutdown.await-termination-period: This is set to the longest timeout of all events.
    spring.task.scheduling.shutdown.await-termination: This is set to true.
    spring.task.scheduling.thread-name-prefix: This is set to outbox-scheduler.

```

2. Outbox Configuration


| Property Name                 |     Default     |                                                                                                                                                         Description                                                                                                                                                         |
|-------------------------------|:---------------:|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| outbox.process.enabled        |      true       |                                                                               Enables or disables event processing from the queue in the database. This property does not affect the saving of events to the database, only their processing.                                                                               |
| outbox.save.enabled           |      true       |                                                                                                Enables or disables saving events to the database. This property does not affect event processing, only the saving of events.                                                                                                |
| outbox.load.events.batch      |       100       |                                                                                                                          Specifies the batch size of events to be selected when processing events.                                                                                                                          |
| outbox.first.sleep            |        0        |                                                                           Waiting time for the first event processing. This sets the time for the first event to be processed as Instant.now() + number of milliseconds from outbox.first.sleep.                                                                            |
| outbox.repeat.delay           |        0        |                                                         Waiting time when attempting to acquire a lock for processing the event queue type (not for each individual event). This is relevant when multiple instances of the application are running simultaneously.                                                         |
| outbox.repeat.delay.on.lock   |      1000       |                                                         Waiting time when attempting to acquire a lock for processing the event queue type (not for each individual event). This is relevant when multiple instances of the application are running simultaneously.                                                         |
| outbox.repeat.delay.on.empty  |      10000      |                                                                                                                               Waiting time for reprocessing the event if the queue is empty.                                                                                                                                |
| outbox.next.delay             |      10000      |                                                                                                        Time in milliseconds after which reprocessing of the event can begin (retry delay for subsequent processes).                                                                                                         |
| outbox.attempts.max           |        3        |                                                                                                              Maximum number of attempts to process a specific event. A busy lock does not count as an attempt.                                                                                                              |
| outbox.delete.attempts.max    |      false      |                                                                                                   Determines whether to delete or retain events from the queue when the maximum number of processing attempts is reached.                                                                                                   |
| outbox.pool.max.size          |        2        |                                                                                                                                Maximum pool size for the task executor for each event type.                                                                                                                                 |
| outbox.pool.core.size         |        1        |                                                                                                                                  Core pool size for the task executor for each event type.                                                                                                                                  |
| outbox.pool.timeout           |     120000      |                                                                            Maximum waiting time for event processing (set using taskExecutor.setRejectedExecutionHandler(CallerBlocksPolicy(setting.timeout))) and TTL in Redis for event locks.                                                                            |
| outbox.coroutines.per.thread  |        5        |                                                                                                                                  Number of coroutines per thread in outbox.pool.max.size.                                                                                                                                   |
| outbox.execution.type         |    PARALLEL     |                                                 Specifies the execution type: EXCLUSIVE or PARALLEL. In PARALLEL mode, all instances of the application process the same event type simultaneously. In EXCLUSIVE mode, only one instance will process the given event type.                                                 |
| outbox.stash.name             | $eventType-HASH |                                                                                                                     Key for the hash map in Redis to store event IDs that are already being processed.                                                                                                                      |
| outbox.next.delay.coefficient |       1f        |                                                                                     Coefficient by which the delay time is multiplied with each attempt, starting from outbox.next.delay. It can be a non-integer value, such as 0.42!                                                                                      |
| outbox.next.delay.increase    |      true       | Whether to increase the waiting time for repeated event processing. By default, this is set to true. The processing delay between repeated attempts will be calculated as attempt number * outbox.next.delay.coefficient * outbox.next.delay. If set to false, attempts will occur at equal intervals of outbox.next.delay. |


Each property can be set in the application.properties file. You have the option to configure properties either for all queues at once or separately for each one.

- outbox.STRATEGY_NAME.repeat.delay = 9999: This sets the repeat delay for the queue named STRATEGY_NAME.
- outbox.repeat.delay = 8888: This sets the repeat delay for all queues that do not have a specific property set.

This allows you to specify only the parameters that need a custom value. If a property is not defined in the configuration, the default value from the table above will be used.




