package com.example.springdemo.controller;

import jakarta.validation.Valid;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("uml")
public class PlantUmlController {
    @PostMapping(path = {"generate/{format}", "generate"})
    public ResponseEntity<StreamingResponseBody> generateDiagram(@PathVariable(value = "format", required = false) @Valid FileFormat fileFormat) {
        String script = "@startuml;component one;@enduml";
        System.out.println("Generate UML for: " + script);

        final FileFormat formatToUse = (fileFormat == null) ? FileFormat.PNG : fileFormat;
        StreamingResponseBody body = outputStream -> {
            SourceStringReader reader = new SourceStringReader(script.replace(';', '\n'));
            reader.outputImage(outputStream, new FileFormatOption(formatToUse, false));
        };

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(formatToUse.getMimeType()))
                // .contentLength(resource.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("sample" + formatToUse.getFileSuffix())
                                .build().toString())
                .body(body);
    }

    @GetMapping(path = {"fonts/{format}","fonts"})
    public ResponseEntity<StreamingResponseBody> listFonts(@PathVariable(value = "format", required = false) @Valid FileFormat fileFormat) {
        String script = "@startuml;listfonts;@enduml";

        final FileFormat formatToUse = (fileFormat == null) ? FileFormat.PNG : fileFormat;
        StreamingResponseBody body = outputStream -> {
            SourceStringReader reader = new SourceStringReader(script.replace(';', '\n'));
            reader.outputImage(outputStream, new FileFormatOption(formatToUse, false));
        };

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(formatToUse.getMimeType()))
                .body(body);
    }
}
