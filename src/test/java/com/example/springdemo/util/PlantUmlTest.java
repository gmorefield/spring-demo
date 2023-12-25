package com.example.springdemo.util;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PlantUmlTest {
    @Test
    public void firstSample() throws IOException {
        String script = "@startuml;actor Kaka;@enduml";
        System.out.println("Generate UML for: "+script);
        SourceStringReader reader = new SourceStringReader(script.replace(';','\n'));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileOutputStream fos = new FileOutputStream(new File("/Users/morefigs/Downloads/plantuml-sample.png"));
        reader.outputImage(fos, new FileFormatOption(FileFormat.PNG, false));
//        return bos.toByteArray();
    }
}
