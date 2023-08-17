# Heartbeating activity

The example demonstrates the concept of Activity heartbeats and how to use it.  
How to run:

**(1)** Schedule workflow executing:

```shell
make run-heartbeatingactivity-starter
```

**(2)** Start the worker

```shell
make run-heartbeatingactivity-worker
```

You should see the worker processing records one-by-one (it will take looong time to finish it.)

**(3)** Kill the worker process (CTRL+C).  

**(4)** Re-start the worker process again

```shell
make run-heartbeatingactivity-worker
```

You should see the worker continuing the current workflow from the latest processed record id 

**(4a)** If you're tired waiting the workflow to finish, simply terminate it:
```shell
make run-heartbeatingactivity-terminate
```