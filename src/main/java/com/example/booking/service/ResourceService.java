package com.example.booking.service;

import com.example.booking.dto.ResourceRequest;
import com.example.booking.dto.ResourceResponse;
import com.example.booking.entity.Resource;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.ResourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceService {
    private final ResourceRepository repository;
    public ResourceService(ResourceRepository repository) { this.repository = repository; }

    public List<ResourceResponse> getAll() { return repository.findAll().stream().map(ResourceResponse::from).toList(); }

    public ResourceResponse getById(Long id) { return ResourceResponse.from(find(id)); }

    public ResourceResponse create(ResourceRequest request) {
        Resource r = new Resource();
        apply(r, request);
        return ResourceResponse.from(repository.save(r));
    }

    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource r = find(id);
        apply(r, request);
        return ResourceResponse.from(repository.save(r));
    }

    public void delete(Long id) { repository.delete(find(id)); }

    private Resource find(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id)); }
    private void apply(Resource r, ResourceRequest q) {
        r.setName(q.name()); r.setDescription(q.description()); r.setType(q.type()); r.setPrice(q.price()); r.setAvailable(q.available());
    }
}
