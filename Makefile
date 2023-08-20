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

.PHONY: start-jaeger
start-jaeger:
	docker run -d --name jaeger \
	  -e COLLECTOR_ZIPKIN_HOST_PORT=:9411 \
	  -e COLLECTOR_OTLP_ENABLED=true \
	  -p 6831:6831/udp \
	  -p 6832:6832/udp \
	  -p 5778:5778 \
	  -p 16686:16686 \
	  -p 4317:4317 \
	  -p 4318:4318 \
	  -p 14250:14250 \
	  -p 14268:14268 \
	  -p 14269:14269 \
	  -p 9411:9411 \
	  jaegertracing/all-in-one:1.47

.PHONY: stop-jaeger
stop-jaeger:
	docker rm -f jaeger

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
