package com.example;

import io.netty.buffer.ByteBuf;
import jsm.ReactorUtils;
import jsm.WriterUtils;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

public class Client {

    private final HttpClient httpClient;

    public Client(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Mono<String> create(Mono<com.example.Item> itemMono) {
        return createImpl(itemMono);
    }

    private Mono<String> createImpl(Mono<com.example.Item> requestBodyMono) {
        Mono<ByteBuf> requestByteBufMono =
                requestBodyMono.map(it -> WriterUtils.toByteBuf(com.example.Item.Writer.INSTANCE, it));
        return httpClient
                .post()
                .uri("/")
                .send((httpClientRequest, nettyOutbound) -> nettyOutbound.send(requestByteBufMono))
                .responseSingle((httpClientResponse, responseByteBufMono) -> responseByteBufMono.asString());
    }

    public Mono<com.example.Item> get(String id) {
        return getImpl(id);
    }

    private Mono<com.example.Item> getImpl(String param0) {
        return httpClient
                .get()
                .uri("/" + param0)
                .response((httpClientResponse, byteBufFlux) ->
                        ReactorUtils.decode(byteBufFlux.asByteArray(), new com.example.Item.Parser())
                ).single();
    }
}
