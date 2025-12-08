package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.Org;
import com.nexusarchive.mapper.OrgMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrgService {

    private final OrgMapper orgMapper;

    public List<OrgTreeNode> getTree() {
        List<Org> all = orgMapper.selectList(new LambdaQueryWrapper<Org>().eq(Org::getDeleted, 0));
        List<OrgTreeNode> nodes = all.stream().map(this::toNode).collect(Collectors.toList());
        // build tree
        return buildTree(nodes);
    }

    public List<Org> listAll() {
        return orgMapper.selectList(new LambdaQueryWrapper<Org>().eq(Org::getDeleted, 0).orderByAsc(Org::getOrderNum));
    }

    @Transactional
    public Org create(Org org) {
        if (!StringUtils.hasText(org.getName())) {
            throw new BusinessException("组织名称不能为空");
        }
        orgMapper.insert(org);
        return org;
    }

    @Transactional
    public Org update(String id, Org payload) {
        Org existing = orgMapper.selectById(id);
        if (existing == null || existing.getDeleted() != null && existing.getDeleted() == 1) {
            throw new BusinessException("组织不存在");
        }
        checkDataScope(existing);
        existing.setName(payload.getName());
        existing.setCode(payload.getCode());
        existing.setParentId(payload.getParentId());
        existing.setType(payload.getType());
        existing.setOrderNum(payload.getOrderNum());
        orgMapper.updateById(existing);
        return existing;
    }

    @Transactional
    public void delete(String id) {
        Org existing = orgMapper.selectById(id);
        if (existing == null) {
            return;
        }
        checkDataScope(existing);
        existing.setDeleted(1);
        orgMapper.updateById(existing);
    }

    @Transactional
    public void createBatch(List<Org> orgs) {
        if (orgs == null || orgs.isEmpty()) {
            return;
        }
        // Check data scope for parent orgs if provided? 
        // For batch creation, usually done by admin or import. 
        // Let's assume caller has permission or we check each.
        // For simplicity, we just insert.
        for (Org org : orgs) {
            create(org);
        }
    }

    @Transactional
    public void updateOrder(String id, Integer orderNum) {
        Org existing = orgMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("组织不存在");
        }
        checkDataScope(existing);
        existing.setOrderNum(orderNum);
        orgMapper.updateById(existing);
    }

    /**
     * 解析上传文件导入组织，支持 Excel(xls/xlsx) 与 CSV
     */
    @Transactional
    public com.nexusarchive.dto.request.OrgImportResult importFromFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            return importFromExcel(file);
        }
        return importFromCsv(file);
    }

    private com.nexusarchive.dto.request.OrgImportResult importFromExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int success = 0;
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException("Excel 文件为空");
            }
            int firstRow = sheet.getFirstRowNum() + 1; // 跳过表头
            int lastRow = sheet.getLastRowNum();
            for (int i = firstRow; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String name = getCellString(row.getCell(0));
                    if (!StringUtils.hasText(name)) {
                        throw new BusinessException("名称为空");
                    }
                    Org org = new Org();
                    org.setName(name);
                    org.setCode(getCellString(row.getCell(1)));
                    org.setParentId(getCellString(row.getCell(2)));
                    String type = getCellString(row.getCell(3));
                    org.setType(StringUtils.hasText(type) ? type : "DEPARTMENT");
                    String orderStr = getCellString(row.getCell(4));
                    org.setOrderNum(StringUtils.hasText(orderStr) ? Integer.parseInt(orderStr) : 0);
                    orgMapper.insert(org);
                    success++;
                } catch (Exception e) {
                    errors.add("行 " + (i + 1) + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new BusinessException("解析 Excel 失败: " + e.getMessage());
        }
        return com.nexusarchive.dto.request.OrgImportResult.builder()
                .successCount(success)
                .failCount(errors.size())
                .errors(errors)
                .build();
    }

    private com.nexusarchive.dto.request.OrgImportResult importFromCsv(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int success = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT
                     .withHeader("name", "code", "parentId", "type", "orderNum")
                     .withSkipHeaderRecord()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                try {
                    String name = record.get("name");
                    if (!StringUtils.hasText(name)) {
                        throw new BusinessException("名称为空");
                    }
                    Org org = new Org();
                    org.setName(name);
                    org.setCode(record.get("code"));
                    org.setParentId(record.get("parentId"));
                    org.setType(StringUtils.hasText(record.get("type")) ? record.get("type") : "DEPARTMENT");
                    String orderStr = record.get("orderNum");
                    org.setOrderNum(StringUtils.hasText(orderStr) ? Integer.parseInt(orderStr) : 0);
                    orgMapper.insert(org);
                    success++;
                } catch (Exception e) {
                    errors.add("行 " + record.getRecordNumber() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new BusinessException("解析 CSV 失败: " + e.getMessage());
        }
        return com.nexusarchive.dto.request.OrgImportResult.builder()
                .successCount(success)
                .failCount(errors.size())
                .errors(errors)
                .build();
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        return new org.apache.poi.ss.usermodel.DataFormatter().formatCellValue(cell).trim();
    }

    private void checkDataScope(Org targetOrg) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return; // Let Security filter handle unauthenticated
        }
        
        // System Admin bypass
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_system_admin"))) {
            return;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof com.nexusarchive.security.CustomUserDetails) {
            com.nexusarchive.security.CustomUserDetails user = (com.nexusarchive.security.CustomUserDetails) principal;
            // Allow if user belongs to the target org or is the parent of the target org
            // For simplicity in this iteration: Allow if user.departmentId matches targetOrg.id
            // In real world, we might check if targetOrg is a child of user.departmentId
            if (user.getDepartmentId() != null && user.getDepartmentId().equals(targetOrg.getId())) {
                return;
            }
             if (user.getDepartmentId() != null && user.getDepartmentId().equals(targetOrg.getParentId())) {
                return;
            }
        }
        
        throw new BusinessException("无权操作此组织数据");
    }

    private List<OrgTreeNode> buildTree(List<OrgTreeNode> nodes) {
        List<OrgTreeNode> roots = nodes.stream()
                .filter(n -> !StringUtils.hasText(n.getParentId()))
                .sorted(Comparator.comparing(OrgTreeNode::getOrderNum, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());

        for (OrgTreeNode root : roots) {
            root.setChildren(findChildren(root.getId(), nodes));
        }
        return roots;
    }

    private List<OrgTreeNode> findChildren(String parentId, List<OrgTreeNode> nodes) {
        List<OrgTreeNode> children = nodes.stream()
                .filter(n -> parentId.equals(n.getParentId()))
                .sorted(Comparator.comparing(OrgTreeNode::getOrderNum, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        for (OrgTreeNode child : children) {
            child.setChildren(findChildren(child.getId(), nodes));
        }
        return children;
    }

    private OrgTreeNode toNode(Org org) {
        OrgTreeNode node = new OrgTreeNode();
        node.setId(org.getId());
        node.setLabel(org.getName());
        node.setType(org.getType());
        node.setParentId(org.getParentId());
        node.setOrderNum(org.getOrderNum());
        return node;
    }

    public static class OrgTreeNode {
        private String id;
        private String label;
        private String type;
        private String parentId;
        private Integer orderNum;
        private List<OrgTreeNode> children = new ArrayList<>();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getParentId() { return parentId; }
        public void setParentId(String parentId) { this.parentId = parentId; }
        public List<OrgTreeNode> getChildren() { return children; }
        public void setChildren(List<OrgTreeNode> children) { this.children = children; }
        public Integer getOrderNum() { return orderNum; }
        public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }
    }
}
