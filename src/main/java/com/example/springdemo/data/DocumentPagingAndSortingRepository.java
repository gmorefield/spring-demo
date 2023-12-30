package com.example.springdemo.data;

import com.example.springdemo.model.Document;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentPagingAndSortingRepository extends PagingAndSortingRepository<Document, String> {
    List<Document> findByFileName(String fileName);
}
