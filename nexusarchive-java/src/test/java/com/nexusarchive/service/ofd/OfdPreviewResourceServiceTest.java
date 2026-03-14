package com.nexusarchive.service.ofd;

import com.nexusarchive.dto.OfdPreviewResourceResponse;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.OriginalVoucherFile;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.OriginalVoucherFileMapper;
import com.nexusarchive.service.FileStorageService;
import com.nexusarchive.service.ofd.impl.OfdPreviewResourceServiceImpl;
import com.nexusarchive.service.preview.PreviewFilePathResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class OfdPreviewResourceServiceTest {

    @Mock
    private PreviewFilePathResolver previewFilePathResolver;

    @Mock
    private ArcFileContentMapper arcFileContentMapper;

    @Mock
    private OriginalVoucherFileMapper originalVoucherFileMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Test
    void prefersConvertedArchivePdfWhenSiblingRecordExists(@TempDir Path tempDir) throws Exception {
        Path ofdPath = tempDir.resolve("invoice.ofd");
        Path pdfPath = tempDir.resolve("invoice.pdf");
        Files.writeString(ofdPath, "ofd");
        Files.writeString(pdfPath, "pdf");

        when(previewFilePathResolver.resolveFileById("ofd-file")).thenReturn(
            new PreviewFilePathResolver.ResolvedPreviewFile(
                "file",
                "ofd-file",
                "ofd-file",
                "archive/2026/invoice.ofd",
                "invoice.ofd",
                "ofd"
            )
        );
        ArcFileContent converted = ArcFileContent.builder()
            .id("pdf-file")
            .itemId("archive-1")
            .fileName("invoice.pdf")
            .fileType("pdf")
            .storagePath("archive/2026/invoice.pdf")
            .build();
        when(arcFileContentMapper.selectById("ofd-file")).thenReturn(
            ArcFileContent.builder()
                .id("ofd-file")
                .itemId("archive-1")
                .fileName("invoice.ofd")
                .fileType("ofd")
                .storagePath("archive/2026/invoice.ofd")
                .build()
        );
        when(arcFileContentMapper.selectList(any())).thenReturn(List.of(converted));

        OfdPreviewResourceService service = new OfdPreviewResourceServiceImpl(
            previewFilePathResolver,
            arcFileContentMapper,
            originalVoucherFileMapper,
            fileStorageService
        );

        OfdPreviewResourceResponse response = service.resolve("ofd-file");

        assertEquals("converted", response.getPreferredMode());
        assertEquals("pdf-file", response.getConvertedFileId());
        assertEquals("application/pdf", response.getConvertedMimeType());
        assertEquals("/api/preview?resourceType=file&fileId=pdf-file&mode=stream", response.getConvertedPreviewUrl());
        assertEquals("/api/archive/files/download/ofd-file", response.getOriginalDownloadUrl());
    }

    @Test
    void fallsBackToLiteOfdWhenNoConvertedSiblingExists(@TempDir Path tempDir) throws Exception {
        Path ofdPath = tempDir.resolve("invoice.ofd");
        Files.writeString(ofdPath, "ofd");

        when(previewFilePathResolver.resolveFileById("orig-ofd")).thenReturn(
            new PreviewFilePathResolver.ResolvedPreviewFile(
                "file",
                "orig-ofd",
                "orig-ofd",
                "original-vouchers/voucher-1/invoice.ofd",
                "invoice.ofd",
                "ofd"
            )
        );
        when(fileStorageService.resolvePath("original-vouchers/voucher-1/invoice.ofd")).thenReturn(ofdPath);
        when(arcFileContentMapper.selectById("orig-ofd")).thenReturn(null);
        when(originalVoucherFileMapper.selectById("orig-ofd")).thenReturn(
            OriginalVoucherFile.builder()
                .id("orig-ofd")
                .voucherId("voucher-1")
                .fileName("invoice.ofd")
                .fileType("ofd")
                .storagePath("original-vouchers/voucher-1/invoice.ofd")
                .build()
        );
        when(originalVoucherFileMapper.findByVoucherId("voucher-1")).thenReturn(List.of(
            OriginalVoucherFile.builder()
                .id("orig-ofd")
                .voucherId("voucher-1")
                .fileName("invoice.ofd")
                .fileType("ofd")
                .storagePath("original-vouchers/voucher-1/invoice.ofd")
                .build()
        ));

        OfdPreviewResourceService service = new OfdPreviewResourceServiceImpl(
            previewFilePathResolver,
            arcFileContentMapper,
            originalVoucherFileMapper,
            fileStorageService
        );

        OfdPreviewResourceResponse response = service.resolve("orig-ofd");

        assertEquals("liteofd", response.getPreferredMode());
        assertNull(response.getConvertedFileId());
        assertNull(response.getConvertedPreviewUrl());
        assertEquals("/api/original-vouchers/files/download/orig-ofd", response.getOriginalDownloadUrl());
    }

    @Test
    void keepsOriginalDownloadWhenConvertedImageExists(@TempDir Path tempDir) throws Exception {
        Path ofdPath = tempDir.resolve("invoice.ofd");
        Path pngPath = tempDir.resolve("invoice.png");
        Files.writeString(ofdPath, "ofd");
        Files.writeString(pngPath, "png");

        when(previewFilePathResolver.resolveFileById("orig-ofd")).thenReturn(
            new PreviewFilePathResolver.ResolvedPreviewFile(
                "file",
                "orig-ofd",
                "orig-ofd",
                "original-vouchers/voucher-2/invoice.ofd",
                "invoice.ofd",
                "ofd"
            )
        );
        when(arcFileContentMapper.selectById("orig-ofd")).thenReturn(null);
        when(originalVoucherFileMapper.selectById("orig-ofd")).thenReturn(
            OriginalVoucherFile.builder()
                .id("orig-ofd")
                .voucherId("voucher-2")
                .fileName("invoice.ofd")
                .fileType("ofd")
                .storagePath("original-vouchers/voucher-2/invoice.ofd")
                .build()
        );
        when(originalVoucherFileMapper.findByVoucherId("voucher-2")).thenReturn(List.of(
            OriginalVoucherFile.builder()
                .id("orig-ofd")
                .voucherId("voucher-2")
                .fileName("invoice.ofd")
                .fileType("ofd")
                .storagePath("original-vouchers/voucher-2/invoice.ofd")
                .build(),
            OriginalVoucherFile.builder()
                .id("png-file")
                .voucherId("voucher-2")
                .fileName("invoice.png")
                .fileType("png")
                .storagePath("original-vouchers/voucher-2/invoice.png")
                .build()
        ));

        OfdPreviewResourceService service = new OfdPreviewResourceServiceImpl(
            previewFilePathResolver,
            arcFileContentMapper,
            originalVoucherFileMapper,
            fileStorageService
        );

        OfdPreviewResourceResponse response = service.resolve("orig-ofd");

        assertEquals("converted", response.getPreferredMode());
        assertEquals("png-file", response.getConvertedFileId());
        assertEquals("image/png", response.getConvertedMimeType());
        assertEquals("/api/original-vouchers/files/download/png-file", response.getConvertedPreviewUrl());
        assertEquals("/api/original-vouchers/files/download/orig-ofd", response.getOriginalDownloadUrl());
    }
}
