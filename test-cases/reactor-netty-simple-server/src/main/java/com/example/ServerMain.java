package com.example;

import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

public class ServerMain {
    public static void main(String[] args) throws InterruptedException {
        DisposableServer httpServer = HttpServer.create()
                .port(8080)
                .route(new SimpleRoutes() {
                    @Override
                    public Mono<String> create(Mono<Item> itemMono) {
                        return itemMono.map(it -> it.property1 + " " + it.property2.property21 + " " + it.property2.property22);
                    }

                    @Override
                    public Mono<Item> get(String id) {
                        return Mono.just(new Item("property1", new ItemProperty2("property21", "property22")));
                    }
                }).bindNow();

        Thread.sleep(1000000);
    }
}
