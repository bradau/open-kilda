#

GRAPH_SOURCES := $(wildcard *.dot)
GRAPH_IMAGES := $(GRAPH_SOURCES:.dot=.png)

all: docs

docs: ${GRAPH_IMAGES}

%.png: %.dot
	dot -Tpng -o$@ $<

.PHONY: all docs
