package com.example.springdemo.data;

import com.example.springdemo.model.Document;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentPagingAndSortingRepository extends PagingAndSortingRepository<Document, String> {
    Optional<Document> findById(String id);
    List<Document> findByFileName(String fileName);
}
