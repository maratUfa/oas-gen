package com.example;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

public class ClientMain {
    public static void main(String[] args) {

        HttpClient httpClient = HttpClient.create()
                .tcpConfiguration(tcpClient -> tcpClient.host("localhost"))
                .port(8080);
        Client client = new Client(httpClient);
        Mono<String> idMono = client.create(Mono.just(new Item("value1", new ItemProperty2("value21", "value22"))));
        String id = idMono.block();
        System.out.println(id);

        Mono<Item> itemMono = client.get("id");
        Item item = itemMono.block();
        System.out.println(item);
    }
}
