package com.example;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class SimpleRoutersImpl implements SimpleRoutes {

    @Override
    public ResponseEntity<String> create(Item item) {
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<Item> get(String id) {
        Item item = new Item("property1", new ItemProperty2("property21", "property22"));
        return ResponseEntity.ok(item);
    }
}
