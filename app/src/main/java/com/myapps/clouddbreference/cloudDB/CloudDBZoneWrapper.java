package com.myapps.clouddbreference.cloudDB;

import android.content.Context;
import android.util.Log;

import com.huawei.agconnect.cloud.database.AGConnectCloudDB;
import com.huawei.agconnect.cloud.database.CloudDBZone;
import com.huawei.agconnect.cloud.database.CloudDBZoneConfig;
import com.huawei.agconnect.cloud.database.CloudDBZoneObjectList;
import com.huawei.agconnect.cloud.database.CloudDBZoneQuery;
import com.huawei.agconnect.cloud.database.CloudDBZoneSnapshot;
import com.huawei.agconnect.cloud.database.CloudDBZoneTask;
import com.huawei.agconnect.cloud.database.ListenerHandler;
import com.huawei.agconnect.cloud.database.OnFailureListener;
import com.huawei.agconnect.cloud.database.OnSnapshotListener;
import com.huawei.agconnect.cloud.database.OnSuccessListener;
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException;
import com.myapps.clouddbreference.model.UserSurvey;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CloudDBZoneWrapper {
    private AGConnectCloudDB mCloudDB;
    private CloudDBZone mCloudDBZone;
    private ListenerHandler mRegister;
    private CloudDBZoneConfig mConfig;
    private UiCallBack mUiCallBack = UiCallBack.DEFAULT;
    private static final String TAG = "DB_Zone_Wrapper";
    boolean state = false;

    private int mUserIndex = 0;

    private ReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();

    public CloudDBZoneWrapper() {
        mCloudDB = AGConnectCloudDB.getInstance();
    }

    public static void initAGConnectCloudDB(Context context) {
        AGConnectCloudDB.initialize(context);
        Log.w(TAG, "initAGConnectCloudDB");
    }

    public void createObjectType() {
        try {
            mCloudDB.createObjectType(ObjectTypeInfoHelper.getObjectTypeInfo());
            Log.w(TAG, "createObjectTypeSuccess ");
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "createObjectTypeError: " + e.getMessage());
        }
    }

    public void openCloudDBZone() {
        mConfig = new CloudDBZoneConfig("CloudReference",
                CloudDBZoneConfig.CloudDBZoneSyncProperty.CLOUDDBZONE_CLOUD_CACHE,
                CloudDBZoneConfig.CloudDBZoneAccessProperty.CLOUDDBZONE_PUBLIC);
        mConfig.setPersistenceEnabled(true);
        Log.w(TAG, "openCloudDBZoneSuccess ");
        try {
            mCloudDBZone = mCloudDB.openCloudDBZone(mConfig, true);
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "openCloudDBZoneError: " + e.getMessage());
        }
    }

    public void closeCloudDBZone() {
        try {
            mRegister.remove();
            mCloudDB.closeCloudDBZone(mCloudDBZone);
            Log.w(TAG, "closeCloudDBZoneSuccess ");
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "closeCloudDBZoneError: " + e.getMessage());
        }
    }

    public void deleteCloudDBZone() {
        try {
            mCloudDB.deleteCloudDBZone(mConfig.getCloudDBZoneName());
            Log.w(TAG, "deleteCloudBZoneSuccess");
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "deleteCloudDBZone: " + e.getMessage());
        }
    }

    private void updateUserIndex(UserSurvey userInfo) {
        try {
            mReadWriteLock.writeLock().lock();
            if (mUserIndex < userInfo.getId()) {
                mUserIndex = userInfo.getId();
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    private OnSnapshotListener<UserSurvey> mSnapshotListener = new OnSnapshotListener<UserSurvey>() {
        @Override
        public void onSnapshot(CloudDBZoneSnapshot<UserSurvey> cloudDBZoneSnapshot, AGConnectCloudDBException e) {
            if (e != null) {
                Log.w(TAG, "onSnapshot: " + e.getMessage());
                return;
            }
            CloudDBZoneObjectList<UserSurvey> snapshotObjects = cloudDBZoneSnapshot.getSnapshotObjects();
            List<UserSurvey> userInfoList = new ArrayList<>();
            try {
                if (snapshotObjects != null) {
                    while (snapshotObjects.hasNext()) {
                        UserSurvey userInfo = snapshotObjects.next();
                        userInfoList.add(userInfo);
                        updateUserIndex(userInfo);
                    }
                }
                if (mUiCallBack != null) {
                    mUiCallBack.onSubscribeUserList(userInfoList);
                }
            } catch (AGConnectCloudDBException snapshotException) {
                Log.w(TAG, "onSnapshot:(getObject) " + snapshotException.getMessage());
            } finally {
                cloudDBZoneSnapshot.release();
            }
        }
    };

    public void addSubscription() {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        try {
            CloudDBZoneQuery<UserSurvey> snapshotQuery = CloudDBZoneQuery.where(UserSurvey.class);
            mRegister = mCloudDBZone.subscribeSnapshot(snapshotQuery,
                    CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY, mSnapshotListener);
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "subscribeSnapshot: " + e.getMessage());
        }
    }

    public void getAllUsers() {
        if (mCloudDBZone == null) {
            Log.w(TAG, "GET USER DETAIL : CloudDBZone is null, try re-open it");
            return;
        }
        CloudDBZoneTask<CloudDBZoneSnapshot<UserSurvey>> queryTask = mCloudDBZone.executeQuery(
                CloudDBZoneQuery.where(UserSurvey.class),
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);
        queryTask.addOnSuccessListener(new OnSuccessListener<CloudDBZoneSnapshot<UserSurvey>>() {
            @Override
            public void onSuccess(CloudDBZoneSnapshot<UserSurvey> snapshot) {
                userListResult(snapshot);
                Log.w(TAG, "GET USER DETAIL : GoResults: ");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (mUiCallBack != null) {
                    mUiCallBack.updateUiOnError("GET USER DETAIL : " +
                            "Query user list from cloud failed");
                }
                Log.e("onFailure", "onFailure: " + e);
            }
        });
    }

    public void queryUsers(CloudDBZoneQuery<UserSurvey> query) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        CloudDBZoneTask<CloudDBZoneSnapshot<UserSurvey>> queryTask =
                mCloudDBZone.executeQuery(query,
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);
        queryTask.await();
        if (!queryTask.isSuccessful()) {
            mUiCallBack.updateUiOnError("Query failed");
            return;
        }
        userListResult(queryTask.getResult());
    }

    private void userListResult (CloudDBZoneSnapshot<UserSurvey> snapshot) {
        CloudDBZoneObjectList<UserSurvey> userInfoCursor = snapshot.getSnapshotObjects();
        List<UserSurvey> userInfoList = new ArrayList<>();
        try {
            while (userInfoCursor.hasNext()) {
                UserSurvey userInfo = userInfoCursor.next();
                userInfoList.add(userInfo);
                Log.w(TAG, "USER DETAIL RESULT : processQueryResult: ");

            }
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "USER DETAIL RESULT : " +
                    "processQueryResult: " + e.getMessage());
        }
        snapshot.release();
        if (mUiCallBack != null) {
            mUiCallBack.onAddOrQueryUserList(userInfoList);
        }
    }

    public double average(){
        if (mCloudDBZone == null) {
            Log.w(TAG, "INSERT USER : CloudDBZone is null, try re-open it");
            return 0;
        }
        CloudDBZoneQuery<UserSurvey> query;
        query = CloudDBZoneQuery.where(UserSurvey.class);
        CloudDBZoneTask<Double> averageQueryTask = mCloudDBZone.executeAverageQuery(query, "age",
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);
        averageQueryTask.await();
        if (averageQueryTask.getException() != null) {
            Log.w(TAG, "Average query is failed: " + Log.getStackTraceString(averageQueryTask.getException()));
            return 1;
        }
        Log.w(TAG, "Average price is " + averageQueryTask.getResult());
        return averageQueryTask.getResult();
    }

    public void insertUser(UserSurvey user) {

        if (mCloudDBZone == null) {
            Log.w(TAG, "INSERT USER : CloudDBZone is null, try re-open it");
            return;
        }
        CloudDBZoneTask<Integer> upsertTask = mCloudDBZone.executeUpsert(user);
        if (mUiCallBack == null) {
            return;
        }
        upsertTask.addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer cloudDBZoneResult) {
                state = true;
                Log.w(TAG, "INSERT USER : upsert " + cloudDBZoneResult + " records");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                state = false;
                mUiCallBack.updateUiOnError("INSERT USER : Insert user info failed");
            }
        });
        if (mUiCallBack != null) {
            mUiCallBack.isDataUpsert(state);
        }
    }

    public void deleteUserInfo(List<UserSurvey> userList) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }
        CloudDBZoneTask<Integer> deleteTask = mCloudDBZone.executeDelete(userList);
        deleteTask.addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                if (mUiCallBack != null) {
                    Log.w(TAG, "DELETE USER : delete " + integer + " records");
                    mUiCallBack.onDeleteUserList(userList);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (mUiCallBack != null) {
                    mUiCallBack.updateUiOnError("Delete user info failed " + e.getMessage() );
                }
            }
        });
    }

    public interface UiCallBack {
        UiCallBack DEFAULT = new UiCallBack() {
            @Override
            public void onAddOrQueryUserList(List<UserSurvey> userList) {
                Log.w(TAG, "Using default onAddOrQuery");
            }
            @Override
            public void onSubscribeUserList(List<UserSurvey> userList) {
                Log.w(TAG, "Using default onSubscribe");
            }
            @Override
            public void onDeleteUserList(List<UserSurvey> userList) {
                Log.w(TAG, "Using default onDelete");
            }
            @Override
            public void updateUiOnError(String errorMessage) {
                Log.w(TAG, "Using default updateUiOnError");
            }
            @Override
            public void isDataUpsert(Boolean state) {
                Log.w(TAG, "Using default isDataUpsert");
            }
        };
        void onAddOrQueryUserList(List<UserSurvey> userList);
        void isDataUpsert(Boolean state);
        void onSubscribeUserList(List<UserSurvey> userList);
        void onDeleteUserList(List<UserSurvey> userList);
        void updateUiOnError(String errorMessage);
    }
    public void addCallBacks(UiCallBack uiCallBack) {
        mUiCallBack = uiCallBack;
    }

}
