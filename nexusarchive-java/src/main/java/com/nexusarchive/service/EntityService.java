// Input: MyBatis-Plus IService、SysEntity Entity
// Output: EntityService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nexusarchive.entity.SysEntity;

import java.util.List;

/**
 * 法人实体服务接口
 *
 * PRD 来源: Section 1.1 - 法人仅管理维度
 * 功能: 法人实体的 CRUD 操作，法人与全宗的关联管理，法人层级树形结构管理
 */
public interface EntityService extends IService<SysEntity> {

    /**
     * 获取法人列表（仅活跃状态）
     * @return 法人列表
     */
    List<SysEntity> listActive();

    /**
     * 获取法人树形结构
     * @return 法人树节点列表
     */
    List<EntityTreeNode> getTree();

    /**
     * 获取指定法人下的全宗ID列表
     * @param entityId 法人ID
     * @return 全宗ID列表
     */
    List<String> getFondsIdsByEntityId(String entityId);

    /**
     * 检查法人是否可以删除
     * @param entityId 法人ID
     * @return true = 可删除，false = 存在关联全宗不可删除
     */
    boolean canDelete(String entityId);

    /**
     * 更新法人父节点（调整层级关系）
     * @param entityId 法人ID
     * @param parentId 父法人ID（null 表示顶级法人）
     */
    void updateParent(String entityId, String parentId);

    /**
     * 更新法人排序
     * @param entityId 法人ID
     * @param orderNum 排序号
     */
    void updateOrder(String entityId, Integer orderNum);

    /**
     * 法人树节点
     */
    class EntityTreeNode {
        private String id;
        private String name;
        private String taxId;
        private String parentId;
        private Integer orderNum;
        private String status;
        private Integer fondsCount;
        private List<EntityTreeNode> children = new java.util.ArrayList<>();

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getTaxId() { return taxId; }
        public void setTaxId(String taxId) { this.taxId = taxId; }
        public String getParentId() { return parentId; }
        public void setParentId(String parentId) { this.parentId = parentId; }
        public Integer getOrderNum() { return orderNum; }
        public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getFondsCount() { return fondsCount; }
        public void setFondsCount(Integer fondsCount) { this.fondsCount = fondsCount; }
        public List<EntityTreeNode> getChildren() { return children; }
        public void setChildren(List<EntityTreeNode> children) { this.children = children; }
    }
}





