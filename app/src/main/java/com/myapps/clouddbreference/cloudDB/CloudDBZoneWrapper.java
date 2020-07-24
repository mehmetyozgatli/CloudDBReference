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
import com.myapps.clouddbreference.model.UserInfo;

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

    /**
     * Call AGConnectCloudDB.openCloudDBZone to open a cloudDBZone.
     * We set it with cloud cache mode, and data can be store in local storage
     */
    public void openCloudDBZone() {
        mConfig = new CloudDBZoneConfig("CarCodeScanner",
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

    /**
     * Call AGConnectCloudDB.closeCloudDBZone
     */
    public void closeCloudDBZone() {
        try {
            mRegister.remove();
            mCloudDB.closeCloudDBZone(mCloudDBZone);
            Log.w(TAG, "closeCloudDBZoneSuccess ");
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "closeCloudDBZoneError: " + e.getMessage());
        }
    }


    /**
     * Call AGConnectCloudDB.deleteCloudDBZone
     */
    public void deleteCloudDBZone() {
        try {
            mCloudDB.deleteCloudDBZone(mConfig.getCloudDBZoneName());
            Log.w(TAG, "deleteCloudBZoneSuccess");
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "deleteCloudDBZone: " + e.getMessage());
        }
    }

    private void updateUserIndex(UserInfo userInfo) {
        try {
            mReadWriteLock.writeLock().lock();
            if (mUserIndex < userInfo.getId()) {
                mUserIndex = userInfo.getId();
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    private OnSnapshotListener<UserInfo> mSnapshotListener = new OnSnapshotListener<UserInfo>() {
        @Override
        public void onSnapshot(CloudDBZoneSnapshot<UserInfo> cloudDBZoneSnapshot, AGConnectCloudDBException e) {
            if (e != null) {
                Log.w(TAG, "onSnapshot: " + e.getMessage());
                return;
            }
            CloudDBZoneObjectList<UserInfo> snapshotObjects = cloudDBZoneSnapshot.getSnapshotObjects();
            List<UserInfo> userInfoList = new ArrayList<>();
            try {
                if (snapshotObjects != null) {
                    while (snapshotObjects.hasNext()) {
                        UserInfo userInfo = snapshotObjects.next();
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
            CloudDBZoneQuery<UserInfo> snapshotQuery = CloudDBZoneQuery.where(UserInfo.class);
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
        CloudDBZoneTask<CloudDBZoneSnapshot<UserInfo>> queryTask = mCloudDBZone.executeQuery(
                CloudDBZoneQuery.where(UserInfo.class),
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);
        queryTask.addOnSuccessListener(new OnSuccessListener<CloudDBZoneSnapshot<UserInfo>>() {
            @Override
            public void onSuccess(CloudDBZoneSnapshot<UserInfo> snapshot) {
                userListResult(snapshot);
                Log.w(TAG, "GET USER DETAIL : GoResults: ");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (mUiCallBack != null) {
                    mUiCallBack.updateUiOnError("GET USER DETAIL : Query user list from cloud failed");
                }
                Log.e("onFailure", "onFailure: " + e);
            }
        });
    }

    public void queryUsers(CloudDBZoneQuery<UserInfo> query) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it");
            return;
        }

        CloudDBZoneTask<CloudDBZoneSnapshot<UserInfo>> queryTask = mCloudDBZone.executeQuery(query,
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);
        queryTask.await();
        if (!queryTask.isSuccessful()) {
            mUiCallBack.updateUiOnError("Query failed");
            return;
        }
        userListResult(queryTask.getResult());
    }

    private void userListResult (CloudDBZoneSnapshot<UserInfo> snapshot) {
        CloudDBZoneObjectList<UserInfo> userInfoCursor = snapshot.getSnapshotObjects();
        List<UserInfo> userInfoList = new ArrayList<>();
        try {
            while (userInfoCursor.hasNext()) {
                UserInfo userInfo = userInfoCursor.next();
                userInfoList.add(userInfo);
                Log.w(TAG, "USER DETAIL RESULT : processQueryResult: ");

            }
        } catch (AGConnectCloudDBException e) {
            Log.w(TAG, "USER DETAIL RESULT : processQueryResult: " + e.getMessage());
        }
        snapshot.release();
        if (mUiCallBack != null) {
            mUiCallBack.onAddOrQueryUserList(userInfoList);
        }
    }

    public double averageVerifyCode(){
        if (mCloudDBZone == null) {
            Log.w(TAG, "INSERT USER : CloudDBZone is null, try re-open it");
            return 0;
        }
        CloudDBZoneQuery<UserInfo> query;
        query = CloudDBZoneQuery.where(UserInfo.class);
        CloudDBZoneTask<Double> averageQueryTask = mCloudDBZone.executeAverageQuery(query, "id",
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);
        averageQueryTask.await();
        if (averageQueryTask.getException() != null) {
            Log.w(TAG, "Average query is failed: " + Log.getStackTraceString(averageQueryTask.getException()));
            return 1;
        }
        Log.w(TAG, "Average price is " + averageQueryTask.getResult());
        return averageQueryTask.getResult();
    }

    public void insertUser(UserInfo user) {

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

    public void deleteUserInfo(List<UserInfo> userList) {
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


    /**
     * Call back to update ui in MainActivity
     */
    public interface UiCallBack {
        UiCallBack DEFAULT = new UiCallBack() {

            @Override
            public void onAddOrQueryUserList(List<UserInfo> userList) {
                Log.w(TAG, "Using default onAddOrQuery");
            }

            @Override
            public void onSubscribeUserList(List<UserInfo> userList) {
                Log.w(TAG, "Using default onSubscribe");
            }

            @Override
            public void onDeleteUserList(List<UserInfo> userList) {
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

        void onAddOrQueryUserList(List<UserInfo> userList);
        void isDataUpsert(Boolean state); // Veri eklerken
        void onSubscribeUserList(List<UserInfo> userList);
        void onDeleteUserList(List<UserInfo> userList);
        void updateUiOnError(String errorMessage);

    }

    /**
     * Add a callback to update book info list
     *
     * @param uiCallBack callback to update book list
     */
    public void addCallBacks(UiCallBack uiCallBack) {
        mUiCallBack = uiCallBack;
    }

}
