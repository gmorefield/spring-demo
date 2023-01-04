package com.example.springdemo.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

import lombok.Data;

@Data
public class Document {
    @Id
    private String id;
    
    @Column("Content_Type")
    private String contentType;
    
    @Column("Content_Len")
    private long contentLength;
    
    @Column("File_Nm")
    private String fileName;
    
    @Column("Create_Dt")
    private LocalDateTime createDateTime;
}