package com.example.springdemo.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

import lombok.Data;

@Data
public class Document {
    @Id
    private String id;
    
    @Column("CONTENT_TYPE")
    private String contentType;
    
    @Column("CONTENT_LEN")
    private long contentLength;
    
    @Column("FILE_NM")
    private String fileName;
    
    @Column("CREATE_DT")
    private LocalDateTime createDateTime;
}