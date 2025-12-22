// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: ClamAvAdapter 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.adapter.impl;

import com.nexusarchive.service.adapter.VirusScanAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * ClamAV 病毒扫描适配器 (Production Ready)
 * 通过 TCP 协议直接与 ClamAV 守护进程通信
 * 
 * 配置要求:
 * virus.scan.type=clamav
 * virus.scan.clamav.host=localhost (默认)
 * virus.scan.clamav.port=3310 (默认)
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "virus.scan.type", havingValue = "clamav")
public class ClamAvAdapter implements VirusScanAdapter {

    @Value("${virus.scan.clamav.host:localhost}")
    private String host;

    @Value("${virus.scan.clamav.port:3310}")
    private int port;

    private static final int CHUNK_SIZE = 2048;
    private static final byte[] INSTREAM_CMD = "zINSTREAM\0".getBytes(StandardCharsets.US_ASCII);

    @Override
    public boolean scan(InputStream inputStream, String fileName) {
        if (inputStream == null) {
            return true;
        }

        log.info("Initiating ClamAV stream-based scan for file: {}", fileName);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000); // 2s timeout
            socket.setSoTimeout(5000); // 5s read timeout

            try (OutputStream out = new BufferedOutputStream(socket.getOutputStream());
                 InputStream in = socket.getInputStream()) {

                // 1. Send Command
                out.write(INSTREAM_CMD);
                out.flush();

                // 2. Stream Data
                byte[] buffer = new byte[CHUNK_SIZE];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    // Write length (4 bytes big endian)
                    out.write(intToBytes(read));
                    // Write data
                    out.write(buffer, 0, read);
                }
                
                // Write 0 length to signal end
                out.write(new byte[]{0, 0, 0, 0});
                out.flush();

                // 3. Read Response
                String response = readResponse(in);
                log.debug("ClamAV Response for {}: {}", fileName, response);

                if (response.trim().endsWith("FOUND")) {
                    log.warn("VIRUS DETECTED in file {}: {}", fileName, response);
                    return false;
                } else if (!response.trim().endsWith("OK")) {
                    log.error("Unknown ClamAV response: {}", response);
                    return false; // Fail secure
                }

                return true;
            }
        } catch (IOException e) {
            log.error("ClamAV scan failed for {}: {}", fileName, e.getMessage());
            return false;
        }
    }

    private byte[] intToBytes(int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }

    private String readResponse(InputStream in) throws IOException {
        StringBuilder result = new StringBuilder();
        byte[] buffer = new byte[1024];
        int read = in.read(buffer);
        if (read > 0) {
            result.append(new String(buffer, 0, read, StandardCharsets.US_ASCII));
        }
        return result.toString();
    }
}
