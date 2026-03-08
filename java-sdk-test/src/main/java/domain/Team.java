package domain;



import java.util.*;

public class Team {

    private static final long serialVersionUID = 8403921025827300518L;
    /**
     * ID
     */
    private String id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 上级id
     */
    private String parentId;

    /**
     * 描述
     */
    private String description;

    /**
     * 来源id
     */
    private String sourceId;

    /**
     * 创建时间
     */

    private Date createTime;

    /**
     * 修改时间
     */
    private Date modifyTime;

    /**
     * 部门路径
     */
    private String path;

    /**
     * 部门id路径
     */
    private String idPath  ;

    /**
     *  扩展字段
     */
    private Map<String,Object> extraData=new HashMap<>();


	private int status;



	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(Date modifyTime) {
        this.modifyTime = modifyTime;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getIdPath() {
        return idPath;
    }

    public void setIdPath(String idPath) {
        this.idPath = idPath;
    }

    public Map<String, Object> getExtraData() {
        return extraData;
    }

    public void setExtraData(Map<String, Object> extraData) {
        this.extraData = extraData;
    }


	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Team)) return false;
        Team team = (Team) o;
        return Objects.equals(name, team.name) &&
                Objects.equals(sourceId, team.sourceId) &&
                Objects.equals(extraData, team.extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, sourceId, extraData);
    }
}