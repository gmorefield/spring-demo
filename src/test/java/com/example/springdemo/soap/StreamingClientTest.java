package com.example.springdemo.soap;

import java.net.MalformedURLException;
import java.net.URL;

import javax.activation.DataHandler;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.jws.soap.SOAPBinding.Style;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;
import javax.xml.ws.Action;
import javax.xml.ws.Service;
import javax.xml.ws.soap.MTOMFeature;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.example.springdemo.soap.model.ObjectFactory;
import com.example.springdemo.soap.model.ReadStorageRecordRequest;
import com.example.springdemo.soap.model.ReadStorageRecordResponse;
import com.sun.xml.ws.developer.StreamingAttachmentFeature;
import com.sun.xml.ws.developer.StreamingDataHandler;

@Disabled("Manual test")
public class StreamingClientTest {

    @WebService(targetNamespace = "https://springdemo.example.com/soap")
    @SOAPBinding(style = Style.DOCUMENT, parameterStyle = ParameterStyle.BARE)
    @XmlSeeAlso({ ObjectFactory.class })
    public static interface StorageRecordPortService {
        @WebMethod()
        @WebResult(partName = "ReadStorageRecordResponse")
        @Action(input = "https://springdemo.example.com/soap/StorageRecordPort/ReadStorageRecordRequest", output = "https://springdemo.example.com/soap/StorageRecordPort/ReadStorageRecordResponse")
        ReadStorageRecordResponse ReadStorageRecord(
                @WebParam(targetNamespace = "https://springdemo.example.com/soap", name = "ReadStorageRecordRequest", partName = "ReadStorageRecordRequest") ReadStorageRecordRequest request);
    }

    @Test
    public void ClientTest() throws MalformedURLException {
        MTOMFeature mtom = new MTOMFeature();
        // Configure such that whole MIME message is parsed eagerly,
        // Attachments under 4MB are kept in memory
        StreamingAttachmentFeature stf = new StreamingAttachmentFeature("target", true, 4000000L);

        URL url = new URL("http://localhost:8080/ws/storageRecord.wsdl");
        // 1st argument service URI, refer to wsdl document above
        // 2nd argument is service name, refer to wsdl document above
        QName qname = new QName("https://springdemo.example.com/soap", "StorageRecordPortService");
        QName qnamePort = new QName("https://springdemo.example.com/soap", "StorageRecordPortSoap11");

        Service service = Service.create(url, qname);
        StorageRecordPortService readStorage = service.getPort(qnamePort, StorageRecordPortService.class, mtom, stf);

        // BindingProvider bp = (BindingProvider)readStorage;
        // bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
        // "http://localhost:8080/ws");

        ReadStorageRecordRequest request = new ReadStorageRecordRequest();
        request.setName("ideaIC-2022.3-aarch64.dmg");
        ReadStorageRecordResponse response = readStorage.ReadStorageRecord(request);
        DataHandler dh = response.getStorageRecord().getContent();
        Assertions.assertThat(dh).isInstanceOf(StreamingDataHandler.class);
        StreamingDataHandler sdh = (StreamingDataHandler) dh;
        IOUtils.closeQuietly(sdh);
        // MyService service = new MyService();
        // MyProxy proxy = service.getProxyPort(feature, stf);
    }
}
