package com.example.springdemo.controller;

import jakarta.validation.Valid;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("uml")
public class PlantUmlController {
    @PostMapping(path = "generate/{format}")
    public ResponseEntity<StreamingResponseBody> generateDiagram(@PathVariable("format") @Valid FileFormat fileFormat) {
        String script = "@startuml;component one;@enduml";
        System.out.println("Generate UML for: " + script);

        StreamingResponseBody body = outputStream -> {
            SourceStringReader reader = new SourceStringReader(script.replace(';', '\n'));
            reader.outputImage(outputStream, new FileFormatOption(fileFormat, false));
        };

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(fileFormat.getMimeType()))
                // .contentLength(resource.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("sample" + fileFormat.getFileSuffix())
                                .build().toString())
                .body(body);
    }
}
