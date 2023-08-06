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
	temporal server start-dev --dynamic-config-value 'frontend.workerVersioningDataAPIs=true'

# Heartbeating activity example
.PHONY: run-heartbeatingactivity-worker
run-heartbeatingactivity-worker:
	sbt "examples/runMain com.example.heartbeatingactivity.HeartbeatingActivityBatchWorker"

.PHONY: run-heartbeatingactivity-starter
run-heartbeatingactivity-starter:
	sbt "examples/runMain com.example.heartbeatingactivity.HeartbeatingActivityBatchStarter"

.PHONY: run-heartbeatingactivity-cancel
run-heartbeatingactivity-cancel:
	sbt "examples/runMain com.example.heartbeatingactivity.HeartbeatingActivityBatchCancelling"

# payments example
.PHONY: run-payments-example
run-payments-example:
	sbt "examples/runMain com.example.payments.Main"

# versioned workflows
.PHONY: run-versioned-wf-worker-v0
run-versioned-wf-worker-v0: WORKER_BUILD_ID=wrkr-v0
run-versioned-wf-worker-v0: run-versioned-wf-worker

.PHONY: run-versioned-wf-worker-v1
run-versioned-wf-worker-v1: WORKER_BUILD_ID=wrkr-v1
run-versioned-wf-worker-v1: run-versioned-wf-worker

run-versioned-wf-worker:
	sbt "examples/runMain com.example.versioning.VersionedWorkflowMain worker --build-id=$(WORKER_BUILD_ID)"

.PHONY: run-versioned-wf-starter-v0
run-versioned-wf-starter-v0: WORKER_BUILD_ID=wrkr-v0
run-versioned-wf-starter-v0: run-versioned-wf-starter

.PHONY: run-versioned-wf-starter-v1
run-versioned-wf-starter-v1: WORKER_BUILD_ID=wrkr-v1
run-versioned-wf-starter-v1: run-versioned-wf-starter

run-versioned-wf-starter:
	sbt "examples/runMain com.example.versioning.VersionedWorkflowMain start --build-id=$(WORKER_BUILD_ID)"
