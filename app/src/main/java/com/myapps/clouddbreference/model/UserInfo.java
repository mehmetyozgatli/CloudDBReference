package com.myapps.clouddbreference.model;

import com.huawei.agconnect.cloud.database.CloudDBZoneObject;
import com.huawei.agconnect.cloud.database.annotations.DefaultValue;
import com.huawei.agconnect.cloud.database.annotations.NotNull;
import com.huawei.agconnect.cloud.database.annotations.PrimaryKey;

public class UserInfo extends CloudDBZoneObject {
    @PrimaryKey
    private Integer id;

    @NotNull
    @DefaultValue(stringValue = "test")
    private String fullName;

    @NotNull
    @DefaultValue(stringValue = "test")
    private String emailOrPhone;

    @NotNull
    @DefaultValue(stringValue = "test")
    private String verifyCode;

    @NotNull
    @DefaultValue(stringValue = "test")
    private String password;

    @NotNull
    @DefaultValue(stringValue = "test")
    private String pushToken;

    public UserInfo() {
        super();
        this.fullName = "test";
        this.emailOrPhone = "test";
        this.verifyCode = "test";
        this.password = "test";
        this.pushToken = "test";

    }
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setEmailOrPhone(String emailOrPhone) {
        this.emailOrPhone = emailOrPhone;
    }

    public String getEmailOrPhone() {
        return emailOrPhone;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public void setPassword(String password) { this.password = password; }

    public String getPassword() {
        return password;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public String getPushToken() {
        return pushToken;
    }
}
