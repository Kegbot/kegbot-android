TOP := $(shell pwd)
SRCDIR := $(TOP)/../java
PROTODIR := $(TOP)

proto:
	protoc -I $(PROTODIR) $(PROTODIR)/api.proto --java_out=$(SRCDIR)

.PHONY: proto
