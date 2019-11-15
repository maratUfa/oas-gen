package com.example;

import java.util.function.Consumer;
import jsm.ReactorUtils;
import jsm.WriterUtils;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRoutes;

public interface SimpleRoutes extends Consumer<HttpServerRoutes> {

    Mono<String> create(Mono<com.example.Item> requestBodyMono);

    Mono<com.example.Item> get(String id);

    @Override
    default void accept(HttpServerRoutes httpServerRoutes) {
        httpServerRoutes
            .post("/", (request, response) -> {

                Mono<com.example.Item> requestMono = ReactorUtils.decode(request.receive().asByteArray(), new com.example.Item.Parser());
                Mono<String> responseMono = create(requestMono);
                return response.sendString(responseMono);
            })
            .get("/{id}", (request, response) -> {
                String param0 = request.param("id");

                Mono<com.example.Item> responseMono = get(param0);
                return response.send(responseMono.map(it -> WriterUtils.toByteBuf(com.example.Item.Writer.INSTANCE, it)));
            })
        ;
    }
}
