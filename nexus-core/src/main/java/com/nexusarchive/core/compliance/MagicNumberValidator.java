// Input: 文件头字节 (Magic Number)
// Output: 文件类型校验结果
// Pos: NexusCore compliance/validator
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class MagicNumberValidator {
    private static final byte[] MAGIC_PDF = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D};  // %PDF-
    private static final byte[] MAGIC_ZIP = new byte[]{0x50, 0x4B, 0x03, 0x04};        // PK.. (OFD/ZIP)
    private static final byte[] MAGIC_XML = new byte[]{0x3C, 0x3F, 0x78, 0x6D, 0x6C};  // <?xml
    private static final byte[] MAGIC_JPEG = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] MAGIC_PNG = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};

    private static final int MAX_HEADER_SIZE = 8;

    public FileType detectFileType(Path filePath) throws IOException {
        Objects.requireNonNull(filePath, "filePath must not be null");
        byte[] header = readHeader(filePath);
        return detectFromHeader(header);
    }

    public FileType detectFileType(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        byte[] header = new byte[MAX_HEADER_SIZE];
        int read = inputStream.read(header);
        if (read < 3) {
            return FileType.UNKNOWN;
        }
        return detectFromHeader(Arrays.copyOf(header, read));
    }

    public boolean validate(Path filePath, String expectedExtension) throws IOException {
        FileType detected = detectFileType(filePath);
        return detected.matchesExtension(expectedExtension);
    }

    private byte[] readHeader(Path filePath) throws IOException {
        try (InputStream is = Files.newInputStream(filePath)) {
            byte[] header = new byte[MAX_HEADER_SIZE];
            int read = is.read(header);
            return read > 0 ? Arrays.copyOf(header, read) : new byte[0];
        }
    }

    private FileType detectFromHeader(byte[] header) {
        if (startsWith(header, MAGIC_PDF)) {
            return FileType.PDF;
        }
        if (startsWith(header, MAGIC_ZIP)) {
            return FileType.OFD;
        }
        if (startsWith(header, MAGIC_XML)) {
            return FileType.XML;
        }
        if (startsWith(header, MAGIC_JPEG)) {
            return FileType.JPEG;
        }
        if (startsWith(header, MAGIC_PNG)) {
            return FileType.PNG;
        }
        return FileType.UNKNOWN;
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    public enum FileType {
        PDF("pdf", "application/pdf"),
        OFD("ofd", "application/ofd"),
        XML("xml", "application/xml"),
        JPEG("jpg", "image/jpeg"),
        PNG("png", "image/png"),
        UNKNOWN("", "application/octet-stream");

        private final String extension;
        private final String mimeType;

        FileType(String extension, String mimeType) {
            this.extension = extension;
            this.mimeType = mimeType;
        }

        public String getExtension() {
            return extension;
        }

        public String getMimeType() {
            return mimeType;
        }

        public boolean matchesExtension(String ext) {
            if (ext == null || ext.isBlank()) {
                return false;
            }
            String normalized = ext.toLowerCase().replaceFirst("^\\.", "");
            return extension.equals(normalized) 
                    || (this == OFD && "zip".equals(normalized))
                    || (this == JPEG && "jpeg".equals(normalized));
        }
    }
}
