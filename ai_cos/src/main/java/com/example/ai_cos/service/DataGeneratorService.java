package com.example.ai_cos.service;

import com.example.ai_cos.model.Client;
import com.example.ai_cos.model.Order;
import com.example.ai_cos.model.OrderStatus;
import com.example.ai_cos.repository.ClientRepository;
import com.example.ai_cos.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Service
public class DataGeneratorService {

    private final ClientRepository clientRepository;
    private final OrderRepository orderRepository;
    private final Random random = new Random();

    private static final String[] FIRST_NAMES = {
            "Jan", "Anna", "Piotr", "Maria", "Krzysztof", "Katarzyna", "Andrzej", "Małgorzata",
            "Tomasz", "Agnieszka", "Marcin", "Barbara", "Paweł", "Ewa", "Michał", "Joanna"
    };

    private static final String[] LAST_NAMES = {
            "Nowak", "Kowalski", "Wiśniewski", "Wójcik", "Kowalczyk", "Kamiński", "Lewandowski",
            "Zieliński", "Szymański", "Woźniak", "Dąbrowski", "Kozłowski", "Jankowski", "Mazur"
    };

    private static final String[] PRODUCTS = {
            "Laptop", "Smartfon", "Tablet", "Słuchawki", "Klawiatura", "Mysz", "Monitor",
            "Drukarka", "Kamera", "Głośniki", "Powerbank", "Pendrive", "Dysk SSD", "Router"
    };

    public DataGeneratorService(ClientRepository clientRepository, OrderRepository orderRepository) {
        this.clientRepository = clientRepository;
        this.orderRepository = orderRepository;
    }

    public Client generateRandomClient() {
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + random.nextInt(1000) + "@email.pl";
        String phone = "+48" + (100000000 + random.nextInt(900000000));

        Client client = new Client(firstName, lastName, email, phone);
        client.setRegistrationDate(LocalDate.now().minusDays(random.nextInt(365)));

        return clientRepository.save(client);
    }

    public Order generateRandomOrder() {
        List<Client> clients = clientRepository.findAll();
        if (clients.isEmpty()) {
            generateRandomClient();
            clients = clientRepository.findAll();
        }

        Client randomClient = clients.get(random.nextInt(clients.size()));
        String productName = PRODUCTS[random.nextInt(PRODUCTS.length)];
        int quantity = 1 + random.nextInt(5);
        BigDecimal price = BigDecimal.valueOf(50 + random.nextInt(2000));
        
        Order order = new Order(productName, quantity, price, randomClient);
        
        OrderStatus[] statuses = OrderStatus.values();
        order.setStatus(statuses[random.nextInt(statuses.length)]);

        return orderRepository.save(order);
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
