package com.example.swagger_center.store;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemorySpecStore implements SpecStore {

    private final ConcurrentHashMap<String, StoredSpec> store = new ConcurrentHashMap<>();

    @Override
    public void save(StoredSpec spec) {
        store.put(spec.serviceName(), spec);
    }

    @Override
    public Optional<StoredSpec> findByServiceName(String serviceName) {
        return Optional.ofNullable(store.get(serviceName));
    }

    @Override
    public List<StoredSpec> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public boolean delete(String serviceName) {
        return store.remove(serviceName) != null;
    }
}
