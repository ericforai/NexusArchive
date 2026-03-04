package com.nexusarchive.service.sso;

import com.nexusarchive.dto.sso.YonSuiteSsoTokenRequest;
import com.nexusarchive.dto.sso.YonSuiteSsoTokenResponse;
import com.nexusarchive.dto.sso.YonSuiteSsoUrlRequest;
import com.nexusarchive.dto.sso.YonSuiteSsoUrlResponse;

public interface YonSuiteSsoBridgeService {

    YonSuiteSsoTokenResponse issueToken(YonSuiteSsoTokenRequest request);

    YonSuiteSsoUrlResponse buildLaunchUrl(YonSuiteSsoUrlRequest request);
}
