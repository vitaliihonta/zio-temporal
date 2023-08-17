# Versioning example

## Worker versioning
How to run:  
**(1)** Start the workflow with the initial version. It will set the build-id the task queue is bind to:

```shell
make run-versioned-worker-starter-v0
```

**(2)** Run the versioned worker with the initial version.

```shell
make run-versioned-worker-v0
```

**(3)** Start the workflow with the next version. It will set a new default build-id for task queue.
```shell
make run-versioned-worker-starter-v1
```
You should see that the first worker is not picking up the workflow (it's in Running state).

**(4)** Run the second versioned worker along with the one with the initial version 

```shell
make run-versioned-worker-v1
```

You should see the second worker running a workflow, while the first worker is doing nothing.
