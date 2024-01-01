package com.example.springdemo.soap;

import com.example.springdemo.soap.model.ObjectFactory;
import com.example.springdemo.soap.model.ReadStorageRecordRequest;
import com.example.springdemo.soap.model.ReadStorageRecordResponse;
import com.example.springdemo.soap.model.SaveStorageRecordRequest;
import com.example.springdemo.soap.model.SaveStorageRecordResponse;
import com.example.springdemo.soap.model.StorageRecord;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Endpoint
public class StorageEndpoint {

	private static final String NAMESPACE_URI = "https://springdemo.example.com/soap";

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public StorageEndpoint(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "ReadStorageRecordRequest")
	@ResponsePayload
	public ReadStorageRecordResponse readRecord(
			@RequestPayload ReadStorageRecordRequest request) {

				ObjectFactory of = new ObjectFactory();
				
		StorageRecord record = of.createStorageRecord();
		record.setName("test");
		record.setContentType("application/octet-stream");
		FileDataSource fds = new FileDataSource(new File("target/" + request.getName()));
		record.setContent(new DataHandler(fds));
		ReadStorageRecordResponse response = of.createReadStorageRecordResponse();
		response.setStorageRecord(record);
		return response;
	}


	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "SaveStorageRecordRequest")
	@ResponsePayload
	@Transactional
	public SaveStorageRecordResponse storageRecord(
			@RequestPayload SaveStorageRecordRequest request) throws IOException, NoSuchAlgorithmException {

				SaveStorageRecordResponse response = new SaveStorageRecordResponse();
				response.setSuccess(false);
		
		MessageDigest md = MessageDigest.getInstance("sha1");
		StorageRecord record = request.getStorageRecord();
		try (InputStream in = record.getContent().getInputStream()) {
			DigestInputStream dis = new DigestInputStream(in, md);
			CountingInputStream cis = new CountingInputStream(dis);

			Map<String, Object> params = new HashMap<>();
			params.put("id", UUID.randomUUID().toString());
			params.put("contentType", record.getContentType());
			params.put("fileName", record.getName());
			params.put("docBin", cis);
			params.put("createDt", LocalDateTime.now());

			int rowsUpdated = jdbcTemplate.update(
					"insert into DOCUMENT (ID, Create_Dt, Doc_Bin, Content_Type, File_Nm) values (:id, :createDt, :docBin, :contentType, :fileName)",
					new MapSqlParameterSource(params));

			// cis.close();
			// dis.close();
			params.put("checksum", new BigInteger(1, md.digest()).toString(16));
			System.out.println("checksum:"+ params.get("checksum"));
			System.out.println("checksum2:" + new String(Base64.encodeBase64(dis.getMessageDigest().digest(),false)));
			params.put("contentLen", cis.getByteCount());
			jdbcTemplate.update("update DOCUMENT set checksum = :checksum, content_len = :contentLen WHERE id=:id",
					new MapSqlParameterSource(params));
			System.out.printf("received %d bytes. stored id %s%n", cis.getByteCount(), params.get("id"));
			response.setSha1((String)params.get("checksum"));
		}
		response.setSuccess(true);
		return response;
	}

}