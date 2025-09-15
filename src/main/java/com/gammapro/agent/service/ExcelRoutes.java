package com.gammapro.agent.service;

import java.util.List;

public interface ExcelRoutes {
    List<String> readRoutes(byte[] excelBytes) throws Exception;
}
