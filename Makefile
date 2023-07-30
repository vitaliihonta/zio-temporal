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
	temporal server start-dev

.PHONY: run-heartbeatingactivity-worker
run-heartbeatingactivity-worker:
	sbt "examples/runMain com.example.heartbeatingactivity.HeartbeatingActivityBatchWorker"

.PHONY: run-heartbeatingactivity-starter
run-heartbeatingactivity-starter:
	sbt "examples/runMain com.example.heartbeatingactivity.HeartbeatingActivityBatchStarter"

.PHONY: run-heartbeatingactivity-cancel
run-heartbeatingactivity-cancel:
	sbt "examples/runMain com.example.heartbeatingactivity.HeartbeatingActivityBatchCancelling"

.PHONY: run-payments-example
run-payments-example:
	sbt "examples/runMain com.example.payments.Main"
