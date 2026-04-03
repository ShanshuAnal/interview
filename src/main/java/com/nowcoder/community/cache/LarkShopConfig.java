package com.nowcoder.community.cache;

import lombok.Data;

import java.util.Date;

@Data
public class LarkShopConfig {
    private Integer id;

    private String shopName;

    private String shopNameFull;

    private String secret;

    private String apiKey;

    private String accountId;

    private Integer storeCode;

    private Integer popCollectMethod;

    private String popCollectMethodName;

    private Integer popRefundMethod;

    private String popRefundMethodName;

    private Integer signCityCode;

    private String signCityName;

    private Integer backupProductId;

    private String specialProductName;

    private String specialProductLine;

    private Integer isSpecialShop;

    private Integer isOnline;

    private String category;

    private Integer operateId;

    private String operateName;

    private Integer delFlag;

    private Date createTime;

    private Date updateTime;

}