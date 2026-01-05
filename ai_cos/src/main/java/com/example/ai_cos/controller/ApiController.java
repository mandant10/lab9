package com.example.ai_cos.controller;

import com.example.ai_cos.model.Client;
import com.example.ai_cos.model.Order;
import com.example.ai_cos.service.ChatService;
import com.example.ai_cos.service.DataGeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final ChatService chatService;
    private final DataGeneratorService dataGeneratorService;

    public ApiController(ChatService chatService, DataGeneratorService dataGeneratorService) {
        this.chatService = chatService;
        this.dataGeneratorService = dataGeneratorService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String response = chatService.chat(message);
        
        Map<String, String> result = new HashMap<>();
        result.put("response", response);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add-client")
    public ResponseEntity<Client> addRandomClient() {
        Client client = dataGeneratorService.generateRandomClient();
        return ResponseEntity.ok(client);
    }

    @PostMapping("/add-order")
    public ResponseEntity<Map<String, Object>> addRandomOrder() {
        Order order = dataGeneratorService.generateRandomOrder();
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", order.getId());
        result.put("productName", order.getProductName());
        result.put("quantity", order.getQuantity());
        result.put("price", order.getPrice());
        result.put("status", order.getStatus());
        result.put("orderDate", order.getOrderDate().toString());
        result.put("clientId", order.getClient().getId());
        result.put("clientName", order.getClient().getFirstName() + " " + order.getClient().getLastName());
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/clients")
    public ResponseEntity<List<Client>> getAllClients() {
        return ResponseEntity.ok(dataGeneratorService.getAllClients());
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Map<String, Object>>> getAllOrders() {
        List<Order> orders = dataGeneratorService.getAllOrders();
        List<Map<String, Object>> result = orders.stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("productName", order.getProductName());
            map.put("quantity", order.getQuantity());
            map.put("price", order.getPrice());
            map.put("status", order.getStatus());
            map.put("orderDate", order.getOrderDate().toString());
            map.put("clientId", order.getClient().getId());
            map.put("clientName", order.getClient().getFirstName() + " " + order.getClient().getLastName());
            return map;
        }).toList();
        
        return ResponseEntity.ok(result);
    }
}
