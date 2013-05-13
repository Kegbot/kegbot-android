proto:
	protoc -I src/org/kegbot/proto src/org/kegbot/proto/*.proto --java_out=src/
	(cd src; javac -classpath ../libs/protobuf/protobuf-java-2.5.0.jar org/kegbot/proto/*.java)
	(cd src; jar cf ../libs/kegbot-api/kegbot-api.jar org/kegbot/proto/*.class)
	(cd src; jar cf ../libs/kegbot-api/kegbot-api-src.jar org/kegbot/proto/*.java)
	rm -f src/org/kegbot/proto/*.class src/org/kegbot/proto/*.java

.PHONY: proto
