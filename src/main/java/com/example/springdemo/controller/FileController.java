package com.example.springdemo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileController {
  
    @PostMapping("/document/store")
    public String storeDocument(@RequestPart("file") MultipartFile multipartFile) {
        return multipartFile.toString();
    }

    @PostMapping("/document/storeWithMeta")
    public String storeDocument(@RequestPart("meta") String info, @RequestPart("file") MultipartFile multipartFile) {
        return info + ":" + multipartFile.toString();
    }
}
