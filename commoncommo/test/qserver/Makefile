PUBINCDIR := ../../core/include
TAKTHIRDPARTYDIR := ../../../takthirdparty/builds/linux-amd64-release
CURL_CONF := $(TAKTHIRDPARTYDIR)/bin/curl-config

OBJS := qserver.o

CXXFLAGS += -I$(TAKTHIRDPARTYDIR)/include                                    \
            -fPIC -g -DCURL_STATICLIB                                        \
            -D__STDC_FORMAT_MACROS                                           \
            -Wall -Werror -std=c++11

qserver: $(OBJS)
	g++ -o $@ $^ -L../../core/impl -L$(TAKTHIRDPARTYDIR)/lib -lngtcp2 -lngtcp2_crypto_quictls -lssl -lcrypto -ldl -lpthread -liconv 

qserver.o: qserver.h qserver.cpp

.PHONY: clean all
clean:
	rm -f $(OBJS) qserver

all: qserver

%.cpp:
	@[ -f "$@" ] && touch "$@"
%.h:
	@[ -f "$@" ] && touch "$@"


