package com.gammapro.agent.service;

import com.gammapro.agent.service.impl.SftpServiceImpl;

import java.util.List;

public interface SftpService {
    List<SftpServiceImpl.FileData> fetchFiles(List<String> paths) throws Exception;
}
