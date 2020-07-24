package com.myapps.clouddbreference.cloudDB;

import com.huawei.agconnect.cloud.database.ObjectTypeInfo;
import com.myapps.clouddbreference.model.UserInfo;

import java.util.Arrays;

public class ObjectTypeInfoHelper {
    private final static int FORMAT_VERSION = 1;
    private final static int OBJECT_TYPE_VERSION = 5;

    public static ObjectTypeInfo getObjectTypeInfo() {
        ObjectTypeInfo objectTypeInfo = new ObjectTypeInfo();
        objectTypeInfo.setFormatVersion(FORMAT_VERSION);
        objectTypeInfo.setObjectTypeVersion(OBJECT_TYPE_VERSION);
        objectTypeInfo.setObjectTypes(Arrays.asList(UserInfo.class));
        return objectTypeInfo;
    }
}
