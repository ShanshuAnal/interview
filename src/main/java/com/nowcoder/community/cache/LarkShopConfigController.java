package com.nowcoder.community.cache;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * @Author: 19599
 * @Date: 2026/4/4 14:55
 * @Description:
 */
@Controller
public class LarkShopConfigController {

    @Resource
    private LarkShopConfigService larkShopConfigService;

    @Resource
    private LarkShopConfigBusiness larkShopConfigBusiness;


    @GetMapping("/getShopConfig")
    @ResponseBody
    public LarkShopConfig getShopConfig(@RequestBody LarkShopConfig larkShopConfig) {
        LarkShopConfig config = larkShopConfigBusiness.getShopConfig(larkShopConfig.getStoreCode());
        return config;
    }
}
