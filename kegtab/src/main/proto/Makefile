TOP := $(shell pwd)
SRCDIR := $(TOP)/../java
PROTODIR := $(TOP)
PROTOLIB := $(TOP)/protobuf-java-$(shell protoc --version | cut -d" " -f2).jar
LIBDIR := $(TOP)/../../../libs/kegbot-api

proto:
	protoc -I $(PROTODIR) $(PROTODIR)/*.proto --java_out=$(SRCDIR)
	(cd $(SRCDIR); javac -classpath $(PROTOLIB) $(SRCDIR)/org/kegbot/proto/*.java)
	(cd $(SRCDIR); jar cf $(LIBDIR)/kegbot-api.jar org/kegbot/proto/*.class)
	(cd $(SRCDIR); jar cf $(LIBDIR)/kegbot-api-src.jar org/kegbot/proto/*.java)
	rm -f $(SRCDIR)/org/kegbot/proto/*.{class,java,jar}

.PHONY: proto
