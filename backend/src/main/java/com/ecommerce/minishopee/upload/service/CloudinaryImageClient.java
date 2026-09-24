package com.ecommerce.minishopee.upload.service;

import java.io.IOException;
import java.util.Map;

public interface CloudinaryImageClient {
    Map<String, Object> upload(byte[] content, Map<String, Object> options) throws IOException;
}
