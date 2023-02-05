package com.example.springdemo.soap;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import com.example.springdemo.soap.model.SaveStorageRecordRequest;
import com.example.springdemo.soap.model.SaveStorageRecordResponse;
import com.example.springdemo.soap.model.StorageRecord;

@Endpoint
public class StorageEndpoint {

	private static final String NAMESPACE_URI = "https://springdemo.example.com/soap";

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "SaveStorageRecordRequest")
	@ResponsePayload
	public SaveStorageRecordResponse storageRecord(
			@RequestPayload SaveStorageRecordRequest request) throws IOException, NoSuchAlgorithmException {

		MessageDigest md = MessageDigest.getInstance("sha1");
		StorageRecord record = request.getStorageRecord();
		try (InputStream in = record.getContent().getInputStream()) {
			DigestInputStream dis = new DigestInputStream(in, md);
			long counter = 0;
			while (dis.read() != -1) {
				++counter;
			}
			System.out.println(String.format("received %d bytes", counter));
		}
		SaveStorageRecordResponse response = new SaveStorageRecordResponse();
		response.setSuccess(true);
		response.setSha1(new BigInteger(1, md.digest()).toString(16));
		return response;
	}

}