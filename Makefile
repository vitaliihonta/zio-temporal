.PHONY: help
# target: help - Display callable targets
help:
	@egrep "^# target:" [Mm]akefile

.PHONY: gen-doc
gen-doc:
	SBT_OPTS="-Xmx4G -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=2G -Xss32M" sbt 'unidoc; docs/mdoc;'

.PHONY: clean
clean:
	sbt clean

.PHONY: build-site
build-site: gen-doc
	cd website && npm install --save && npm run build

.PHONY: start-site
start-site: gen-doc
	cd website && npm install --save && npm run start