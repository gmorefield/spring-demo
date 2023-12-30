package com.example.springdemo.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;

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

    @Column("CHECKSUM")
    private String checksum;
}