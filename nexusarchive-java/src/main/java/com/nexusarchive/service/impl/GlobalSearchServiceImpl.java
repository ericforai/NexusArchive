package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.GlobalSearchDTO;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcFileMetadataIndex;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArcFileMetadataIndexMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalSearchServiceImpl implements GlobalSearchService {

    private final ArchiveMapper archiveMapper;
    private final ArcFileMetadataIndexMapper metadataIndexMapper;
    private final ArcFileContentMapper fileContentMapper;

    @Override
    public List<GlobalSearchDTO> search(String query) {
        if (!StringUtils.hasText(query)) {
            return Collections.emptyList();
        }

        String keyword = query.trim();
        List<GlobalSearchDTO> results = new ArrayList<>();

        // 1. Search Archive Table
        searchArchives(keyword, results);

        // 2. Search Metadata Index Table
        searchMetadata(keyword, results);

        // Deduplicate results based on Archive ID
        return results.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(GlobalSearchDTO::getId))),
                        ArrayList::new
                ));
    }

    private void searchArchives(String keyword, List<GlobalSearchDTO> results) {
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Archive::getArchiveCode, keyword)
                .or().like(Archive::getTitle, keyword)
                .or().like(Archive::getFondsNo, keyword)
                .last("LIMIT 20"); // Limit to prevent too many results

        List<Archive> archives = archiveMapper.selectList(wrapper);
        for (Archive archive : archives) {
            results.add(new GlobalSearchDTO(
                    archive.getId(),
                    archive.getArchiveCode(),
                    archive.getTitle(),
                    "ARCHIVE",
                    "Matched Archive Info"
            ));
        }
    }

    private void searchMetadata(String keyword, List<GlobalSearchDTO> results) {
        LambdaQueryWrapper<ArcFileMetadataIndex> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(ArcFileMetadataIndex::getInvoiceCode, keyword)
                .or().like(ArcFileMetadataIndex::getInvoiceNumber, keyword)
                .or().like(ArcFileMetadataIndex::getSellerName, keyword)
                .or().eq(ArcFileMetadataIndex::getTotalAmount, tryParseDecimal(keyword)) // Exact match for amount
                .last("LIMIT 20");

        List<ArcFileMetadataIndex> metadataList = metadataIndexMapper.selectList(wrapper);
        
        // Collect File IDs
        Set<String> fileIds = metadataList.stream()
                .map(ArcFileMetadataIndex::getFileId)
                .collect(Collectors.toSet());

        if (fileIds.isEmpty()) {
            return;
        }

        // Resolve File ID -> Archival Code
        LambdaQueryWrapper<ArcFileContent> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.in(ArcFileContent::getId, fileIds);
        List<ArcFileContent> files = fileContentMapper.selectList(fileWrapper);

        Map<String, String> fileIdToArchivalCodeMap = files.stream()
                .collect(Collectors.toMap(ArcFileContent::getId, ArcFileContent::getArchivalCode));
        
        Set<String> archivalCodes = new HashSet<>(fileIdToArchivalCodeMap.values());

        if (archivalCodes.isEmpty()) {
            return;
        }

        // Resolve Archival Code -> Archive
        LambdaQueryWrapper<Archive> archiveWrapper = new LambdaQueryWrapper<>();
        archiveWrapper.in(Archive::getArchiveCode, archivalCodes);
        List<Archive> archives = archiveMapper.selectList(archiveWrapper);
        Map<String, Archive> archiveMap = archives.stream()
                .collect(Collectors.toMap(Archive::getArchiveCode, a -> a));

        // Build Results
        for (ArcFileMetadataIndex meta : metadataList) {
            String archivalCode = fileIdToArchivalCodeMap.get(meta.getFileId());
            if (archivalCode != null) {
                Archive archive = archiveMap.get(archivalCode);
                if (archive != null) {
                    String detail = String.format("Invoice: %s, Amount: %s", 
                            meta.getInvoiceNumber(), meta.getTotalAmount());
                    results.add(new GlobalSearchDTO(
                            archive.getId(),
                            archive.getArchiveCode(),
                            archive.getTitle(),
                            "METADATA",
                            detail
                    ));
                }
            }
        }
    }

    private java.math.BigDecimal tryParseDecimal(String val) {
        try {
            return new java.math.BigDecimal(val);
        } catch (Exception e) {
            return null;
        }
    }
}
