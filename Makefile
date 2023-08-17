.PHONY: help
# target: help - Display callable targets
help:
	@egrep "^# target:" [Mm]akefile

.PHONY: gen-doc
gen-doc:
	SBT_OPTS="-Xmx6G -Xss64M" sbt 'unidoc; docs/mdoc;'

.PHONY: clean
clean:
	sbt clean

.PHONY: build-site
build-site: gen-doc
	cd website && npm install --save && npm run build

.PHONY: start-site
start-site: gen-doc
	cd website && npm install --save && npm run start

.PHONY: start-temporal
start-temporal:
	temporal server start-dev --dynamic-config-value 'frontend.workerVersioningDataAPIs=true' --dynamic-config-value 'frontend.workerVersioningWorkflowAPIs=true'

# Heartbeating activity example
.PHONY: run-heartbeatingactivity-worker
run-heartbeatingactivity-worker:
	sbt "examples/runMain com.example.heartbeatingactivity.HeartbeatingActivityBatchWorker"

.PHONY: run-heartbeatingactivity-starter
run-heartbeatingactivity-starter:
	sbt "examples/runMain com.example.heartbeatingactivity.HeartbeatingActivityBatchStarter"

.PHONY: run-heartbeatingactivity-terminate
run-heartbeatingactivity-terminate:
	sbt "examples/runMain com.example.heartbeatingactivity.HeartbeatingActivityBatchTerminate"

# payments example
.PHONY: run-payments-example
run-payments-example:
	sbt "examples/runMain com.example.payments.Main"

# versioned worker
.PHONY: run-versioned-worker-v0
run-versioned-worker-v0: WORKER_BUILD_ID=wrkr-v0
run-versioned-worker-v0: run-versioned-worker

.PHONY: run-versioned-worker-v1
run-versioned-worker-v1: WORKER_BUILD_ID=wrkr-v1
run-versioned-worker-v1: run-versioned-worker

run-versioned-worker:
	sbt "examples/runMain com.example.versioning_worker.VersionedWorkerMain worker $(WORKER_BUILD_ID)"

.PHONY: run-versioned-worker-starter-v0
run-versioned-worker-starter-v0: WORKER_BUILD_ID=wrkr-v0
run-versioned-worker-starter-v0:  run-versioned-worker-starter

.PHONY: run-versioned-worker-starter-v1
run-versioned-worker-starter-v1: WORKER_BUILD_ID=wrkr-v1
run-versioned-worker-starter-v1: run-versioned-worker-starter

run-versioned-worker-starter:
	sbt "examples/runMain com.example.versioning_worker.VersionedWorkerMain start $(WORKER_BUILD_ID)"
