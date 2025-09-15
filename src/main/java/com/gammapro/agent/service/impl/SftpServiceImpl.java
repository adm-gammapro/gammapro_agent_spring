package com.gammapro.agent.service.impl;

import com.gammapro.agent.service.SftpService;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
public class SftpServiceImpl implements SftpService {
    @Value("${app.sftp.host}") private String defaultHost;
    @Value("${app.sftp.port}") private int defaultPort;
    @Value("${app.sftp.username}") private String defaultUser;
    @Value("${app.sftp.password:}") private String defaultPass;
    @Value("${app.sftp.privateKey:}") private String defaultKey;

    public List<FileData> fetchFiles(List<String> paths) throws Exception {
        validateCredentials(defaultHost, defaultUser, defaultPass, defaultKey);

        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.connect(defaultHost, defaultPort);
            try {
                if (defaultKey != null && !defaultKey.isBlank()) {
                    ssh.authPublickey(defaultUser, ssh.loadKeys(defaultKey, null, null));
                } else {
                    ssh.authPassword(defaultUser, defaultPass);
                }

                try (SFTPClient sftp = ssh.newSFTPClient()) {
                    List<FileData> out = new ArrayList<>();
                    for (String p : paths) {
                        String normalized = p.replace('\\', '/');
                        try (RemoteFile rf = sftp.open(normalized, EnumSet.of(OpenMode.READ))) {
                            byte[] bytes = readAllBytes(rf); // usa la variante A o B según tu firma
                            out.add(new FileData(filenameFromPath(normalized), bytes));
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                    return out;
                }
            } finally {
                ssh.disconnect();
            }
        }
    }

    private static byte[] readAllBytes(RemoteFile rf) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        long offset = 0;
        byte[] buf = new byte[32 * 1024];
        while (true) {
            int n = rf.read(offset, buf, 0, buf.length);
            if (n <= 0) break;
            bos.write(buf, 0, n);
            offset += n;
        }
        return bos.toByteArray();
    }

    private String filenameFromPath(String path){
        int i = path.lastIndexOf('/');
        return i>=0 ? path.substring(i+1) : path;
    }

    private void validateCredentials(String host, String user, String password, String privateKey) {
        boolean hasPwd = password != null && !password.isBlank();
        boolean hasKey = privateKey != null && !privateKey.isBlank();

        if (isBlank(host) || isBlank(user) || (!hasPwd && !hasKey)) {
            throw new IllegalArgumentException("Credenciales SFTP incompletas (host, user y password o privateKey)");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public record FileData(String filename, byte[] bytes) {}
}
