package domain;

import com.alibaba.fastjson.annotation.JSONField;


import java.util.*;

public class User  {

    private static final long serialVersionUID = -8614929008491824422L;
    /**
     * 唯一ID
     */
    private String  uuid;
    /**
     * 用户名称
     */
    private String userName;
    /**
     * 账号
     */
    private String accountNumber;
    /**
     * 工号
     */
    private String jobNo;
    /**
     * 手机号
     */
    private String mobile;
    /**
     * 邮箱
     */
    private String mail;

    /**
     * 首字母名
     */
    private String firstName;
    /**
     * 尾字母名
     */
    private String lastName;
    /**
     * 昵称
     */
    private String nickName;
    /**
     *用户的身份证号
     */
    private String idNum;
    /**
     * 性别
     */
    private Integer gender;
    /**
     * 生日
     */
    @JSONField(format="yyyy-MM-dd HH:mm:ss")
    private Date birthday;

    /**
     * 家庭或公司电话
     */
    private String tel;

    /**
     *职位
     */
    private String jobTitle;

    /**
     *工作单位
     */
    private String workUnit;
    /**
     * 家庭地址
     */
    private String address;
    /**
     * 是否是管理员
     */
    private String roleType;
    /**
     *  扩展字段
     */
    private Map<String,Object> extraData=new HashMap<>();

    /**
     * 1-启用,2-停用,3-锁定,-1-删除
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 修改时间
     */
    private Date modifyTime;


	/**
	 * 用户组织路径
	 */
    private String path;


	/**
	 * 过期时间设置
	 */
	private Date expireTime;

	/**
	 * 备注
	 */
	private String remark;




	public Date getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(Date expireTime) {
		this.expireTime = expireTime;
	}



    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getJobNo() {
        return jobNo;
    }

    public void setJobNo(String jobNo) {
        this.jobNo = jobNo;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getIdNum() {
        return idNum;
    }

    public void setIdNum(String idNum) {
        this.idNum = idNum;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getWorkUnit() {
        return workUnit;
    }

    public void setWorkUnit(String workUnit) {
        this.workUnit = workUnit;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public Map<String, Object> getExtraData() {
        return extraData;
    }

    public void setExtraData(Map<String, Object> extraData) {
        this.extraData = extraData;
    }


    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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



	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}



	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(uuid, user.uuid) &&
                Objects.equals(userName, user.userName) &&
                Objects.equals(accountNumber, user.accountNumber) &&
                Objects.equals(jobNo, user.jobNo) &&
                Objects.equals(mobile, user.mobile) &&
                Objects.equals(mail, user.mail) &&
                Objects.equals(nickName, user.nickName) &&
                Objects.equals(idNum, user.idNum) &&
                Objects.equals(gender, user.gender) &&
                Objects.equals(birthday, user.birthday) &&
                Objects.equals(jobTitle, user.jobTitle) &&
                Objects.equals(workUnit, user.workUnit) &&
                Objects.equals(address, user.address) &&
                Objects.equals(extraData, user.extraData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid,userName, accountNumber, jobNo, mobile, mail, nickName, idNum, gender, birthday, jobTitle, workUnit, address, extraData);
    }
}