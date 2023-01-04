package com.example.springdemo.data;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.example.springdemo.model.Document;

@Repository
public interface DocumentPagingAndSortingRepository extends PagingAndSortingRepository<Document, String> {
    List<Document> findByFileName(String fileName);
}
