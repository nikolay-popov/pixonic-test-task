# pixonic-test-task
Test task for Pixonic

Build and run tests with `mvn clean install`

`TaskExecutionService` interface with `DefaultTaskExecutionService` implementation complete the service requested in test task.

`DefaultTaskExecutionServiceTest` shows the service in action and provides the proof that the solution works.

Main algorithm:

- Tasks submitted by clients are put into a PriorityBlockingQueue where they are sorted by `localDateTime`
- Separate task execution thread peeks first item in the queue (waiting for an item if necessary)
- If it is not yet time to execute the task, thread waits on a monitor for time until task should be executed
- For the case where a new task arrives while thread waits on monitor, service submit service signals the thread to give it a change to check if new task should be executed earlier.

Implementation notes:

- There is single task execution thread for the sake of simplicity but it's trivial to add multithreaded tasks executor without compromising thread safety.
- `DefaultTaskExecutionService` takes `LocalDateTime` supplier to allow tests to trigger time updates without relying or need to wait on real time. `DefaultLocalDateTimeSupplier` implementation provided which allows `DefaultTaskExecutionService` to use real time.