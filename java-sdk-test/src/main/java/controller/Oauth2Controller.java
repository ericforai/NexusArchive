package controller;

import com.alibaba.fastjson.JSONObject;
import com.dbappsecurity.aitrust.appSecSso.HttpUtils;
import com.dbappsecurity.aitrust.appSecSso.InvokeResult;
import com.dbappsecurity.aitrust.appSecSso.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class Oauth2Controller {



	/**
	 * 应用对接单点登录第一步：应用重定向到零信任认证页
	 * redirect_uri 应用地址
	 * client_id 零信任申请的应用ID
	 * state 随机值
	 * 接口请求地址为零信任的认证页地址
	 */
	@RequestMapping(value = "/sso")
	public String loginWithSso() {
		StringBuffer sBuffer = new StringBuffer();
		sBuffer.append("https://192.168.1.103/iam/auth/oauth2/sso").append("?redirect_uri=");
		try {
			sBuffer.append(URLEncoder.encode("redirectUrl", "utf-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		sBuffer.append("&state=").append(1);
		sBuffer.append("&client_id=").append("client_id");
		try {
			return sBuffer.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}




	/**
	 * 根据零信任重定向回来的code换取用户令牌accessToken,根据用户令牌accessToken 获取用户信息
	 * client_secret 零信任申请的应用密钥
	 * client_id 零信任申请的应用ID
	 * 接口请求地址为零信任的认证页地址
	 */
	@RequestMapping(value = "/authBack")
	public void onAuthCodeBack1(HttpServletRequest request) {
		String code = request.getParameter("code");
		try {
            //1、根据code获取accessToken
			List<NameValuePair> tokenNameValuePairs = new ArrayList<>();
			NameValuePair nvp = new BasicNameValuePair("code", code);
			tokenNameValuePairs.add(nvp);
			nvp = new BasicNameValuePair("client_id", "client_id");
			tokenNameValuePairs.add(nvp);
			nvp = new BasicNameValuePair("client_secret", "client_secret");
			tokenNameValuePairs.add(nvp);
			String tokenJson = HttpUtils.getJsonFromServer("https://192.168.1.103/iam/auth/oauth2/accessToken", tokenNameValuePairs);
			InvokeResult tokenResult = JSONObject.parseObject(tokenJson, InvokeResult.class);
			if (tokenResult == null || tokenResult.getCode() != 0) {
				return;
			}
            //2、根据accessToken获取用户信息
			String accessToken = (String) tokenResult.getContent();
			List<NameValuePair> nameValuePairs = new ArrayList<>();
			nvp = new BasicNameValuePair("Authorization", accessToken);
			nameValuePairs.add(nvp);
			String json = HttpUtils.getJsonFromServer("https://192.168.1.103/iam/auth/oauth2/userInfo", nameValuePairs);
			if (StringUtils.isEmpty(json)) {
				return;
			}
			JSONObject jsonObject = JSONObject.parseObject(json);
			String uuid = jsonObject.getString("sub");
			//3、根据返回的用户信息，应用处理自己的业务

		} catch (Exception e) {

		}

	}



	/**
	 * 根据零信任重定向回来的code换取应用令牌appToken,根据appToken 获取用户信息
	 * client_secret 零信任申请的应用密钥
	 * client_id 零信任申请的应用ID
	 * 接口请求地址为零信任的认证页地址
	 */
	@RequestMapping(value = "/authBack")
	public void onAuthCodeBack2(HttpServletRequest request) {
		String code = request.getParameter("code");
		try {
			//1、根据code获取accessToken
			List<NameValuePair> tokenNameValuePairs = new ArrayList<>();
			NameValuePair nvp = new BasicNameValuePair("code", code);
			tokenNameValuePairs.add(nvp);
			nvp = new BasicNameValuePair("client_id", "client_id");
			tokenNameValuePairs.add(nvp);
			nvp = new BasicNameValuePair("client_secret", "client_secret");
			tokenNameValuePairs.add(nvp);
			String tokenJson = HttpUtils.getJsonFromServer("https://192.168.1.103/iam/auth/oauth2/appToken", tokenNameValuePairs);
			InvokeResult tokenResult = JSONObject.parseObject(tokenJson, InvokeResult.class);
			if (tokenResult == null || tokenResult.getCode() != 0) {
				return;
			}
			//2、根据accessToken获取用户信息
			String accessToken = (String) tokenResult.getContent();
			List<NameValuePair> nameValuePairs = new ArrayList<>();
			nvp = new BasicNameValuePair("Authorization", accessToken);
			nameValuePairs.add(nvp);
			String json = HttpUtils.getJsonFromServer("https://192.168.1.103/iam/auth/oauth2/appUserInfo", nameValuePairs);
			if (StringUtils.isEmpty(json)) {
				return;
			}
			JSONObject jsonObject = JSONObject.parseObject(json);
			String uuid = jsonObject.getString("sub");
			//3、根据返回的用户信息，应用处理自己的业务

		} catch (Exception e) {

		}

	}

}
