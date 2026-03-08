// Input: 无
// Output: DA/T 95—2022《行政事业单位一般公共预算支出财务报销电子会计凭证档案管理技术规范》 全量精准版
// Pos: /Users/user/nexusarchive/src/pages/product-website/regulations/DAT95Spec.tsx

import React from 'react';
import { RegulationLayout } from './RegulationLayout';

export const DAT95Spec: React.FC = () => {
  return (
    <RegulationLayout 
        title="DA/T 95—2022《行政事业单位一般公共预算支出财务报销电子会计凭证档案管理技术规范》"
        category="技术规范"
        source="国家标准/行业规范"
        effectiveDate="2022-07-01"
    >
      <div className="regulation-content whitespace-pre-wrap font-serif leading-relaxed text-slate-300">
ICS01.140.20
CCS A 14
中华人民共和国档案行业标准
DA/T 95—2022
行政事业单位一般公共预算支出财务报销
电子会计凭证档案管理技术规范
Technical

specification

for

electronic

accounting

voucher

archives

management

on

reimbursement

of

general

public

budget

in

administrative

institutions
2022-04-07发布 2022-07-01实施
国家档案局发布 112

DA/T

95—2022

前言
本文件按照 GB/T

1.1—2020《标准化工作导则第1部分:标准化文件的结构和起草规则》的规定
起草。
请注意本文件的某些内容可能涉及专利。本文件的发布机构不承担识别专利的责任。
本文件由国家档案局提出并归口。
本文件起草单位:国家档案局档案科学技术研究所、北京四方启点科技有限公司、中国科学院自动
化研究所、百望股份有限公司。
本文件主要起草人:聂曼影、战冰、邓高明、晏杰、张淑霞、陈吉、王熹、张学锋、王春恒、肖柏华、李玉杰、
李杰、冯辉。
212

DA/T

95—2022

行政事业单位一般公共预算支出财务报销
电子会计凭证档案管理技术规范
1 范围
本文件规定了行政事业单位一般公共预算支出财务报销电子会计凭证的采集、组件、归档、存储、统
计、利用以及相关系统衔接的要求。
本文件适用于行政事业单位财务报销电子会计凭证档案管理以及相关信息系统的设计、开发、测试
和使用。
2 规范性引用文件
下列文件中的内容通过文中的规范性引用而构成本文件必不可少的条款。其中,注日期的引用文
件,仅该日期对应的版本适用于本文件;不注日期的引用文件,其最新版本(包括所有的修改单)适用于
本文件。
GB/T

7408—2005 数据元和交换格式信息交换日期和时间表示法
GB/T

24589.2—2010 财经信息技术会计核算软件数据接口第2部分:行政事业单位
GB/T

29194—2012 电子文件管理系统通用功能要求
GB/T

33190—2016 电子文件存储与交换格式版式文档
GB/T

35275—2017 信息安全技术 SM2密码算法加密签名消息语法规范
GB/T

38540—2020 信息安全技术安全电子签章密码技术规范
GB/T

39784—2021 电子档案管理系统通用功能要求
DA/T

94—2022 电子会计档案管理规范
3 术语和定义
下列术语和定义适用于本文件。
3.1
会计凭证 accounting

voucher
记录经济业务,明确经济责任,按一定格式编制的据以登记会计账簿的书面证明。
注:

按填制的程序和用途,会计凭证分为原始凭证和记账凭证。
3.2
电子会计凭证 electronic

accounting

voucher
通过计算机系统开具,依赖计算机系统阅读、处理并可在通信网络上传输的会计凭证。
示例:电子发票、财政电子票据、电子客票、电子行程单、电子海关专用缴款书、银行电子回单等。
注1:

电子会计凭证包括可视化的版式文档以及对应的元数据。
注2:

纸质会计凭证的数字副本也是电子会计凭证的一种形式,但该形式仅作为开具系统未标准化前的过渡措施加
以使用。
312

DA/T

95—2022

3.3
会计档案 accounting

archives
单位在进行会计核算等过程中接收或形成的,记录和反映单位经济业务事项的,具有保存价值的文
字、图表等各种形式的会计资料。
注:

会计档案包括通过计算机等电子设备形成、传输和存储的电子会计凭证档案。 主要形式包括会计凭证、会计账
簿和财务报告等。
3.4
组件 comprise
按照要求明确件的构成并对件内文件排序的过程。
3.5
电子签名 electronic

signature
数据电文中以电子形式所含、所附用于识别签名人身份并表明签名人认可其中内容的数据。
4 系统功能
电子会计凭证生命周期中,一般存在三种类型系统,即电子原始凭证生成系统、财务管理系统和电
子档案管理系统,各系统定位如下。
a) 电子原始凭证生成系统是单位外部生成电子原始凭证的系统。
b) 财务管理系统用来支持单位业务工作的开展并形成、管理、归档电子会计凭证,系统应具备电
子报销、入账、会计核算、电子会计凭证存储和电子会计凭证归档处理等功能,其中:
1) 电子报销功能用于支持单位财务报销工作并形成电子审批单据,提供该功能的软件或模
块可包括办公自动化软件、网络化财务报销软件等;
2) 会计核算功能用于根据电子原始凭证记账数据生成记账凭证,并进行会计核算;
3) 电子会计凭证存储功能用于接收电子原始凭证和电子记账凭证,维护电子会计凭证、元数
据和业务之间的关联关系,以有序、系统、可审计的方式,存储、查询和利用电子会计凭证;
4) 电子会计凭证归档处理功能用于对电子会计凭证进行数据组织、清点、检测并提交归档。
c) 电子档案管理系统负责电子会计档案的移交接收、管理、长期保存和共享利用。
各系统之间的关系如图1所示。
412

DA/T

95—2022

图1 电子会计凭证相关系统的业务逻辑关系示意图
5 电子会计凭证采集与归档范围
5.1 电子原始凭证采集与归档范围
应采集、归档并以电子形式保存以下电子原始凭证:
a) 单位内部在经济业务发生时填制的,用于实施经办、审核、审批等必要审签程序的电子单据,包
括申请单和报销单等电子原始凭证版式文件,采用 OFD 等国家标准格式,文件格式应符合
GB/T

33190—2016的要求;
b) 单位从外部接收的电子形式的各类原始凭证的版式文件,采用 OFD 等国家标准格式,文件格
式应符合 GB/T

33190—2016的要求;
c) 纸质原始凭证的数字副本,可参照电子原始凭证处理,宜附有电子签名或电子签章;当使用签
名或签章时,电子签名应符合 GB/T

35275—2017 的要求,电子签章应符合 GB/T

38540—
2020的要求。
5.2 电子记账凭证采集与归档范围
应采集、归档并以电子形式保存以下电子记账凭证:
a) 实行电子化记账的单位,根据原始凭证填制的电子记账凭证版式文件,采用 OFD 等国家标准
格式,文件格式应符合 GB/T

33190—2016的要求;
b) 实行纸质记账的单位,纸质记账凭证的数字副本,可参照电子记账凭证处理,宜附有电子签名
或电子签章;当使用签名或签章时,电子签名应符合 GB/T

35275—2017 的要求,电子签章应
512

DA/T

95—2022

符合 GB/T

38540—2020的要求。
5.3 电子会计凭证元数据的归档范围
5.3.1 电子原始凭证元数据的归档范围
电子原始凭证元数据应与电子原始凭证一并收集、归档。单位内部形成的电子原始凭证元数据应
符合附录 A 的规定。单位从外部接收的电子原始凭证元数据应符合附录 B 的规定。
5.3.2 电子记账凭证元数据的归档范围
电子记账凭证元数据应与电子记账凭证一并收集、归档。 电子记账凭证元数据应符合附录 C 的
规定。
5.3.3 机构人员实体元数据的归档范围
应采集、归档并以电子形式保存形成、处理和管理电子会计凭证的机构/人员的相关元数据。机构
人员实体元数据应符合附录 D 的规定。
5.3.4 业务实体元数据归档范围
应采集、归档并以电子形式保存电子会计凭证在电子化归集、报销、入账、归档、移交和档案管理业
务中的相关元数据。业务实体元数据应符合附录 E 的规定。
6 电子会计凭证组件要求
6.1 电子会计凭证组件
6.1.1 应按记账凭证号对电子会计凭证进行组件,形成电子会计凭证册。组件时应保持电子会计凭证
内在的有机联系,并建立电子会计凭证与会计核算功能中记账数据的关联。
6.1.2 一个记账凭证号对应电子会计凭证的组件一般包括电子记账凭证和所附的电子原始凭证。电
子原始凭证一般包括单位内部形成的和单位从外部接收的两类电子原始凭证,其中单位内部形成的电
子原始凭证主要是与一笔经济业务有关的单位内部业务审签单据等;单位从外部接收的电子原始凭证
主要是用于证明单位已经发生明确经济责任并用作记账原始依据的电子票据和若干附件等。件内文件
按记账凭证、原始凭证和其他附件的顺序排列。电子会计凭证信息包组织方式应符合附录 F 的规定。
6.1.3 经上级有关部门批准的经济业务,应当将批准文件作为原始凭证附件。
6.1.4 应基于财务管理系统完成电子会计凭证的组件。
6.1.5 应按规则命名电子会计凭证的版式文件,命名规则应能保持电子会计凭证版式文件内在有机联
系与排列顺序,能通过物理文件名建立电子会计凭证版式文件与会计核算功能中记账数据的关联。
6.1.6 电子报销功能和会计核算功能应按内置命名规则自动、有序、连续地为电子会计凭证册命名。
6.2 纸质会计凭证数字副本组件
6.2.1 纸质会计凭证数字副本组件由业务经办部门或财务部门基于财务管理系统或设备完成。
6.2.2 纸质会计凭证数字化的同时,可提取图像上的文字内容,自动或手工补全纸质会计凭证的元
数据。
6.2.3 纸质会计凭证数字副本的组件应基于财务管理系统建立与纸质会计凭证的关联。
6.3 电子会计凭证元数据的组织
6.3.1 将一笔经济业务的电子会计凭证组件成册时,应创建一个存放本册电子会计凭证元数据的文
612

DA/T

95—2022

件,以 XBRL、XML 等格式保存。
6.3.2 财务管理系统中获取的该业务事项相关的电子会计凭证,应根据电子会计凭证所属类型,将电
子会计凭证元数据填入元数据文件中,作为元数据文件的节点。
6.3.3 应按照附录 F 规定的组织方式将电子会计凭证的元数据和版式文件分类存放。
6.3.4 单位内部形成的电子原始凭证元数据和组织方式应符合附录 A 的要求。单位从外部接收的电
子原始凭证元数据和组织方式应符合附录 B 的要求。
6.3.5 电子记账凭证元数据和组织方式应符合 GB/T

24589.2—2010和附录 C 的要求。
6.3.6 电子会计凭证元数据中的日期和时间元素,应符合 GB/T

7408—2005中完全表示法的要求。
7 电子会计凭证组卷及排列要求
电子会计凭证的组卷及排列应符合 DA/T

94—2022中8.4.2和8.4.3的规定。
8 应用系统及其功能要求
8.1 电子原始凭证生成系统
8.1.1 单位外部形成的电子原始凭证,应包含版式文件及其元数据。
8.1.2 单位外部形成的电子原始凭证,宜采取必要的技术措施确保其在流转过程中的信息安全。
8.2 财务管理系统
8.2.1 电子报销功能
应具备以下电子报销功能:
a) 采取相关技术措施确保接收的电子会计凭证来源合法、真实,对接收的电子会计凭证进行
检测;
b) 设定经办、审核、审批等必要的审签程序;
c) 能够准确、完整、有效地接收和读取电子会计凭证及其元数据,按照附录 A 和附录 B 规定的组
织方式,输出组件成册的电子原始凭证及其元数据;
d) 确保电子原始凭证在流转过程中的信息安全,对电子会计凭证的任何篡改能够及时被发现。
8.2.2 会计核算功能
应具备以下会计核算功能:
a) 设定制单、记账、审核等必要的审签程序,且能有效防止电子会计凭证重复入账;
b) 按照附录 C 规定的组织方式,输出组件成册的电子记账凭证及其元数据;
c) 按照附录 A 和附录 B 规定的组织方式,查询、调阅电子会计凭证版式文件及其元数据;
d) 通过电子会计凭证文件名或元数据,建立电子记账凭证与电子原始凭证的关联关系。
8.2.3 电子会计凭证存储功能
应具备以下电子会计凭证存储功能:
a) 按照附录 A、附录 B 和附录 C 规定的组织方式,接收组件成册的电子会计凭证及其元数据;
b) 按照附录 A、附录 B 和附录 C 规定的组织方式,提供电子会计凭证及其元数据的查询调用
功能;
c) 具备电子会计凭证导入、导出、备份、恢复功能。
712

DA/T

95—2022

电子会计凭证存储还宜支持网络异地数据备份功能。
8.2.4 电子会计凭证归档处理功能
应具备以下电子会计凭证归档处理功能:
a) 具备按流程对电子会计凭证进行归档的功能,支持电子会计凭证的数据组织、清点,对归档数
据进行真实性、完整性、可用性和安全性检测;
b) 支持生成会计档案移交清册版式文件,并按照国家档案管理的有关规定办理移交手续;
c) 财务管理系统中加密存储的电子会计凭证,应解密后提交归档。
8.3 电子档案管理系统
电子会计凭证经归档后,转变为凭证类会计档案,使用电子档案管理系统对凭证类会计档案进行规
范化管理。电子档案管理系统的功能应符合 GB/T

29194—2012和 GB/T

39784—2021的规定。
9 财务管理系统电子会计凭证的对接要求
9.1 电子会计凭证存入过程的系统对接要求
提供电子会计凭证存储功能的模块应支持从电子报销功能和会计核算功能的模块接收电子会计凭
证,并满足以下要求:
a) 采取系统对接的认证鉴权功能,识别为安全可信的系统才可接收电子会计凭证;
b) 支持在线和离线的批量接收与处理,保存移交接收处理记录;
c) 检查接收的电子会计凭证的数量、质量、完整性和规范性,并标注不合格项目;
d) 登记已检查合格的电子会计凭证,清点电子会计凭证的数量,验证电子会计凭证版式文件及其
元数据的有效性;
e) 支持电子会计凭证的自动归类与排序,支持分类与排序的调整处理;
f) 支持电子会计凭证的著录、标引等功能,形成电子会计凭证目录,并与电子会计凭证相关联;
g) 维护组件成册的电子会计凭证各组成部分及相关元数据之间、电子会计凭证之间的关联;
h) 记录电子会计凭证存入存储介质的处理过程,形成审计记录。
9.2 电子会计凭证读取过程的系统对接要求
提供电子会计凭证存储功能的模块应支持向电子报销功能、会计核算功能和电子会计凭证归档处
理功能的模块输出电子会计凭证,并满足以下要求:
a) 采取系统对接的认证鉴权功能,识别为安全可信的系统才可输出电子会计凭证;
b) 具备对电子会计凭证进行多条件的模糊检索、精确检索和全文检索等功能,检索结果能够进行
局部浏览和有选择性地输出;
c) 具备电子会计凭证在线查询、调阅功能,支持在线申请、在线审批、在线阅览、授权下载与打印,
并记录用户使用电子会计凭证的意见和效果等信息;
d) 具备与同类系统及设备进行数据交互的功能,并制定相应的备份与恢复策略。
10 财务管理系统电子会计凭证安全存储要求
10.1 通则
在财务管理系统中,应保证电子会计凭证安全存储,并进行真实性、完整性、可用性和安全性的
812

DA/T

95—2022

检测。
10.2 电子会计凭证完整性检查
电子会计凭证的完整性检查包括对电子会计凭证册的完整性检查以及对单张电子会计凭证的完整
性检查。
电子会计凭证册的完整性检查要求包括:
a) 财务管理系统应提供已组件的整册电子会计凭证完整性校验功能,保证在数据传入、数据存储
和数据传出时文件的完整性;
b) 财务管理系统中提供电子会计凭证存储功能的模块与异地备份系统应具备相互间文件完整性
校验功能,保证存储文件的一致性;
c) 财务管理系统应核对电子报销功能、会计核算功能记录的账务数据和电子会计凭证存储功能
记录的电子会计凭证数据,保证账务数据与电子会计凭证的一致性。
单张电子会计凭证的完整性检查要求包括:
a) 财务管理系统应能够检查单张电子会计凭证的完整性,应能及时发现对电子会计凭证的任何
篡改;
b) 财务管理系统应能读取电子会计凭证中数字证书的信息,并对电子签名信息进行验证,确认文
件完整性;
c) 财务管理系统应标识未通过完整性校验的电子会计凭证,自动记入日志,并主动提醒授权
用户。
10.3 电子会计凭证的真实性检查
10.3.1 财务管理系统应能检查电子会计凭证文件中电子签名来源的真实性、合法性。
10.3.2 财务管理系统宜通过数字证书可信链溯源的方式检查电子签名来源的真实性、合法性。
10.3.3 财务管理系统宜提供根证书管理功能,包括预置、更新根证书。
10.3.4 财务管理系统应标识未通过电子签名真实性检查的电子会计凭证,自动记入日志,并主动提醒
授权用户。
10.4 电子会计凭证的存储
10.4.1 应依据会计凭证号等标识符,在存储介质中自动逐级建立文件夹,分门别类、集中有序地存储
电子会计凭证。
10.4.2 应使用软件存储介质和电子会计凭证存储介质分离的专用本地存储模块或云存储模块存储电
子会计凭证,避免数据隐私泄露或者数据丢失。
10.4.3 财务管理系统应提供自检功能,及时发现存储空间不足或系统故障等可能影响电子会计凭证
存储的问题。
10.4.4 财务管理系统宜支持本单位电子会计凭证本地化存储和集中核算的单位电子会计凭证集中存
储功能,并通过数据同步保持数据一致性。
10.5 电子会计凭证的备份
10.5.1 财务管理系统宜提供网络备份等容灾功能,提升数据存储可靠性,应采取措施确保网络备份数
据的传输安全,避免由于网络问题造成数据丢失。
10.5.2 提供电子会计凭证存储功能的模块应实现数据存储介质备份,提升数据存储可靠性。
10.6 电子会计凭证的加密
10.6.1 财务管理系统可加密存储电子会计凭证,加密存储时应使用国产密码加密技术,采用符合国家
912

DA/T

95—2022

密码主管部门认可的加密模块。
10.6.2 财务管理系统可采用口令、密码技术、生物技术等两种或两种以上组合的鉴别技术对用户进行
身份鉴别,且其中至少一种鉴别技术应使用国产密码加密技术,采用符合国家密码主管部门认可的加密
模块。
11 财务管理系统电子会计凭证的统计要求
财务管理系统应提供电子会计凭证统计功能。电子会计凭证统计功能的要求如下:
a) 应确保所有数据的结构、术语、实体描述和属性在使用中的一致性;
b) 应按照业务逻辑对电子会计凭证进行必要描述,包括赋予唯一标识符、题名等,应满足会计业
务及档案业务所涉及的信息,包括但不限于制单人、审核人、记账凭证日期、记账凭证摘要等;
c) 财务管理系统应具备对一定时间期限内的电子会计凭证的接收、整理、保存、鉴定、利用等关键
业务过程工作情况进行统计的功能;
d) 财务管理系统宜提供报表制作工具,支持授权用户自定义统计报表等功能,电子会计凭证统计
报表宜采用 XBRL、ET、XML、XLS等常用格式。
022

DA/T

95—2022

附录 A
(规范性)
单位内部形成的电子原始凭证的元数据及组织方式
A.1 国内差旅出差审批单
表 A.1规定了国内差旅出差审批单的元数据及组织方式。
表 A.1 国内差旅出差审批单元数据及组织方式
标识符元素名称级次
是否
为容器型
元数据
约束性备注
TravelApplicationForm 国内差旅出差审批单 1 是 —
TravelApplicationFormNo 出差审批单号 2 否必选
MOF_DIV_CODE 财政区划 2 否必选
AgencyName 单位名称 2 否必选
ApplyDate 申请日期 2 否必选
Applicant 申请人 2 否必选
Department 部门 2 否必选
CurSourceName 经费列支渠道 2 否可选
Comment 备注 2 否可选
ExpectAmount 预估费用 2 否可选
LoanApplicationFormNo 关联的借款审批单号 2 否可选
RouteInfoData
国内差旅出差审批单行程
明细 2 是 —
RouteNo 行程编号 3 否必选
TravelReason 出差事由 3 否必选
TravelType 出差类型 3 否必选
Destination 出差地点 3 否必选
可包含多个地点
的字符串
StartDate 开始日期 3 否必选
EndDate 结束日期 3 否必选
TravelerInfoData 国内差旅出差人员明细 2 是 —
Name 姓名 3 否必选
Id 出差人证件号码 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
122

DA/T

95—2022

表 A.1 国内差旅出差审批单元数据及组织方式

(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
Title 职称 3 否可选
Rank 职级 3 否可选
Vehicle 交通工具 3 否必选
PersonalType 人员类型 3 否必选
ApprovalInfoData
国内差旅出差审批单审批
明细 2 是 —
Name 姓名 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Role 审批角色 3 否必选
Date 审批日期 3 否必选
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
A.2 国内差旅报销审批单
表 A.2规定了国内差旅报销审批单的元数据及组织方式。
表 A.2 国内差旅报销审批单元数据及组织方式
标识符元素名称级次
是否
为容器型
元数据
约束性备注
ReimbursementForm 国内差旅报销审批单 1 是 —
ReimbursementFormNo 报销单号 2 否必选
MOF_DIV_CODE 财政区划 2 否必选
ReimbursementTypeNo 报销类型编码 2 否必选
ReimbursementType 报销类型名称 2 否必选
AgencyName 单位名称 2 否必选
Department 部门 2 否必选
ApplyDate 报销申请日期 2 否必选
222

DA/T

95—2022

表 A.2 国内差旅报销审批单元数据及组织方式

(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
TravelApplicationFormNo 关联的出差审批单号 2 否可选
DepDate 差旅出发日期 2 否可选
ReturnDate 差旅返回日期 2 否可选
AttachmentCount 附件数 2 否必选
Applicant 申请人姓名 2 否必选
TravelReason 出差事由 2 否必选
ApplyAmount 填报金额 2 否必选
DeductAmount 核减金额 2 否必选
ReimburseAmount 报销合计金额 2 否必选
ReimburseAmountComplex 报销合计金额大写 2 否必选
LoanApplicationFormNo 关联的借款审批单号 2 否可选
LoanAmount 借款金额 2 否必选
BalanceAmount 借支差额 2 否必选
BusinessCardAmount 消费金额 2 否必选
AllowanceAmount 补助金额 2 否必选
Comment 备注 2 否可选
PermissionComment 特殊事项审批事由 2 否可选
CurSourceName 经费列支渠道 2 否可选
TravelerInfoData
国内差旅报销出差人员明细
信息 2 是 —
Name 出差人姓名 3 否必选
Id 出差人证件号码 3 否必选
Department 出差人所在部门 3 否必选
Vehicle 交通工具 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
PersonalType 人员类型 3 否必选
VehicleData 国内差旅报销交通费用明细 2 是 —
Name 出差人姓名 3 否必选
Id 出差人证件号码 3 否必选
VoucherType 凭证类型 3 否必选
322

DA/T

95—2022

表 A.2 国内差旅报销审批单元数据及组织方式

(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
DepDate 出发日期 3 否必选
DepCity 出发地 3 否必选
ArrDate 到达日期 3 否必选
ArrCity 到达地 3 否必选
VehicleType 交通工具类型 3 否必选
ReceiptAmount 凭证票面金额 3 否必选
DeductAmount 核减金额 3 否可选
AllowanceData
国内差旅报销审批单补助
明细 2 是 —
Name 姓名 3 否必选
Id 证件号码 3 否必选
AllowanceType 补助类型 3 否必选
AllowanceDays 补助天数 3 否必选
AllowanceAmount 补助金额 3 否必选
DeductAmount 核减金额 3 否可选
OtherExpenseData
国内差旅报销审批单其他凭
证明细 2 是 —
Name 出差人姓名 3 否必选
Id 出差人证件号码 3 否必选
VoucherType 凭证类型 3 否必选
ExpenseType 凭证类型名称 3 否必选
ReceiptAmount 凭证票面金额 3 否必选
DeductAmount 核减金额 3 否可选
Comment

备注 3 否可选
ApprovalInfoData
国内差旅报销审批单审批信
息明细 2 是 —
Name 姓名 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Role 审批角色 3 否必选
Date 审批日期 3 否必选
422

DA/T

95—2022

表 A.2 国内差旅报销审批单元数据及组织方式

(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
PermissionApprovalInfoData
国内差旅报销审批单特殊事
项审批明细 2 是 —
Name 姓名 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Role 审批角色 3 否必选
Date 审批日期 3 否必选
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
A.3 借款审批单
表 A.3规定了借款审批单的元数据及组织方式。
表 A.3 借款审批单元数据及组织方式
标识符元素名称级次
是否
为容器型
元数据
约束性备注
LoanApplicationForm 借款审批单主单 1 是 —
LoanApplicationFormNo 借款审批单号 2 否必选
MOF_DIV_CODE 财政区划 2 否必选
AgencyName 单位名称 2 否必选
ApplyDate 申请日期 2 否必选
LoanUser 借款人 2 否必选
Department 部门 2 否必选
LoanType 借款方式 2 否必选
LoanReason 借款事由 2 否必选
522

DA/T

95—2022

表 A.3 借款审批单元数据及组织方式

(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
LoanAmount 借款金额 2 否必选
LoanAmountComplex 借款金额大写 2 否必选
PayeeAcctName 收款单位 2 否必选
PayerAcctBankName 收款银行 2 否必选
PayeeAcctNo 收款账号 2 否必选
PayeeBankName 收款方账户开户行名称 2 否必选
CurSourceName 经费列支渠道 2 否必选
Rank 职级 2 否必选
Comment 备注 2 否必选
ApprovalInfoData 借款审批单审批明细 2 是 —
Name 姓名 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Role 审批角色 3 否必选
Date 审批日期 3 否必选
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
A.4 支出事项审批单
表 A.4规定了支出事项审批单的元数据及组织方式。
表 A.4 支出事项审批单元数据及组织方式
标识符元素名称级次
是否
为容器型
元数据
约束性备注
SpendApplicationForm 支出事项审批单主单 1 是 —
SpendApplicationFormNo 支出事项审批单号 2 否必选
MOF_DIV_CODE 财政区划 2 否必选
AgencyName 单位名称 2 否必选
622

DA/T

95—2022

表 A.4 支出事项审批单元数据及组织方式(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
ApplyDate 申请日期 2 否必选
Applicant 申请人 2 否必选
Department 部门 2 否必选
SpendReason 支出事项 2 否必选
SpendAmount 支出事项申请金额 2 否必选
SpendAmountComplex 支出事项申请金额大写 2 否必选
CurSourceName 经费列支渠道 2 否必选
Comment 备注 2 否可选
UseDepartmentInfoData
支出事项审批单使用部门
明细 2 是 —
UseDepartment 支出事项使用部门名称 3 否必选
UserInfoData 支出事项审批单使用人明细 2 是 —
UserName 支出事项使用人姓名 3 否必选
UserDepartment 支出事项使用人部门名称 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
ApprovalInfoData 支出事项审批单审批明细 2 是 —
Name 姓名 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Role 审批角色 3 否必选
Date 审批日期 3 否必选
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
A.5 付款审批单
表 A.5规定了付款审批单的元数据及组织方式。
722

DA/T

95—2022

表 A.5 付款审批单元数据及组织方式
标识符元素名称级次
是否
为容器型
元数据
约束性备注
PaymentApplicationForm 付款审批单主单 1 是 —
PaymentApplicationFormNo 付款审批单号 2 否必选
MOF_DIV_CODE 财政区划 2 否必选
AgencyName 单位名称 2 否必选
ApplyDate 申请日期 2 否必选
Applicant 申请人 2 否必选
Department 部门 2 否必选
PaymentMode 付款方式 2 否必选
PaymentReason 付款事由 2 否必选
PaymentAmount 付款金额 2 否必选
PaymentAmountComplex 付款金额大写 2 否必选
PayeeAcctName 收款人全称 2 否必选
收款人可以是机
构名称或自然人
姓名
PayerAcctBankName 收款银行 2 否必选
PayeeAcctNo 收款账号 2 否必选
PayeeBankName 收款方账户开户行名称 2 否可选具体到支行
CurSourceName 经费列支渠道 2 否必选
Rank 职级 2 否必选
Comment 备注 2 否可选
ApprovalInfoData 付款审批单审批明细 2 是 —
Name 姓名 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Role 审批角色 3 否必选
Date 审批日期 3 否必选
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
822

DA/T

95—2022

A.6 一般费用报销审批单
表 A.6规定了一般费用报销审批单的元数据及组织方式。
表 A.6 一般费用报销审批单元数据及组织方式
标识符元素名称级次
是否
为容器型
元数据
约束性备注
ReimbursementForm 一般费用报销审批单主单 1 是 —
ReimbursementFormNo 报销审批单号 2 否必选
MOF_DIV_CODE 财政区划 2 否必选
AgencyName 单位名称 2 否必选
Department 部门 2 否必选
ApplyDate 申请日期 2 否必选
AttachmentCount 附件数 2 否必选
Applicant 申请人 2 否必选
SpendApplicationFormNo 关联的支出事项审批单号 2 否可选
ReimburseReason 报销事由 2 否必选
ApplyAmount 填报金额 2 否必选
DeductAmount 核减金额 2 否必选
ReimburseAmount 报销合计金额 2 否必选
ReimburseAmountComplex 报销合计金额大写 2 否必选
LoanApplicationFormNo 关联的借款审批单号 2 否可选
LoanAmount 借款金额 2 否必选
BalanceAmount 借支差额 2 否必选
CurSourceName 经费列支渠道 2 否必选
Comment 备注 2 否必选
CostData
一般费用报销审批单费用
明细 2 是 —
Pjzl 票据种类 3 否必选
发票、财政票据、
其他类票据
Pjhm 票据号码 3 否可选
Pjdm 票据代码 3 否必选
ApplyAmount 报销申请金额 3 否必选
DeductAmount 核减金额 3 否必选
ApprovedAmount 报销核准金额 3 否必选
PayTypeName 支付方式名称 3 否必选
PayerAcctBankName 收款银行 3 否必选
922

DA/T

95—2022

表 A.6 一般费用报销审批单元数据及组织方式(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
PayeeAcctNo 收款账号 3 否必选
PayeeBankName 收款方账户开户行名称 3 否必选
CostType 费用类型 3 否必选
CostTypeCode 费用类型代码 3 否必选
CostApprovedAmount 报销核准金额 3 否必选
ApprovalInfoData
一般费用报销审批单审批
明细 2 是 —
Name 姓名 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Role 审批角色 3 否必选
Date 审批日期 3 否必选
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
A.7 会议费报销审批单
表 A.7规定了会议费报销审批单的元数据及组织方式。
表 A.7 会议费报销审批单元数据及组织方式
标识符元素名称级次
是否
为容器型
元数据
约束性备注
ReimbursementForm 会议费报销审批单主单 1 是 —
ReimbursementFormNo 报销审批单号 2 否必选
MOF_DIV_CODE 财政区划 2 否必选
AgencyName 单位名称 2 否必选
Department 部门 2 否必选
ApplyDate 申请日期 2 否必选
AttachmentCount 附件数 2 否必选
032

DA/T

95—2022

表 A.7 会议费报销审批单元数据及组织方式(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
Applicant 申请人 2 否必选
MeetingPlanNo 关联的会议计划编号 2 否可选
ConferenceName 会议名称 2 否必选
CategoryName 会议类型 2 否必选
ConferenceContent 会议内容 2 否必选
HostCity 举办城市 2 否必选
HotelName 酒店名称 2 否必选
BeginTime 会议开始日期 2 否必选
EndTime 会议结束日期 2 否必选
RepresentNum 会议代表人数 2 否必选
WorkerNum 工作人员人数 2 否必选
TotalNum 会议总人数 2 否必选
ConferenceDay 会期(天数) 2 否必选
PrepareEvacuationDay 报道撤离天数 2 否必选
TotalDay 会议总天数 2 否必选
ApplyAmount 填报金额 2 否必选
DeductAmount 核减金额 2 否必选
ReimburseAmount 报销合计金额 2 否必选
ReimburseAmountComplex 报销合计金额大写 2 否必选
LoanApplicationFormNo 关联的借款审批单号 2 否可选
LoanAmount 借款金额 2 否必选
BalanceAmount 借支差额 2 否必选
CurSourceName 经费列支渠道 2 否必选
Comment 备注 2 否必选
ApprovalInfoData 会议费报销审批单审批明细 2 是 —
Name 姓名 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Role 审批角色 3 否必选
Date 审批日期 3 否必选
132

DA/T

95—2022

表 A.7 会议费报销审批单元数据及组织方式(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
A.8 医疗费报销审批单
表 A.8规定了医疗费报销审批单的元数据及组织方式。
表 A.8 医疗费报销审批单元数据及组织方式
标识符元素名称级次
是否
为容器型
元数据
约束性备注
ReimbursementForm 医疗费报销审批单主单 1 是 —
ReimbursementFormNo 报销审批单号 2 否必选
MOF_DIV_CODE 财政区划 2 否必选
AgencyName 单位名称 2 否必选
Department 部门 2 否必选
ApplyDate 申请日期 2 否必选
AttachmentCount 附件数 2 否必选
Applicant 申请人 2 否必选
Patient 就医人 2 否必选
PatientDepartment 部门 2 否必选
ReimburseType 报销类型 2 否必选
ReimburseReason 报销事由 2 否必选
ApplyAmount 填报金额 2 否必选
DeductAmount 核减金额 2 否必选
ReimburseAmount 报销合计金额 2 否必选
ReimburseAmountComplex 报销合计金额大写 2 否必选
LoanApplicationFormNo 关联的借款审批单号 2 否可选
LoanAmount 借款金额 2 否必选
BalanceAmount 借支差额 2 否必选
PayerAcctBankName 收款银行 2 否必选
PayeeAcctNo 收款账号 2 否必选
232

DA/T

95—2022

表 A.8 医疗费报销审批单元数据及组织方式(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
PayeeBankName 收款方账户开户行名称 2 否必选
CurSourceName 经费列支渠道 2 否必选
Comment 备注 2 否必选
CostData
医疗费报销审批单医疗费
明细 2 是 —
EinvoiceCode 电子票据代码 3 否必选
EinvoiceNumber 电子票据号码 3 否必选
EinvoiceName 电子票据名称 3 否必选
IssueDate 开票日期 3 否必选
TotalAmount 票据总金额 3 否必选
DeductAmount 核减合计金额 3 否必选
IssuedBy 就诊医院 3 否必选
ItemCode 项目编码 3 否必选
ItemName 项目名称 3 否必选
ItemRemark 备注 3 否必选
ItemAmount 项目金额 3 否必选
ItemDeductAmount 核减金额 3 否必选
ItemDeductRatio 调整比例 3 否必选
ApprovalInfoData 医疗费报销审批单审批明细 2 是 —
Name 姓名 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Role 审批角色 3 否必选
Date 审批日期 3 否必选
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
A.9 因公出国(境)审批单
表 A.9规定了因公出国(境)审批单的元数据及组织方式。
332

DA/T

95—2022

表 A.9 因公出国(境)审批单元数据及组织方式
标识符元素名称级次
是否
为容器型
元数据
约束性备注
TravelApplicationForm 因公出国(境)审批单 1 是 —
TravelApplicationFormNo 出差审批单号 2 否必选
MOF_DIV_CODE 财政区划 2 否必选
AgencyName 单位名称 2 否必选
PlanNo 计划编号 2 否可选
GroupName 团组名称 2 否必选
ApplyDate 申请日期 2 否必选
Applicant 申请人 2 否必选
Department 部门 2 否必选
CurSourceName 经费列支渠道 2 否可选
GroupUnit 组团单位 2 否必选
GroupLeader 团组负责人 2 否必选
TotalPersons 总人数 2 否可选
UnitPersons 本单位人数 2 否可选
PlanProperties 计划属性 2 否可选
InstructionsNo 任务批件号 2 否可选
VisitTarget 出访目标和必要性 2 否可选
VisiFlight 出访航班信息 2 否可选
SumAmount 总金额(人民币) 2 否必选
Comment 备注 2 否可选
ExpectAmount 预估费用 2 否可选
LoanApplicationFormNo 关联的借款审批单号 2 否可选
RouteInfoData 出国(境)行程明细 2 是 —
RouteNo 行程编号 3 否必选
TravelType 团组类别 3 否可选
Destination 出访城市 3 否必选
可包含多个地点
的字符串
StartDate 出境日期 3 否必选
EndDate 入境日期 3 否必选
TravelerInfoData 出国(境)人员明细 2 是 —
Name 姓名 3 否必选
Id 出差人证件号码 3 否必选
432

DA/T

95—2022

表 A.9 因公出国(境)审批单元数据及组织方式(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
PersonalType 人员类型 3 否必选
BudgetInfoData 出国(境)预算汇总 2 是 —
Currency 币种 3 否必选
ExchangeRate

汇率 3 否必选
ForeignCurrencySumA-
mount
外币金额合计 3 否必选
StandardCurrencySumA-
mount
本位币金额合计 3 否可选
BudgetDetailInfoData 出国(境)预算明细 3 是 —
FeeType 费用类型 4 否必选
Country 国家 4 否可选
City 城市 4 否可选
Currency 币种 4 否必选
Standard 费用标准 4 否可选
DaysNo 天数 4 否可选
PeopleNo 人数 4 否可选
FeeSumAmount 金额 4 否必选
ApprovalInfoData 出国(境)审批明细 2 是 —
Name 姓名 3 否必选
Role 审批角色 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Date 审批日期 3 否必选
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
532

DA/T

95—2022

A.10 因公出国(境)费报销审批单
表 A.10规定了因公出国(境)费报销审批单的元数据及组织方式。
表 A.10 因公出国(境)费报销审批单元数据及组织方式
标识符元素名称级次
是否
为容器型
元数据
约束性备注
ReimbursementForm 因公出国(境)费报销审批单 1 是 —
ReimbursementFormNo 报销审批单号 2 否必选
MOF_DIV_CODE 财政区划 2 否必选
AgencyName 单位名称 2 否必选
PlanNo 计划编号 2 否可选
GroupName 团组名称 2 否必选
ApplyDate 申请日期 2 否必选
Applicant 申请人 2 否必选
Department 部门 2 否必选
CurSourceName 经费列支渠道 2 否可选
GroupUnit 组团单位 2 否必选
Destination 出差地点 2 否必选
TotalPersons 总人数 2 否必选
UnitPersons 本单位人数 2 否必选
TravelApplicationFormNo 关联的出差审批单号 2 否可选
StartDate 出境日期 2 否必选
EndDate 入境日期 2 否必选
ApplyAmount 填报金额 2 否必选
DeductAmount 核减金额 2 否可选
ReimburseAmount 报销合计金额 2 否必选
ReimburseAmountComplex 报销合计金额大写 2 否必选
LoanApplicationFormNo 关联的借款审批单号 2 否可选
LoanAmount 借款金额 2 否可选
BalanceAmount 借支差额 2 否可选
AttachmentCount 附件数 2 否可选
PhoneNo 联系电话 2 否可选
Comment 备注 2 否可选
PermissionComment 特殊事项审批事由 2 否可选
TravelerInfoData
出国 (境)差旅报销审批人员
明细 2 是 —
632

DA/T

95—2022

表 A.10 因公出国(境)费报销审批单元数据及组织方式(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
Name 姓名 3 否必选
Id 出差人证件号码 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
PersonalType 人员类型 3 否必选
LoanInfoData
出国 (境 )费报销审批借汇
明细 2 是 —
Currency 币种 3 否必选
ExchangeRate

汇率 3 否可选
ForeignCurrencySumA-
mount
外币金额合计 3 否可选
StandardCurrencySumA-
mount
本位币金额合计 3 否必选
LoanCurrencyType 借汇方式 3 否可选
AdditionalLoanInfoData 出国(境)费报销补汇明细 2 是 —
Currency 补汇币种 3 否必选
SettleinCNY 使用人民币结算 3 否必选
ExchangeRate

汇率 3 否可选
ForeignCurrencySumA-
mount
外币金额 3 否可选
StandardCurrencySumA-
mount
本位币金额 3 否必选
RefundExchangeInfoData 出国(境)费报销退汇明细 2 是 —
Currency 退汇币种 3 否必选
SettleinCNY 使用人民币结算 3 否必选
ExchangeRate

汇率 3 否可选
ForeignCurrencySumA-
mount
外币金额合计 3 否可选
StandardCurrencySumA-
mount
本位币金额合计 3 否必选
ExpendInfoData 出国(境)费报销支出明细 2 是 —
FeeType 费用类型 3 否必选
732

DA/T

95—2022

表 A.10 因公出国(境)费报销审批单元数据及组织方式(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
Currency 币种 3 否必选
ReceiptAmount 凭证票面金额 3 否可选
FeeSumAmount 报销金额 3 否必选
DeductAmount 核减金额 3 否可选
ExchangeRate 汇率 3 否可选
StandardCurrencyAmount 折人民币金额 3 否必选
ApprovalInfoData 出国(境)费报销审批明细 2 是 —
Name 姓名 3 否必选
Role 审批角色 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Date 审批日期 3 否必选
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
PermissionApprovalInfoData
出国 (境)费报销特殊事项审
批明细 2 是 —
Name 姓名 3 否必选
Role 审批角色 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Date 审批日期 3 否必选
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
A.11 培训费报销审批单
表 A.11规定了培训费报销审批单的元数据及组织方式。
832

DA/T

95—2022

表 A.11 培训费报销审批单元数据及组织方式
标识符元素名称级次
是否
为容器型
元数据
约束性备注
ReimbursementForm 培训费报销审批单主单 1 是 —
ReimbursementFormNo 报销审批单号 2 否必选
MOF_DIV_CODE 财政区划 2 否可选
AgencyName 单位名称 2 否必选
Department 部门 2 否必选
ApplyDate 申请日期 2 否必选
AttachmentCount 附件数 2 否必选
Applicant 申请人 2 否必选
TrainingPlanNo 培训计划编号 2 否必选
TrainingName 培训名称 2 否必选
CategoryName 培训类型 2 否必选
TrainingContent 培训内容 2 否必选
HostCity 举办城市 2 否必选
HotelName 酒店名称 2 否必选
BeginTime 培训开始日期 2 否必选
EndTime 培训结束日期 2 否必选
AttendTrainingNum 参训人数 2 否必选
WorkerNum 工作人员人数 2 否必选
TotalNum 培训总人数 2 否必选
TrainingDay 培训时长(天数) 2 否必选
PrepareEvacuationDay 报道撤离天数 2 否必选
TotalDay 培训总天数 2 否必选
ApplyAmount 填报金额 2 否必选
DeductAmount 核减金额 2 否必选
ReimburseAmount 报销合计金额 2 否必选
ReimburseAmountComplex 报销合计金额大写 2 否必选
LoanApplicationFormNo 关联的借款审批单号 2 否可选
LoanAmount 借款金额 2 否可选
BalanceAmount 借支差额 2 否必选
CurSourceName 经费列支渠道 2 否必选
Comment 备注 2 否可选
ApprovalInfoData 培训费报销审批单审批明细 2 是 —
932

DA/T

95—2022

表 A.11 培训费报销审批单元数据及组织方式(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
Name 姓名 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Role 审批角色 3 否必选
Date 审批日期 3 否必选
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
A.12 劳务费报销审批单
表 A.12规定了劳务费报销审批单的元数据及组织方式。
表 A.12 劳务费报销审批单元数据及组织方式
标识符元素名称级次
是否
为容器型
元数据
约束性备注
ReimbursementForm 劳务费报销审批单主单 1 是 —
ReimbursementFormNo 报销审批单号 2 否必选
MOF_DIV_CODE 财政区划 2 否可选
AgencyName 单位名称 2 否必选
Department 部门 2 否必选
ApplyDate 申请日期 2 否必选
AttachmentCount 附件数 2 否必选
Applicant 申请人 2 否必选
ReimburseReason 报销事由 2 否必选
ApplyAmount 填报金额 2 否必选
DeductAmount 核减金额 2 否可选
ReimburseAmount 报销合计金额 2 否必选
ReimburseAmountComplex 报销合计金额大写 2 否必选
LoanApplicationFormNo 关联的借款审批单号 2 否可选
042

DA/T

95—2022

表 A.12 劳务费报销审批单元数据及组织方式(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
LoanAmount 借款金额 2 否可选
BalanceAmount 借支差额 2 否必选
CurSourceName 经费列支渠道 2 否必选
Comment 备注 2 否可选
ReimbursementInfoData 劳务费报销明细信息 2 是 —
Name 劳务人员姓名 3 否必选
Id 劳务人员证件号码 3 否必选
Department 劳务人员所在部门 3 否可选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
PersonalType 人员类型 3 否必选
ServiceFeeAmount 劳务金额 3 否必选
DeductAmount 核减金额 3 否可选
ISAuthorRemuneration 是否稿酬 3 否必选
ApprovalInfoData 劳务费报销审批单审批明细 2 是 —
Name 姓名 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Role 审批角色 3 否必选
Date 审批日期 3 否必选
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
A.13 公务接待费报销审批单
表 A.13规定了公务接待费报销审批单的元数据及组织方式。
142

DA/T

95—2022

表 A.13 公务接待费报销审批单元数据及组织方式
标识符元素名称级次
是否
为容器型
元数据
约束性备注
ReimbursementForm 公务接待费报销审批单主单 1 是 —
ReimbursementFormNo 报销审批单号 2 否必选
MOF_DIV_CODE 财政区划 2 否可选
AgencyName 单位名称 2 否必选
Department 部门 2 否必选
ApplyDate 申请日期 2 否必选
AttachmentCount 附件数 2 否必选
Applicant 申请人 2 否必选
VisitUnit 来访单位 2 否必选
ReimburseReason 来访内容 2 否必选
VisitTime 来访时间 2 否必选
VisitorsNum 来访人数 2 否必选
ReceptionArrangement 接待安排 2 否可选
ApplyAmount 填报金额 2 否必选
DeductAmount 核减金额 2 否可选
ReimburseAmount 报销合计金额 2 否必选
ReimburseAmountComplex 报销合计金额大写 2 否必选
LoanApplicationFormNo 关联的借款审批单号 2 否可选
LoanAmount 借款金额 2 否可选
BalanceAmount 借支差额 2 否必选
CurSourceName 经费列支渠道 2 否必选
Comment 备注 2 否可选
ReimbursementInfoData
公务接待费报销接待人员明
细信息 2 是 —
Name 人员姓名 3 否必选
Id 人员证件号码 3 否必选
Department 人员所在单位/部门 3 否可选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
PersonalType 人员类型 3 否必选
HotelData
公务接待费报销住宿费用
明细 2 是 —
242

DA/T

95—2022

表 A.13 公务接待费报销审批单元数据及组织方式(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
TotalNum 入住人数 3 否必选
StandardRate 标准 3 否必选
ArrDate 入住日期 3 否必选
DepDate 离店日期 3 否必选
TotalAmount 合计金额 3 否必选
DeductAmount 核减金额 3 否可选
Comment 备注 3 否可选
FeteData
公务接待费报销审批单宴请
费用明细 2 是 —
TotalNum 人数 3 否必选
StandardRate 标准 3 否必选
ArrDate 起始日期 3 否必选
DepDate 截止日期 3 否必选
TotalAmount 合计金额 3 否必选
DeductAmount 核减金额 3 否可选
Comment 备注 3 否可选
OfficialDinnerData
公务接待费报销审批单工作
餐明细 2 是 —
TotalNum 人数 3 否必选
StandardRate 标准 3 否必选
ArrDate 起始日期 3 否必选
DepDate 截止日期 3 否必选
TotalAmount 合计金额 3 否必选
DeductAmount 核减金额 3 否可选
Comment 备注 3 否可选
GiftData
公务接待费报销审批单赠礼
费用明细 2 是 —
TotalNum 人数 3 否必选
StandardRate 标准 3 否必选
ArrDate 发放起始日期 3 否必选
342

DA/T

95—2022

表 A.13 公务接待费报销审批单元数据及组织方式(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
DepDate 发放截止日期 3 否必选
TotalAmount 合计金额 3 否必选
DeductAmount 核减金额 3 否可选
Comment 备注 3 否可选
TrafficData
公务接待费报销审批单交通
费明细 2 是 —
TotalNum 人数 3 否必选
StandardRate 标准 3 否必选
ArrDate 起始日期 3 否必选
DepDate 截止日期 3 否必选
TotalAmount 合计金额 3 否必选
DeductAmount 核减金额 3 否可选
Comment 备注 3 否可选
OtherExpenseData
公务接待费报销审批单其他
费用明细 2 是 —
Name 姓名 3 否必选
Id 证件号码 3 否必选
VoucherType 凭证类型 3 否必选
ExpenseType 凭证类型名称 3 否必选
ReceiptAmount 凭证票面金额 3 否必选
DeductAmount 核减金额 3 否可选
Comment 备注 3 否可选
ApprovalInfoData
公务接待费报销审批单审批
明细 2 是 —
Name 姓名 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Role 审批角色 3 否必选
442

DA/T

95—2022

表 A.13 公务接待费报销审批单元数据及组织方式(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
Date 审批日期 3 否必选
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
PermissionApprovalInfoData
公务接待费报销审批单特殊
事项审批明细 2 是 —
Name 姓名 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Role 审批角色 3 否必选
Date 审批日期 3 否必选
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
A.14 公务用车运行维护费报销审批单
表 A.14规定了公务用车运行维护费报销审批单的元数据及组织方式。
表 A.14 公务用车运行维护费报销审批单元数据及组织方式
标识符元素名称级次
是否
为容器型
元数据
约束性备注
ReimbursementForm
公务用车运行维护费报销审
批单主单 1 是 —
ReimbursementFormNo 报销审批单号 2 否必选
MOF_DIV_CODE 财政区划 2 否可选
AgencyName 单位名称 2 否必选
Department 部门 2 否必选
ApplyDate 申请日期 2 否必选
542

DA/T

95—2022

表 A.14 公务用车运行维护费报销审批单元数据及组织方式(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
AttachmentCount 附件数 2 否必选
Applicant 申请人 2 否必选
ReimburseReason 报销事由 2 否必选
ApplyAmount 填报金额 2 否必选
DeductAmount 核减金额 2 否可选
ReimburseAmount 报销合计金额 2 否必选
ReimburseAmountComplex 报销合计金额大写 2 否必选
LoanApplicationFormNo 关联的借款审批单号 2 否可选
LoanAmount 借款金额 2 否可选
BalanceAmount 借支差额 2 否必选
CurSourceName 经费列支渠道 2 否必选
Comment 备注 2 否可选
CostData
公务用车运行维护费报销审
批单费用明细 2 是 —
CarNumber 车牌号 3 否必选
Pjzl 票据种类 3 否必选
发票、财政票据、
其他类票据
Pjhm 票据号码 3 否可选
Pjdm 票据代码 3 否必选
ApplyAmount 报销申请金额 3 否必选
DeductAmount 核减金额 3 否必选
ApprovedAmount 报销核准金额 3 否必选
PayTypeName 支付方式名称 3 否必选
PayerAcctBankName 收款银行 3 否必选
PayeeAcctNo 收款账号 3 否必选
PayeeBankName 收款方账户开户行名称 3 否必选
CostDetail 费用明细 3 是可选
CostType 费用类型 4 否必选
CostTypeCode 费用类型代码 4 否必选
CostApprovedAmount 费用核准金额 4 否必选
ApprovalInfoData
公务用车运行维护费报销审
批单审批明细 2 是 —
Name 姓名 3 否必选
642

DA/T

95—2022

表 A.14 公务用车运行维护费报销审批单元数据及组织方式(续)
标识符元素名称级次
是否
为容器型
元数据
约束性备注
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Role 审批角色 3 否必选
Date 审批日期 3 否必选
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
PermissionApprovalInfoData
公务用车运行维护费报销审
批单特殊事项审批明细 2 是 —
Name 姓名 3 否必选
Department 部门 3 否必选
Post 职务 3 否可选
Title 职称 3 否可选
Rank 职级 3 否可选
Role 审批角色 3 否必选
Date 审批日期 3 否必选
Time 审批时间 3 否必选
Status 审批状态 3 否必选
Comment 审批理由 3 否可选
742

DA/T

95—2022

附录 B
(规范性)
单位从外部接收的电子原始凭证的元数据和组织方式
B.1 支付回单
表 B.1规定了支付回单主单的元数据及组织方式。
表 B.1 支付回单主单元数据及组织方式
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
AuthorizedPaymentVoucher 财政授权支付凭证 1 是 —
Id 凭证号 2 否必选
AdmDivCode 行政区划代码 2 否必选
StYear 业务年度 2 否必选
VtCode 凭证类型编号 2 否必选
VouDate 凭证日期 2 否必选
VoucherNo 凭证号 2 否必选
FundTypeCode 资金性质编码 2 否可选
FundTypeName 资金性质名称 2 否可选
BgtTypeCode 预算类型编码 2 否必选
参见《国库集中支
付电子化管理接
口报文规范 》代
码表
BgtTypeName 预算类型名称 2 否可选
参见《国库集中支
付电子化管理接
口报文规范 》代
码表
PayTypeCode 支付方式编码 2 否可选
PayTypeName 支付方式名称 2 否可选
ProCatCode 收支管理编码 2 否必选
参见《国库集中支
付电子化管理接
口报文规范 》代
码表
ProCatName 收支管理名称 2 否可选
参见《国库集中支
付电子化管理接
口报文规范 》代
码表
842

DA/T

95—2022

表 B.1 支付回单主单元数据及组织方式(续)
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
MOFDepCode 业务处室编码 2 否可选
MOFDepName 业务处室名称 2 否可选
SupDepCode 一级预算单位编码 2 否可选
SupDepName 一级预算单位名称 2 否可选
AgencyCode 基层预算单位编码 2 否必选
单位对应的预算
编码
AgencyName 基层预算单位名称 2 否必选单位名称
ExpFuncCode 支出功能分类科目编码 2 否必选
经费列支渠道对
应的支出功能科
目编码
ExpFuncName 支出功能分类科目名称 2 否可选
经费列支渠道对
应的支出功能科
目名称
ExpFuncCode1 支出功能分类类编码 2 否可选
ExpFuncName1 支出功能分类类名称 2 否可选
ExpFuncCode2 支出功能分类款编码 2 否可选
ExpFuncName2 支出功能分类款名称 2 否可选
ExpFuncCode3 支出功能分类项编码 2 否可选
ExpFuncName3 支出功能分类项名称 2 否可选
GovExpEcoCode
政府预算支出经济分类科目
编码 2 否必选
GovExpEcoName
政府预算支出经济分类科目
名称 2 否可选
DepExpEcoCode
部门预算支出经济分类科目
编码 2 否可选
DepExpEcoName
部门预算支出经济分类科目
名称 2 否可选
DepProCode 预算项目编码 2 否可选
对应预算信息关
联码
DepProName 预算项目名称 2 否可选
SetModeCode 结算方式编码 2 否可选
SetModeName 结算方式名称 2 否可选
PayBankCode 代理银行编码 2 否可选
代理银行分支机
构,财政提供可
为空
942

DA/T

95—2022

表 B.1 支付回单主单元数据及组织方式(续)
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
PayBankName 代理银行名称 2 否可选
代理银行分支机
构,财政提供可
为空
ClearBankCode 清算银行编码 2 否可选
ClearBankName 清算银行名称 2 否可选
ClearAcctNo 清算账号 2 否可选
ClearAcctName 清算账户名称 2 否可选
ClearAcctBankName 清算账号开户行 2 否可选
PayeeAcctNo 收款人账号 2 否可选
PayeeAcctName 收款人名称 2 否可选
PayeeAcctBankName 收款人银行 2 否可选
PayeeAcctBankNo 收款人银行行号 2 否可选
PayAcctNo 付款人账号 2 否必选
预算单位零余额
账户

PayAcctName 付款人名称 2 否必选
预算单位零余额
账户名称
PayAcctBankName 付款人银行 2 否必选
预算单位零余额
账户银行名称
PaySummaryCode 用途编码 2 否可选
PaySummaryName 用途名称 2 否可选
PayAmt 支付金额 2 否必选正数为正常支付
PayMgrCode 支付类型编码 2 否可选编码默认为 0,名
称为空PayMgrName 支付类型名称 2 否可选
FundDealModeCode 办理方式编码 2 否可选名称默认为自助
柜面,编码默认
为2FundDealModeName 办理方式名称 2 否可选
AcessAuthGroupCode 自助柜面业务权限分组标识 2 否可选
用于自助柜面系
统用户访问权限
控制,由预算单位
与代理银行签订
自助柜面协议时
约定可为空
052

DA/T

95—2022

表 B.1 支付回单主单元数据及组织方式(续)
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
BusinessTypeCode 业务类型编码 2 否可选编码默认为 2,名
称默认为公务卡BusinessTypeName 业务类型名称 2 否可选
TaxBillNo 申报完税凭证号 2 否可选
TaxayerID 纳税人识别号 2 否可选
TaxOrgCode 税务征收机关代码 2 否可选
用于税费缴纳业
务关联纳税申报
信息可为空
PayCode 缴款识别码 2 否可选用于非税缴款
GovProcurementID 政府采购交易识别码 2 否可选用于政府采购
CheckNo 支票号(结算号) 2 否可选
XPayDate 实际支付日期 2 否必选
代理银行在回单
中补录
XFundDealModeCode 实际办理方式编码 2 否必选
代理银行在回单
中补录
XFundDealModeName 实际办理方式名称 2 否必选
代理银行在回单
中补录
XAgentBusinessNo 银行交易流水号 2 否必选
主单无收款人的
可以为空
XCheckNo 支票号(结算号) 2 否可选
代理银行在回单
中补录
XPayAmt 实际支付金额 2 否必选
代理银行在回单
中补录
XPayeeAcctBankName 实际收款人银行 2 否必选
XPayeeAcctNo 实际收款人账号 2 否必选
XPayeeAcctName 实际收款人全称 2 否必选
代理银行在回单
中补录,主单无收
款人的可以为空
Hold1 预留字段1 2 否必选
Hold2 预留字段2 2 否可选
Items 支付明细信息 2 是 —
Id 支付明细Id 3 否必选主键
VoucherBillId 财政授权支付凭证Id 3 否必选
与主单 Id 内容
一致
VoucherBillNo 财政授权支付凭证单号 3 否必选
与主单 VoucherNo
内容一致
VoucherDetailNo 支付申请序号 3 否可选
152

DA/T

95—2022

表 B.1 支付回单主单元数据及组织方式(续)
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
FundTypeCode 资金性质编码 3 否可选
FundTypeName 资金性质名称 3 否可选
BgtTypeCode 预算类型编码 3 否必选
参见《国库集中支
付电子化管理接
口报文规范 》代
码表
BgtTypeName 预算类型名称 3 否可选
参见《国库集中支
付电子化管理接
口报文规范 》代
码表
ProCatCode 收支管理编码 3 否必选
参见《国库集中支
付电子化管理接
口报文规范 》代
码表
ProCatName 收支管理名称 3 否可选
参见《国库集中支
付电子化管理接
口报文规范 》代
码表
PayKindCode 支出类型编码 3 否必选
参见《国库集中支
付电子化管理接
口报文规范 》代
码表
PayKindName 支出类型名称 3 否可选
参见《国库集中支
付电子化管理接
口报文规范 》代
码表
SupDepCode 一级预算单位编码 3 否可选
SupDepName 一级预算单位名称 3 否可选
AgencyCode 基层预算单位编码 3 否必选
与主单一致
单位对应的单位
预算编码
AgencyName 基层预算单位名称 3 否必选
与主单一致
单位名称
252

DA/T

95—2022

表 B.1 支付回单主单元数据及组织方式(续)
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
ExpFuncCode 支出功能分类科目编码 3 否必选
与主单一致
经费列支渠道对
应的功能分类科
目编码
ExpFuncName 支出功能分类科目名称 3 否可选
与主单一致
经费列支渠道对
应功能分类科目
名称
GovExpEcoCode
政府预算支出经济分类科目
编码 3 否必选
与主单一致
经费列支渠道对
应的部门预算经
济分类科目编码
GovExpEcoName
政府预算支出经济分类科目
名称 3 否可选
与主单一致
经费列支渠道对
应的部门预算经
济分类科目名称
DepExpEcoCode
部门预算支出经济分类科目
编码 3 否可选
DepExpEcoName
部门预算支出经济分类科目
名称 3 否可选
DepProCode 预算项目编码 3 否可选
对应预算信息关
联码
DepProName 预算项目名称 3 否可选
TrackingID 业务追溯识别码 3 否可选
PayeeAcctNo 收款人账号 3 否必选
公务卡业务收款
人必须有值
PayeeAcctName 收款人名称 3 否必选
PayeeAcctBankName 收款人银行 3 否必选
PayeeAcctBankNo 收款人银行行号 3 否可选
PayAmt 支付金额 3 否必选
正常支付不能为
负,退款、更正业
务允许为负
XPayDate 实际支付日期 3 否必选
XAgentBusinessNo 银行交易流水号 3 否必选
XCheckNo 支票号(结算号) 3 否可选
352

DA/T

95—2022

表 B.1 支付回单主单元数据及组织方式(续)
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
XPayAmt 实际支付金额 3 否必选
批量中出现明细支
付失败时填写
0.00
XAddWord 附言 3 否可选
批量中出现明细
支付失败时填写
失败原因
XPayeeAcctBankName 收款人银行 3 否必选
代理银行在回单
中补录
XPayeeAcctNo 收款人账号 3 否必选
代理银行在回单
中补录
XPayeeAcctName 收款人全称 3 否必选
代理银行在回单
中补录
Remark 备注 3 否可选默认为公务卡还款
B.2 交通客票
表 B.2规定了交通客票的元数据及组织方式。
表 B.2 交通客票元数据及组织方式
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
TrafficTicket 交通客票 1 是 —
VoucherType 凭证类型 2 否必选
TicketNo 凭证号 2 否必选
凭证唯一号,由开
具方提供
Carrier 承运人 2 否必选
GPNo 政采票号 2 否可选机票特有
PassengerName 乘客姓名 2 否必选
PassengerID 乘客有效身份证件号码 2 否必选
IssuedBy 开具单位 2 否必选
IssuedDate 开具日期 2 否必选
Fare 票价 2 否必选
DevelopmentFond 民航发展基金 2 否可选
FuelSurcharge 燃油附加费 2 否可选
452

DA/T

95—2022

表 B.2 交通客票元数据及组织方式(续)
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
OtherTaxes 其他税费 2 否可选
Total 合计 2 否可选
GMF_NSRSBH 购买方纳税人识别号 2 否可选
GMF_MC 购买方名称 2 否可选
GMF_DZDH 购买方地址、电话 2 否可选
GMF_YHZH 购买方银行账号 2 否可选
AddInfo 附加信息 2 否可选
Attachment 附件 2 否可选
为 base64 编码的
文件二进制流
AttachmentType 附件格式 2 否可选
文件格式, 如:
OFD 等
AttachmentDescripsion 附件说明 2 否可选附件内容描述
Journeys 交通客票行程明细信息 2 是 —
NotValidBefore 客票生效日期 3 否可选
NotValidAfter 有效截止日期 3 否可选
DepCity 出发城市 3 否可选
From 出发地 3 否可选
机场及航站楼、车
站、港口
ArrCity 到达城市 3 否可选
To 到达地 3 否可选
机场及航站楼、车
站、港口
DepDate 出发日期 3 否必选
DepTime 出发时间 3 否必选
ArrDate 到达日期 3 否可选
ArrTime 到达时间 3 否可选
TransNo 行程班次 3 否必选
航空客票:航班号
铁路客票:车次
公路客票:车次号
水路客票:航次号
ClassLevel 席别 3 否必选
航空客票:舱位
等级
铁路客票:席别
公路客票:车种
水路客票:舱位
等级
552

DA/T

95—2022

表 B.2 交通客票元数据及组织方式(续)
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
ClassCode 舱位代码 3 否可选用于航空客票
Carriage 车厢号 3 否可选
用于铁路客票:
2位字符串,取值
范围:01-99
Seat 座位/铺位号 3 否可选
ShipName 船名 3 否可选用于水路客票
FareBasis
客票级别/
客票类别 3 否可选
GuidePrice 舱位基准价 3 否可选
ClassBottomPrice 舱位底价 3 否可选
GPPrice 舱位政采价 3 否可选
BottomPrice 航班底价 3 否必选
Rescheduled 改签标识 3 否可选
B.3 交通客票退票单
表 B.3规定了交通客票退票单的元数据及组织方式。
表 B.3 交通客票退票单元数据及组织方式
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
TrafficRefundTicket 交通客票退票单 1 是 —
TicketNo 凭证号 2 否必选
由开具方提供的
凭证唯一号
IssuedBy 开票单位 2 否必选
IssuedDate 开具日期 2 否必选
Fare 退票费 2 否必选
PassengerID 乘客有效身份证件号 2 否必选
PassengerName 乘客姓名 2 否必选
OriginalTicketNo 原凭证号 2 否必选
被退订的电子凭
证凭证号
OriginalIssuedBy 原凭证开具单位 2 否必选
被退订的电子凭
证开具单位
652

DA/T

95—2022

表 B.3 交通客票退票单元数据及组织方式(续)
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
OriginalVoucherType 原凭证类型 2 否必选
被退订的电子凭
证类型
OriginalFare 原凭证票价 2 否必选
被退订的电子凭
证票价
GMF_NSRSBH 购买方纳税人识别号 2 否可选
GMF_MC 购买方名称 2 否可选
GMF_DZDH 购买方地址、电话 2 否可选
GMF_YHZH 购买方银行账号 2 否可选
Attachment 附件 2 否可选
AttachmentType 附件格式 2 否可选
AttachmentDescripsion 附件说明 2 否可选
B.4 交通客票改签单
表 B.4规定了交通客票改签单的元数据及组织方式。
表 B.4 交通客票改签单元数据及组织方式
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
TrafficChangeTicket 交通客票改签单 1 是 —
TicketNo 凭证号 2 否必选
GPNo 政采票号 2 否可选
IssuedBy 开具单位 2 否必选
IssuedDate 开具日期 2 否必选
Fare 改签费 2 否必选
PassengerID 乘客有效身份证件号 2 否必选
PassengerName 乘客姓名 2 否必选
OriginalTicketNo 原凭证号 2 否必选
OriginalIssuedBy 原凭证开具单位 2 否必选
OriginalVoucherType 原凭证类型 2 否必选
OriginalFare 原凭证票价 2 否必选
GMF_NSRSBH 购买方纳税人识别号 2 否可选
752

DA/T

95—2022

表 B.4 交通客票改签单元数据及组织方式(续)
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
GMF_MC 购买方名称 2 否可选
GMF_DZDH 购买方地址、电话 2 否可选
GMF_YHZH 购买方银行账号 2 否可选
Attachment 附件 2 否可选
AttachmentType 附件格式 2 否可选
AttachmentDescripsion 附件说明 2 否可选
TicketChangingItems 改签明细信息 2 是 —
From 改签后的出发地 3 否必选
To 改签后的到达地 3 否必选
DepDate 改签后出发日期 3 否必选
DepTime 改签后出发时间 3 否必选
ArrDate 改签后到达日期 3 否必选
ArrTime 改签后到达时间 3 否必选
Carrier 改签后承运人 3 否必选
TransNo 改签后行程批次号 3 否必选
ClassLevel 改签后席别 3 否必选
Carriage 改签后车厢号 3 否可选
Seat 改签后座位号 3 否必选
FareBasis 改签后客票级别/客票类别 3 否可选
NotValidBefore 改签后客票生效日期 3 否可选
NotValidAfter 改签后有效截止日期 3 否可选
Fare 改签后票价 3 否必选
GuidePrice 舱位基准价 3 否必选
ClassBottomPrice 舱位底价 3 否必选
GPPrice 舱位政采价 3 否可选
BottomPrice 航班底价 3 否必选
B.5 交通客票订票费单
表 B.5规定了交通客票订票费单的元数据及组织方式。
852

DA/T

95—2022

表 B.5 交通客票订票费单元数据及组织方式
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
TrafficBookingInvoice 交通客票订票费 1 是 —
TicketNo 凭证号 2 否必选
IssuedBy 开票单位 2 否必选
IssuedDate 开具日期 2 否必选
Fare 代理费 2 否必选
Poundage 刷卡手续费 2 否必选
Total 合计 2 否必选
PassengerID 乘客有效身份证件号 2 否必选
PassengerName 乘客姓名 2 否必选
OriginalTicketNo 关联凭证号 2 否必选
OriginalIssuedBy 关联凭证开具单位 2 否必选
OriginalVoucherType 关联凭证类型 2 否必选
OriginalFare 关联凭证票价 2 否必选
GMF_NSRSBH 购买方纳税人识别号 2 否可选
GMF_MC 购买方名称 2 否可选
GMF_DZDH 购买方地址、电话 2 否可选
GMF_YHZH 购买方银行账号 2 否可选
Attachment 附件 2 否可选
AttachmentType 附件格式 2 否可选
AttachmentDescripsion 附件说明 2 否可选
B.6 住宿单
表 B.6规定了住宿单的元数据及组织方式。
表 B.6 住宿单元数据及组织方式
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
HotelInvoice 住宿单 1 是 —
VoucherType 凭证类型 2 否必选
TicketNo 凭证号 2 否必选
IssuedBy 开具单位 2 否必选
952

DA/T

95—2022

表 B.6 住宿单元数据及组织方式(续)
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
IssuedDate 开具日期 2 否必选
Hotel 酒店名称 2 否必选
Location 酒店地址 2 否必选
ArrDate 入住日期 2 否必选
DepDate 离店日期 2 否必选
Fare 住宿总费用 2 否必选
GMF_NSRSBH 购买方纳税人识别号 2 否可选
GMF_MC 购买方名称 2 否可选
GMF_DZDH 购买方地址、电话 2 否可选
GMF_YHZH 购买方银行账号 2 否可选
Attachment 附件 2 否可选
AttachmentType 附件格式 2 否可选
AttachmentDescripsion 附件说明 2 否可选
Detail 入住明细信息 2 是 —
PassengerID 住宿人有效身份证件号 3 否必选
PassengerName 住宿人姓名 3 否必选
ArrDate 入住日期 3 否必选
DepDate 离店日期 3 否必选
RoomNo 房间号 3 否可选
Fare 住宿费 3 否必选
B.7 住宿退改单
表 B.7规定了住宿退改单的元数据及组织方式。
表 B.7 住宿退改单元数据及组织方式
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
HotelRefundInvoice 住宿退改单 1 是 —
VoucherType 凭证类型 2 否必选
TicketNo 凭证号 2 否必选
IssuedBy 开具单位 2 否必选
062

DA/T

95—2022

表 B.7 住宿退改单元数据及组织方式(续)
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
IssuedDate 开具日期 2 否必选
Hotel 酒店名称 2 否必选
Location 酒店地址 2 否必选
ArrDate 预订入住日期 2 否必选
DepDate 预订离店日期 2 否必选
Nights 晚数 2 否必选
CancelDate 取消日期 2 否必选
CancelTime 取消时间 2 否必选
Fare 违约金 2 否必选
PaidAmount 预付金额 2 否必选
CancelRule 预付规则 2 否必选
PassengerID 住宿人有效身份证件号 2 否必选
PassengerName 住宿人姓名 2 否必选
GMF_NSRSBH 购买方纳税人识别号 2 否可选
GMF_MC 购买方名称 2 否可选
GMF_DZDH 购买方地址、电话 2 否可选
GMF_YHZH 购买方银行账号 2 否可选
Attachment 附件 2 否可选
AttachmentType 附件格式 2 否可选
AttachmentDescripsion 附件说明 2 否可选
B.8 采购发票
表 B.8规定了采购发票的元数据及组织方式。
表 B.8 采购发票元数据及组织方式
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
Invoice 采购发票 1 是 —
Fpzl 发票种类 2 否必选
专用发票或普通
发票
Fphm 发票号码 2 否必选
162

DA/T

95—2022

表 B.8 采购发票元数据及组织方式(续)
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
Lbdm 类别代码 2 否必选
参见国税局规定
的发票分类代码
的编码规则
Hsbz 商品编码版本号 2 否必选
Hsbz 含税标志 2 否必选
含税标志:
0:不含税税率;1:
含税税率;2:差
额税
Kprq 开票日期 2 否必选
Hjje 合计金额 2 否必选
Hjse 合计税额 2 否必选
Xfsh 销方名称 2 否必选销售方单位名称
Xfsh 销方纳税人识别号 2 否可选
Xfdzdh 销方地址电话 2 否可选
Xfyhzh 销方银行账号 2 否可选
Gfsh 购方税号 2 否可选
Gfmc 购方名称 2 否必选
Gfdzdh 购方地址电话 2 否可选
Gfyhzh 购方银行账号 2 否可选
Kpr 开票人 2 否必选
Fhr 复核人 2 否必选
Skr 收款人 2 否必选
Attachment 附件 2 否可选
为 base64 编码的
文件二进制流
AttachmentType 附件格式 2 否可选
文件格式, 如:
OFD 等
AttachmentDescripsion 附件说明 2 否可选附件内容描述
Bz 备注 2 否可选
Items 发票明细信息 2 是 —
Xh 序号 3 否必选
262

DA/T

95—2022

表 B.8 采购发票元数据及组织方式(续)
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
Spmc 商品名称/项目名称 3 否必选
Spbm 商品编码 3 否可选
Qyspbm 企业商品编码 3 否可选
Syyhzcbz 优惠政策标识 3 否必选 0:不使用;1:使用
Yhzcsm 优惠政策说明 3 否可选
Lslbz 零税率标识 3 否可选
空:非零税率;0:
出口退税;1:免
税;2:不征收;3:
普通零税率
Ggxh 规格型号 3 否可选
Jldw 计量单位 3 否可选
Sl 数量 3 否必选
Dj 单价 3 否必选
Je 金额 3 否必选数量×单价
Slv 税率 3 否必选
Se 税额 3 否必选
Cph 车牌号 3 否可选
Lx 类型 3 否可选
Txrqq 通行日期起 3 否可选
Txrqz 通行日期止 3 否可选
Result 查验结果 2 否可选票据真伪状态
Attachments 附件信息 2 是 —
Attachment 附件 3 否可选
为 base64 编码的
文件二进制流
AttachmentType 附件格式 3 否可选
文件格式, 如:
OFD 等
AttachmentDescripsion 附件说明 3 否可选
附件文件标题,或
附件内容描述
362

DA/T

95—2022

B.9 保险费凭证
表 B.9规定了保险费凭证的元数据及组织方式。
表 B.9 保险费凭证元数据及组织方式
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
InsuranceVoucher 保险费凭证 1 是 —
VoucherType 凭证类型 2 否必选
PolicyNo 凭证号 2 否必选
IssuedBy 开具单位 2 否必选
IssuedDate 开具日期 2 否必选
PolicyName 保险名称 2 否必选
Insurer 承保方 2 否必选
Insured 被保人姓名 2 否必选
DateOfBirth 被保人出生日期 2 否必选
IDType 被保人证件类型 2 否必选
ID 被保人证件号码 2 否必选
Fare 保费 2 否必选
StartDate 保险起始日期 2 否必选
StartTime 保险起始时间 2 否必选
EndDate 保险终止日期 2 否必选
EndTime 保险终止时间 2 否必选
GMF_NSRSBH 购买方纳税人识别号 2 否可选
GMF_MC 购买方名称 2 否可选
GMF_DZDH 购买方地址、电话 2 否可选
GMF_YHZH 购买方银行账号 2 否可选
Attachment 附件 2 否可选
为 base64 编码的
文件二进制流
AttachmentType 附件格式 2 否可选
文件格式, 如:
OFD 等
AttachmentDescripsion 附件说明 2 否可选附件内容描述
InsuranceItems 保险费明细 2 是 —
Item 保险项目 3 否必选
Coverage 保额 3 否必选
462

DA/T

95—2022

B.10 医疗费票据
表 B.10规定了医疗费票据的元数据及组织方式。
表 B.10 医疗费票据元数据及组织方式
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
MedicalBills 医疗费票据 1 是 —
EinvoiceName 电子票据名称 2 否可选
EinvoiceCode 电子票据代码 2 否可选
EinvoiceNumber 电子票据号码 2 否可选
RandomNumber 校验码 2 否可选
TotalAmount 票据总金额 2 否必选
IssueDate 开票日期 2 否可选
IssueTime 开票时间 2 否可选
AgencyName 开票单位名称 2 否必选
AgencyCode 开票单位代码 2 否可选
PayerCode 交款人代码 2 否可选
PayerName 交款人名称 2 否可选
BizCode 业务流水号 2 否可选
Remark 备注 2 否可选
HandlingPerson 开票人 2 否可选
Checker 复核人 2 否可选
SupervisorRemark 财政部门备注 2 否可选
RelatedInvoiceCode 关联票据代码 2 否可选
RelatedInvoiceNumber 关联票据号码 2 否可选
Items 项目 2 是 —
ItemCode 项目编码 3 否可选
ItemName 项目名称 3 否可选
ItemQuantity 数量 3 否可选
ItemUnit 单位 3 否可选
ItemStd 标准 3 否可选
ItemRemark 备注 3 否可选
ItemAmount 项目金额 3 否可选
AuxItems 项目辅助明细 2 是 —
AuxItemRelatedCode 对应项目编码 3 否可选
562

DA/T

95—2022

表 B.10 医疗费票据元数据及组织方式(续)
元素名称中文标签级次
是否
为容器型
元数据
约束性备注
AuxItemRelatedName 对应项目名称 3 否可选
AuxItemCode 收费明细项目编码 3 否可选
AuxItemName 收费明细项目名称 3 否可选
AuxItemQuantity 收费明细数量 3 否可选
AuxItemUnit 收费明细单位 3 否可选
AuxItemStd 收费明细标准 3 否可选
AuxItemRemark 收费明细备注 3 否可选如:有无自付
AuxItemAmount 收费明细项目金额 3 否可选
FinanceCheck 票据查验结果 2 是 —
Result 查验结果 3 否可选
票据真伪状态,已
查验、未查验
IsExchangePaper 是否已打印纸票 3 否可选
RelatedInvoiceCode 纸质票据代码 3 否可选
RelatedInvoiceNumber 纸质票据号码 3 否可选
IsReversal 是否开红票 3 否可选
ReversalInvoiceCode 红票票据代码 3 否可选
ReversalInvoiceNumber 红票票据号码 3 否可选
IsAccounted 是否已入账 3 否可选
AccDate 入账日期 3 否可选
AccAmount 入账金额 3 否可选
Attachments 电子凭证附件 2 是 —
Attachment 附件 3 否可选
为 base64 编码的
文件二进制流
AttachmentType 附件格式 3 否可选
文件格式, 如:
OFD 等
AttachmentDescripsion 附件说明 3 否可选
附件文件标题,或
附件内容描述
662

DA/T

95—2022

附录 C
(规范性)
电子记账凭证元数据及组织方式
表 C.1规定了电子记账凭证的元数据及组织方式。
表 C.1 电子记账凭证元数据及组织方式
标识符元素名称级次
是否
为容器型
元数据

约束性
VOUCHER 记账凭证 1 是 —
MOF_DIV_CODE 财政区划代码 2 否必选
FISCAL_YEAR 会计年度 2 否必选
ACCT_SET_CODE 账套编号 2 否必选
ACCT_PERIOD 会计期间 2 否必选
AGENCY_CODE 单位代码 2 否必选
AGENCY_ACCT_VOUCHER
_TYPE
单位会计记账凭证类型 2 否必选
VOUCHER_NO 记账凭证号 2 否必选
VOUCHER_ABS 凭证摘要 2 否必选
INPUTER 制单人 2 否必选
INPUTER_DATE 制单日期 2 否必选
AUDITOR 审核人 2 否必选
AUDITOR_DATE 审核日期 2 否必选
CHECKER 出纳人 2 否可选
CHECKER_DATE 出纳日期 2 否可选
POSTER 记账人 2 否必选
POSTER_DATE 记账日期 2 否必选
VOUCHER_DATE 记账凭证日期 2 否必选
FI_LEADER 财务负责人 2 否必选
CR_AMT 财务贷方金额 2 否必选
DR_AMT 财务借方金额 2 否必选
CR_YS_AMT 预算贷方金额 2 否必选
DR_YS_AMT 预算借方金额 2 否必选
VOU_CNT 附件数 2 否可选
RED_FLAG 红冲状态 2 否必选
762

DA/T

95—2022

表 C.1 电子记账凭证元数据及组织方式(续)
标识符元素名称级次
是否
为容器型
元数据

约束性
RED_VOU_NO 红冲记账凭证号 2 否可选
IS_ADJUST_PERIOD 是否调整期 2 否必选
VOU_STATUS 凭证状态 2 否必选
IS_CARRY_OVER 是否为结转凭证 2 否必选
VOU_DET 记账凭证分录信息 2 是 —
VOU_DET_ID 凭证分录唯一标识 3 否必选
VOU_ID 凭证主表唯一标识 3 否必选
MOF_DIV_CODE 财政区划代码 3 否必选
FISCAL_YEAR 会计年度 3 否必选
ACCT_SET_CODE 账套编号 3 否必选
VOU_SEQ 记账凭证分录序号 3 否必选
VOU_DET_DESC 会计分录摘要 3 否必选
GOV_ACCT_CLS_CODE 单位会计科目代码 3 否必选
GOV_ACCT_CLS_NAME 单位会计科目名称 3 否必选
DR_CR 借贷方向 3 否必选
AMT 金额 3 否必选
FOREIGN_AMT 外币金额 3 否可选
EXT_RAT 汇率 3 否可选
CURRENCY_CODE 币种代码 3 否可选
QTY 数量 3 否可选
PRO_CODE 项目代码 3 否可选
DEP_BGT_ECO_CODE 部门支出经济分类代码 3 否可选
GOV_BGT_ECO_CODE 政府支出经济分类代码 3 否可选
DEPARTMENT_CODE 部门代码 3 否可选
EMPLOYEE_CODE 人员代码 3 否可选
FUND_ TRAOBJ_ TYPE
_CODE
资金往来对象类别代码 3 否可选
FUND_TRA_OBJ_NO 资金往来对象编码 3 否可选
FUND_TRA_OBJ_NAME 资金往来对象名称 3 否可选
END_DATE 到期日 3 否可选
EXP_FUNC_CODE 支出功能分类科目代码 3 否可选
FUND_TYPE_CODE 资金性质代码 3 否可选
862

DA/T

95—2022

表 C.1 电子记账凭证元数据及组织方式(续)
标识符元素名称级次
是否
为容器型
元数据

约束性
FOUND_TYPE_CODE 资金来源代码 3 否可选
PAY_BUS_TYPE_CODE 支付业务类型代码 3 否可选
PAY_TYPE_CODE 支付方式代码 3 否可选
SET_MODE_CODE 结算方式代码 3 否可选
PUR_MET_CODE 政府采购方式代码 3 否可选
FATYPE_CODE 资产分类代码 3 否可选
COST_TYPE_CODE 费用经济性质代码 3 否可选
BILL_DATE 票据日期 3 否可选
COR_BGT_DOC_NO 本级指标文号 3 否可选
BUDGET_LEVEL_CODE 预算级次代码 3 否可选
SUP_BGT_DOC_NO 上级指标文号 3 否可选
ORI_PRO_CODE 来源项目代码 3 否可选
BGT_TYPE_CODE 指标类型代码 3 否可选
MOF_DEP_CODE 财政内部机构代码 3 否可选
REMARK 备注 3 否可选
962

DA/T

95—2022

附录 D
(规范性)
机构人员实体元数据及组织方式
表 D.1规定了机构人员实体的元数据及组织方式。
表 D.1 机构人员实体元数据及组织方式
标识符元素名称级次
是否
为容器型
元数据
约束性
AgentType 机构人员类型 1 否可选
AgentName 机构人员名称 1 否必选
OrganizationCode 组织机构代码 1 否可选
FinanceDivisionCode 财政区划代码 1 否可选
PositionName 个人职位 1 否可选
072

DA/T

95—2022

附录 E
(规范性)
业务实体元数据及组织方式
表 E.1规定了电子会计凭证在电子化归集、报销、入账、归档、移交和档案管理业务中的相关元数
据及组织方式。
表 E.1 业务实体元数据及组织方式
标识符元素名称级次
是否
为容器型
元数据
约束性
BusinessStatus 业务状态 1 否必选
BusinessActivity 业务行为 1 否必选
ActionTime 行为时间 1 否必选
ActionMandate 行为依据 1 否可选
ActionDescription 行为描述 1 否可选
172

DA/T

95—2022

附录 F
(规范性)
电子会计凭证信息包组织方式
图 F.1规定了电子会计凭证信息包的组织方式。
图 F.1 电子会计凭证信息包的组织方式
272

DA/T

95—2022

参考文献
  [1] GB/T

18894—2016 电子文件归档与电子档案管理规范
[2] 财政部、国家档案局.会计档案管理办法.2015年12月11日.
[3] 国家档案局.电子档案管理系统基本功能要求.2017年12月15日.
[4] 财政部、国家档案局.关于规范电子会计凭证报销入账归档的通知.2020年3月23日.
[5] 财政部.会计基础工作规范.2019年3月14日.
[6] 财政部.财政票据管理办法.2012年10月22日.
[7] 中华人民共和国电子签名法(2019修正).2019年4月23日.
[8] 财政部、中国人民银行.国库集中支付电子化管理接口报文规范.2019年12月25日.
[9] 财政部.关于印发《预算管理一体化系统技术标准 V1.0》的通知.2020年3月17日.
[10] 财政部.关于印发《中央和国家机关差旅费管理办法》的通知.2013年12月31日.
[11] 财政部、国家机关事务管理局、中共中央直属机关事务管理局.关于印发《中央和国家机关会
议费管理办法》的通知.

2013年9月13日.
[12] 财政部、中共中央组织部、国家公务员局.关于印发《中央和国家机关培训费管理办法》的通
知.2016年12月27日.
[13] 财政部、外交部.关于印发《因公临时出国经费管理办法》的通知.2013年12月20日.
[14] 财政部、国家卫生健康委、国家医疗保障局.关于全面推行医疗收费电子票据管理改革的通
知.2019年7月22日.
[15] 中共中央办公厅、国务院办公厅.党政机关国内公务接待管理规定.2013年12月1日.
[16] 中共中央办公厅、国务院办公厅.党政机关公务用车管理办法.2017年12月11日.
372
      </div>
      
      <div className="bg-amber-500/10 border p-8 rounded-3xl border-amber-500/20 mt-16">
          <h3 className="text-xl font-bold text-white mb-4">DigiVoucher 100% 合规落地</h3>
          <p className="text-slate-400 mb-6 leading-relaxed">
              DigiVoucher 系统设计严格遵循 DA/T 95—2022《行政事业单位一般公共预算支出财务报销电子会计凭证档案管理技术规范》 要求。我们确保您的电子档案管理流程在技术层面上完全对标国标与行标，
              实现全生命周期的合规管控。
          </p>
      </div>
    </RegulationLayout>
  );
};

export default DAT95Spec;
