package com.myapps.clouddbreference.cloudDB

import android.content.Context
import android.util.Log
import com.huawei.agconnect.cloud.database.*
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException
import com.myapps.clouddbreference.model.UserSurvey
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock


/**
 * Proxying implementation of CloudDBZone.
 */

class CloudDBZoneWrapper {
    private val mCloudDB: AGConnectCloudDB = AGConnectCloudDB.getInstance()
    private var mCloudDBZone: CloudDBZone? = null
    private var mRegister: ListenerHandler? = null
    private var mConfig: CloudDBZoneConfig? = null
    private var mUiCallBack = UiCallBack.DEFAULT


    /**
     * Mark max id of user info. id is the primary key of [UserSurvey], so we must provide an value for it
     * when upserting to database.
     */
    private var mUserIndex = 0
    private val mReadWriteLock: ReadWriteLock = ReentrantReadWriteLock()

    /**
     * Monitor data change from database. Update user info list if data have changed
     */
    private val mSnapshotListener = OnSnapshotListener<UserSurvey> { cloudDBZoneSnapshot, e ->
        if (e != null) {
            Log.w(TAG, "onSnapshot: " + e.message)
            return@OnSnapshotListener
        }
        val snapshotObjects = cloudDBZoneSnapshot.snapshotObjects
        val userInfoList: MutableList<UserSurvey> = ArrayList()
        try {
            if (snapshotObjects != null) {
                while (snapshotObjects.hasNext()) {
                    val userInfo = snapshotObjects.next()
                    userInfoList.add(userInfo)
                    updateUserIndex(userInfo)
                }
            }
            mUiCallBack.onSubscribe(userInfoList)
        } catch (snapshotException: AGConnectCloudDBException) {
            Log.w(TAG, "onSnapshot:(getObject) " + snapshotException.message)
        } finally {
            cloudDBZoneSnapshot.release()
        }
    }

    /**
     * Call AGConnectCloudDB.createObjectType to init schema
     */
    fun createObjectType() {
        try {
            mCloudDB.createObjectType(ObjectTypeInfoHelper.getObjectTypeInfo())
        } catch (e: AGConnectCloudDBException) {
            Log.w(TAG, "createObjectType: " + e.message)
        }
    }

    /**
     * Call AGConnectCloudDB.openCloudDBZone to open a cloudDBZone.
     * We set it with cloud cache mode, and data can be store in local storage
     */
    fun openCloudDBZone() {
        mConfig = CloudDBZoneConfig(
            "CloudReference",
            CloudDBZoneConfig.CloudDBZoneSyncProperty.CLOUDDBZONE_CLOUD_CACHE,
            CloudDBZoneConfig.CloudDBZoneAccessProperty.CLOUDDBZONE_PUBLIC
        )
        mConfig!!.persistenceEnabled = true
        try {
            mCloudDBZone = mCloudDB.openCloudDBZone(mConfig!!, true)
        } catch (e: AGConnectCloudDBException) {
            Log.w(TAG, "openCloudDBZone: " + e.message)
        }
    }

    /**
     * Call AGConnectCloudDB.closeCloudDBZone
     */
    fun closeCloudDBZone() {
        try {
            mRegister!!.remove()
            mCloudDB.closeCloudDBZone(mCloudDBZone)
        } catch (e: AGConnectCloudDBException) {
            Log.w(TAG, "closeCloudDBZone: " + e.message)
        }
    }

    /**
     * Call AGConnectCloudDB.deleteCloudDBZone
     */
    fun deleteCloudDBZone() {
        try {
            mCloudDB.deleteCloudDBZone(mConfig!!.cloudDBZoneName)
        } catch (e: AGConnectCloudDBException) {
            Log.w(TAG, "deleteCloudDBZone: " + e.message)
        }
    }

    /**
     * Add a callback to update user info list
     *
     * @param uiCallBack callback to update user list
     */
    fun addCallBacks(uiCallBack: UiCallBack) {
        mUiCallBack = uiCallBack
    }

    /**
     * Add mSnapshotListener to monitor data changes from storage
     */
    fun addSubscription() {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it")
            return
        }
        try {
            val snapshotQuery = CloudDBZoneQuery.where(UserSurvey::class.java)
            mRegister = mCloudDBZone!!.subscribeSnapshot(
                snapshotQuery,
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY,
                mSnapshotListener
            )
        } catch (e: AGConnectCloudDBException) {
            Log.w(TAG, "subscribeSnapshot: " + e.message)
        }
    }

    /**
     * Query all users in storage from cloud side with CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
     */
    fun queryAllUsers() {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it")
            return
        }
        val queryTask = mCloudDBZone!!.executeQuery(
            CloudDBZoneQuery.where(UserSurvey::class.java),
            CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
        )

        queryTask.addOnSuccessListener { snapshot -> processQueryResult(snapshot)

        }.addOnFailureListener {
            mUiCallBack.updateUiOnError("Query user list from cloud failed")
        }
    }

    /**
     * Query books with condition
     *
     * @param query query condition
     */
    fun queryUsers(query: CloudDBZoneQuery<UserSurvey>) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it")
            return
        }

        val queryTask = mCloudDBZone!!.executeQuery(
            query,
            CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
        )
        queryTask.addOnSuccessListener { snapshot -> processQueryResult(snapshot)

        }.addOnFailureListener {
            mUiCallBack.updateUiOnError("Query failed")
        }
    }

    private fun processQueryResult(snapshot: CloudDBZoneSnapshot<UserSurvey>) {
        val userInfoCursor = snapshot.snapshotObjects
        val userInfoList: MutableList<UserSurvey> = ArrayList()
        try {
            while (userInfoCursor.hasNext()) {
                val userInfo = userInfoCursor.next()
                userInfoList.add(userInfo)
            }
        } catch (e: AGConnectCloudDBException) {
            Log.w(TAG, "processQueryResult: " + e.message)
        } finally {
            snapshot.release()
        }
        mUiCallBack.onAddOrQuery(userInfoList)
    }


    /**
     * Average user age

     */

    fun average(): Double {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it")
            return 0.0
        }
        val query: CloudDBZoneQuery<UserSurvey> = CloudDBZoneQuery.where(UserSurvey::class.java)
        val averageQueryTask = mCloudDBZone!!.executeAverageQuery(
            query, "age",
            CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY
        )
        averageQueryTask.await()
        if (averageQueryTask.exception != null) {
            Log.w(
                TAG,
                "Average query is failed: " + Log.getStackTraceString(averageQueryTask.exception)
            )
            return 1.0
        }
        Log.w(TAG, "Average price is " + averageQueryTask.result)
        return averageQueryTask.result
    }

    /**
     * Upsert userinfo
     *
     * @param userInfo userinfo added or modified from local
     */
    fun upsertUserInfos(userInfo: UserSurvey?) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it")
            return
        }
        val upsertTask = mCloudDBZone!!.executeUpsert(userInfo!!)
        upsertTask.addOnSuccessListener { cloudDBZoneResult -> Log.w(
            TAG,
            "upsert $cloudDBZoneResult records"
        )

        }.addOnFailureListener {
            mUiCallBack.updateUiOnError("Insert user info failed")
        }
    }

    /**
     * Delete userinfo
     *
     * @param userInfoList users selected by user
     */
    fun deleteUserInfos(userInfoList: List<UserSurvey>?) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "CloudDBZone is null, try re-open it")
            return
        }
        val deleteTask = mCloudDBZone!!.executeDelete(userInfoList!!)
        if (deleteTask.exception != null) {
            mUiCallBack.updateUiOnError("Delete user info failed")
            return
        }
        mUiCallBack.onDelete(userInfoList)
    }

    private fun updateUserIndex(bookInfo: UserSurvey) {
        try {
            mReadWriteLock.writeLock().lock()
            if (mUserIndex < bookInfo.id) {
                mUserIndex = bookInfo.id
            }
        } finally {
            mReadWriteLock.writeLock().unlock()
        }
    }

    /**
     * Get max id of userinfos
     *
     * @return max user info id
     */
    val userIndex: Int
        get() = try {
            mReadWriteLock.readLock().lock()
            mUserIndex
        } finally {
            mReadWriteLock.readLock().unlock()
        }

    /**
     * Call back to update ui in Fragment
     */
    interface UiCallBack {
        fun onAddOrQuery(userInfoList: List<UserSurvey>)
        fun onSubscribe(userInfoList: List<UserSurvey>?)
        fun onDelete(userInfoList: List<UserSurvey>?)
        fun updateUiOnError(errorMessage: String?)

        companion object {
            val DEFAULT: UiCallBack = object : UiCallBack {
                override fun onAddOrQuery(userInfoList: List<UserSurvey>) {
                    Log.w(TAG, "Using default onAddOrQuery")
                }

                override fun onSubscribe(userInfoList: List<UserSurvey>?) {
                    Log.w(TAG, "Using default onSubscribe")
                }

                override fun onDelete(userInfoList: List<UserSurvey>?) {
                    Log.w(TAG, "Using default onDelete")
                }

                override fun updateUiOnError(errorMessage: String?) {
                    Log.w(TAG, "Using default updateUiOnError")
                }
            }
        }
    }

    companion object {
        private const val TAG = "CloudDBZoneWrapper"

        /**
         * Init AGConnectCloudDB in Application
         *
         * @param context application context
         */
        fun initAGConnectCloudDB(context: Context?) {
            AGConnectCloudDB.initialize(context!!)
        }
    }
}