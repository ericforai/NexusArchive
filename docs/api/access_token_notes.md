获取 access_token
说明
调用接口令牌 access_token 是应用调用开放平台业务接口的凭证，有效期为2小时,过期后需要重新获取 。

用户需要先 开发流程 -> 创建应用，然后获取开发流程 -> 接口授权，获取授权时，会生成应用的 appKey 和 appSecret 的值，租户管理员可将这个两个值交给开发者，开发者可用 appKey 和 appSecret 获取 access_token 进行接口的调用。

在【API授权】页面，租户管理员可以查看到所创建应用的 App Key和Secret值，如下图所示： appkey

access_token 的获取方式为主动调用开放平台的令牌授权接口，该接口说明如下：

请求地址
请求地址	https://dbox.yonyoucloud.com/iuap-api-auth/open-auth/selfAppAuth/getAccessToken
请求方法	GET
请求参数
字段	类型	说明
appKey	string	应用 appKey
timestamp	number long	unix timestamp, 毫秒时间戳
signature	string	校验签名，HmacSHA256，加签方式看下文
加签方式
其中，签名字段signature计算使用HmacSHA256，具体计算方式如下：

URLEncode( Base64( HmacSHA256( parameterMap ) ) )

其中，parameterMap 按照参数名称排序，参数名称与参数值依次拼接(signature字段除外)，形成待计算签名的字符串。

示例 ：若发送请求参数appKey为41832a3d2df94989b500da6a22268747,时间戳timestamp为1568098531823，则待加密字符串的值为 appKey41832a3d2df94989b500da6a22268747timestamp1568098531823

之后对 parameterMap 使用 HmacSHA256 计算签名，Hmac 的 key 为自建应用的 appSecret 。计算出的二进制签名先进行 base64，之后进行 urlEncode，即得到 signatrue 字段的值。具体请求方式请参考 demo。

自建应用获取token示例： 自建应用获取token

(开发者也可以选择SDK方式获取token)
获得 access_token 后开发者就可以调用具体的业务接口，获得具体的业务数据。

java加密的常见问题

1、关于java不同jdk之间的base64加密 ibm中的jdk默认使用sun.misc.BASE64Decoder,sun.misc.BASE64Encoder,这种base64的加密不推荐使用，他跟java.util.Base64不互通，org.apache.commons.codec.binary.Base64是与java.util.Base64互通的，所以如果不同jdk之间存在base64的加解密，IBM的jdk建议使用org.apache.commons.codec.binary.Base64替换sun.misc.BASE64。

2、AES解密报错：java.security.InvalidKeyException: Illegal key size 没错 1.8以及以下 替换local_policy.jar 和US_export_policy.jar 注意：oracle jdk的去oracle官网去下载，ibm的jdk的去ibm官网去下载，否则会出现错误，找不到相关类。

请求示例
https://dbox.yonyoucloud.com/iuap-api-auth/open-auth/selfAppAuth/getAccessToken?appKey=xxx&timestamp=xxx&signature=xxx
返回参数说明
字段	类型	说明
code	String	结果码，正确返回 "00000"
message	String	结果信息，若有错误，该字段会返回具体错误信息
data.access_token	String	接口令牌 access_token
data.expire	number int	有效期，单位秒
返回数据
{
    "code": "00000",
    "message": "成功！",
    "data": {
        "access_token": "b8743244c5b44b8fb1e52a55be7e2f",
        "expire": 7200
    }
}
2023-07-21 地址调整说明
一、开放平台获取token的url进行升级 为了支持与友互通鉴权保持一致，开放平台对获取AccessToken进行改造升级。

自建应用获取token， 路径从/iuap-api-auth/open-auth/selfAppAuth/getAccessToken改为/iuap-api-auth/open-auth/selfAppAuth/base/v1/getAccessToken

生态应用获取token， 路径从/iuap-api-auth/open-auth/suiteApp/getAccessToken改为/iuap-api-auth/open-auth/suiteApp/base/v1/getAccessToken

参数不变

二、调整示例

新的获取自建token 比如预发布是：https://bip-pre.diwork.com/iuap-api-auth/open-auth/selfAppAuth/base/v1/getAccessToken?appKey=app_key&timestamp=1689561299693&signature=5emnxzmPp%2FCFvb2hcddwtMVgfKUARq9DGQZOjxqe%2Fp8%3D

老的获取自建token 比如预发布是：https://bip-pre.diwork.com/iuap-api-auth/open-auth/selfAppAuth/getAccessToken?appKey=app_key&timestamp=1689561299694&signature=ySpMB%2F%2Bmia%2BCbYYGM72rHWHsuyhOMy5Q008ppOBgxWM%3D

新的获取生态token 比如预发布是：https://bip-pre.diwork.com/iuap-api-auth/open-auth/suiteApp/base/v1/getAccessToken?suiteKey=suit_key&tenantId=tenant_id&timestamp=1689561299694&signature=BAunhJpuWTFIV0ZJtoML2Qf0rV8vwlFFjknzQGSOkcY%3D

老的获取生态token 比如预发布是：https://bip-pre.diwork.com/iuap-api-auth/open-auth/suiteApp/getAccessToken?suiteKey=app_key&tenantId=tenant_id&timestamp=1689561299694&signature=oOW3HRzq5epDqaRRDsljlQ6T2k3PecOdyJf45iJVx7Y%3D

三、改造接口需要注意的事项：

改造后调用方式不变，参数不变，请注意保持header中的ContentType是application/json

在代码调用中为了保持编码一致请使用UTF-8

新版本获取的token 会比较长，并且包含特殊字符，请在使用新token 调用openApi时候对token进行encode编码。

go语言 python语言默认会在请求时候对参数进行encode，一般来说不用手动encode

java语言需要 调用java.net.URLEncoder.encode(accessToken)

php语言需要调用urlencode(accessToken)