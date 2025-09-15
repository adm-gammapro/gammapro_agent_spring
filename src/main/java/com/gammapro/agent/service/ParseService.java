package com.gammapro.agent.service;

public interface ParseService {
    String parseTxt(byte[] bytes);

    String parseDocx(byte[] bytes) throws Exception;

    String parsePdf(byte[] bytes) throws Exception;

    String parseByName(String filename, byte[] bytes) throws Exception;
}
