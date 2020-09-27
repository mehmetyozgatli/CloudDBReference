package com.myapps.clouddbreference.cloudDB;

import com.huawei.agconnect.cloud.database.ObjectTypeInfo;
import com.myapps.clouddbreference.model.UserSurvey;

import java.util.Collections;

/**
 * Definition of ObjectType Helper.

 */

public class ObjectTypeInfoHelper {
    private final static int FORMAT_VERSION = 1;
    private final static int OBJECT_TYPE_VERSION = 8;

    public static ObjectTypeInfo getObjectTypeInfo() {
        ObjectTypeInfo objectTypeInfo = new ObjectTypeInfo();
        objectTypeInfo.setFormatVersion(FORMAT_VERSION);
        objectTypeInfo.setObjectTypeVersion(OBJECT_TYPE_VERSION);
        objectTypeInfo.setObjectTypes(Collections.singletonList(UserSurvey.class));
        return objectTypeInfo;
    }
}
