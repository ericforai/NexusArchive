package controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dbappsecurity.aitrust.appSecSso.IDPInvoker;
import com.dbappsecurity.aitrust.appSecSso.InvokeResult;
import com.dbappsecurity.aitrust.appSecSso.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import utils.HttpUtil;

import java.util.HashMap;
import java.util.Map;


public class SyncUserController {


	/**
	 * 通过sdk方式获取appToken ,同步用户信息（全量）
	 *
	 * @param pageNum  页面
	 * @param pageSize 总页数最大1000条
	 * @return
	 */
	@RequestMapping(value = "/doSyncUser", method = RequestMethod.POST)
	@ResponseBody
	public void doSyncUser() {
		//获取签名方式可通过以下两种方式（用户可自行选择一种）
		//1、通过sdk方式工程引入app-sec-sso.jar包，从零信任管理后台下载应用的sdk jar包和私钥签名获取应用令牌appToken
		String appToken = getAppTokenBySdk();
		//2、通过接口方式获取应用令牌appToken
		appToken = getAppTokenByRestful();
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("pageNum", 1);
		jsonObject.put("pageSize", 1000);
		Map<String, String> customHeaders = new HashMap<>();
		customHeaders.put("appToken", appToken);
		try {
			String result = HttpUtil.doJsonPostWithHeader("https://192.168.1.1/iam/resource/api/pageUserList", customHeaders, jsonObject.toJSONString());
			if (StringUtils.isEmpty(result)) {
				return;
			}
			int code = JSON.parseObject(result).getInteger("code");
			if (code == 0) {
				String content = JSON.parseObject(result).getString("content");
				//2、根据返回的content 解析组织信息
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * 通过sdk方式获取appToken ,根据组织获取用户信息
	 * @param teamId  为零信任的组织id
	 * @return
	 */
	@RequestMapping(value = "/listUserByTeamId", method = RequestMethod.POST)
	@ResponseBody
	public void listUserByTeamId() {
		//获取签名方式可通过以下两种方式（用户可自行选择一种）
		//1、通过sdk方式工程引入app-sec-sso.jar包，从零信任管理后台下载应用的sdk jar包和私钥签名获取应用令牌appToken
		String appToken = getAppTokenBySdk();
		//2、通过接口方式获取应用令牌appToken
		appToken = getAppTokenByRestful();
		JSONObject jsonObject = new JSONObject();
		//teamId 为零信任的组织id
		jsonObject.put("teamId", "teamId");
		Map<String, String> customHeaders = new HashMap<>();
		customHeaders.put("appToken", appToken);
		try {
			String result = HttpUtil.doJsonPostWithHeader("https://192.168.1.1/iam/resource/api/listUserByTeamId", customHeaders, jsonObject.toJSONString());
			if (StringUtils.isEmpty(result)) {
				return;
			}
			int code = JSON.parseObject(result).getInteger("code");
			if (code == 0) {
				String content = JSON.parseObject(result).getString("content");
				//2、根据返回的content 解析组织信息
			}

		} catch (Exception e) {
			e.printStackTrace();
		}


	}


	/**
	 * 通过sdk方式获取appToken ,根据用户id获取用户信息
	 * @param uuid  为零信任的用户uuid
	 * @return
	 */
	@RequestMapping(value = "/getUserInfo", method = RequestMethod.POST)
	@ResponseBody
	public void getUserInfo() {
		//获取签名方式可通过以下两种方式（用户可自行选择一种）
		//1、通过sdk方式工程引入app-sec-sso.jar包，从零信任管理后台下载应用的sdk jar包和私钥签名获取应用令牌appToken
		String appToken = getAppTokenBySdk();
		//2、通过接口方式获取应用令牌appToken
		appToken = getAppTokenByRestful();
		JSONObject jsonObject = new JSONObject();
		//uuid 为零信任的用户ID
		jsonObject.put("uuid", "uuid");
		Map<String, String> customHeaders = new HashMap<>();
		customHeaders.put("appToken", appToken);
		try {
			String result = HttpUtil.doJsonPostWithHeader("https://192.168.1.1/iam/resource/api/getUserInfo", customHeaders, jsonObject.toJSONString());
			if (StringUtils.isEmpty(result)) {
				return;
			}
			int code = JSON.parseObject(result).getInteger("code");
			if (code == 0) {
				String content = JSON.parseObject(result).getString("content");
				//2、根据返回的content 解析组织信息
			}

		} catch (Exception e) {
			e.printStackTrace();
		}


	}


	private String getAppTokenBySdk() {
		String prikey = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAIIazjyYxPMTPOdr4X482LY7j9xwK+Ck0Err7NlRa6sKnt/9oAXXo61lwJSmO+0NdOgjVfSIHK1p5z0WKyyeHWWafF6FDzIzGOtWtsFYucoJv3Fio0mreXBu5/k8nrMJqi5Et6LKI1a3oH+/U4CpUDRdPoXib7jvdM5rNsu4ZxQ/AgMBAAECgYBtxCfTwCAJ5GUx6jaoxrUfqkjJdmnOcb66Nynwf10TRTadS+HCjBgvpvU/dLCCYyQK5iUS1fM762mIhDeQwSWS6s0B4T9oQswNs/o7ekSShuQ7mQ3b4pBGIQ5ekedYNrqAHPTG/I0bPo9O2W1bhlqk/f+8EWatXn+fyXX4aywJQQJBAMB54YDkd7pkMgpL8vUyJV8/3YI3D14IQapOARYrq8E/XENiQHm5k7piEzoNQjsFhs+Nf3FEFElyy41+14vcM0kCQQCtCzoAQNWJjry+ONeXQW5Gkij5XL2ktPsN61Qskg7UUn2YHjtvTcd0PZo+I38HiJMHsmxQhpmRcjsf2Efg4wNHAkArS79UFRBxlxRCiK8QRMVvVZhoMCZ+ynCq9Hz+Fbi+8Ze5eKJ0PzBh3qnghxb829NlYLjoK548n1v2ai/mQBQxAkAeQEUugCcUeiiS1JsT7TNbEPgqx8S7g4wUHdzEQfBnu9gK/NYFGkLRFmfdjxUI+x5BDTcUSMOWArNFWOkP7n/HAkAvGkPCxAdgyW0rkBQ9pkpqu/hWCs7eY50tWex6BF8rhSM4q6gNiWkrTyTM9+WdiYaqHmFHNbY2LQBd7kqMXelw";
		IDPInvoker.setConfigPriKey(prikey);
		InvokeResult invokeResult = IDPInvoker.applyAppToken();
		String appToken = "";
		if (invokeResult.getCode() == 0) {
			appToken = (String) invokeResult.getContent();
		}
		return appToken;
	}


	private String getAppTokenByRestful() {
		JSONObject param = new JSONObject();
		//应用client_id 和 client_secret 向零信任平台申请
		param.put("client_id", "client_id");
		param.put("client_secret", "client_secret");
		JSONObject jsonObject = HttpUtil.doPost("https://192.168.1.1/iam/auth/api/ext/token", JSONObject.toJSONString(param));
		String appToken = jsonObject.getString("appToken");
		return appToken;
	}


}
