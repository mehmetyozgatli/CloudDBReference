package com.myapps.clouddbreference.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.auth.AGConnectAuthCredential
import com.huawei.agconnect.auth.PhoneAuthProvider
import com.huawei.agconnect.cloud.database.CloudDBZoneQuery
import com.myapps.clouddbreference.R
import com.myapps.clouddbreference.adapter.RecyclerViewAdapter
import com.myapps.clouddbreference.cloudDB.CloudDBZoneWrapper
import com.myapps.clouddbreference.model.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class QueryFragment : Fragment(), CloudDBZoneWrapper.UiCallBack {

    private var mView: View? = null

    private var retrieveUserButton: Button? = null
    private var compoundButton: Button? = null
    private var averageButton: Button? = null
    private var orderAscButton: Button? = null
    private var orderDescButton: Button? = null
    private var limitedButton: Button? = null
    private var recyclerView: RecyclerView? = null

    private var fullName: String? = null
    private var emailOrPhone: String? = null
    private var verifyCode: String? = null
    private var password: String? = null
    private var pushToken: String? = null

    private val mHandler = MyHandler()
    private var mCloudDBZoneWrapper: CloudDBZoneWrapper? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mView = inflater.inflate(R.layout.fragment_query, container, false)
        recyclerView = mView?.findViewById(R.id.mRecyclerView)

        if (AGConnectAuth.getInstance().currentUser != null) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)

            CloudDBZoneWrapper.initAGConnectCloudDB(activity)
            mCloudDBZoneWrapper = CloudDBZoneWrapper()
            mHandler.post {
                mCloudDBZoneWrapper?.addCallBacks(this)
                mCloudDBZoneWrapper?.createObjectType()
                mCloudDBZoneWrapper?.openCloudDBZone()
                mCloudDBZoneWrapper?.addSubscription()
            }
            Toast.makeText(activity, "Success Login", Toast.LENGTH_SHORT).show()

        } else {
            val credential: AGConnectAuthCredential =
                PhoneAuthProvider.credentialWithPassword("+90", "5316771595", "123456my")
            signIn(credential)
        }

        retrieveUserButton = mView?.findViewById(R.id.retrieveUsersButton)
        retrieveUserButton?.setOnClickListener {
            mCloudDBZoneWrapper?.getAllUsers()
        }

        compoundButton = mView?.findViewById(R.id.compoundQueryButton)
        compoundButton?.setOnClickListener {
            val query: CloudDBZoneQuery<UserInfo> =
                CloudDBZoneQuery.where(UserInfo::class.java).greaterThan("verifyCode", "100000")
                    .lessThan("verifyCode", "300000")
            mCloudDBZoneWrapper?.queryUsers(query)
        }

        averageButton = mView?.findViewById(R.id.averageQueryButton)
        averageButton?.setOnClickListener {

            Toast.makeText(
                activity,
                "VerifyCode Average : ${mCloudDBZoneWrapper?.averageVerifyCode()}",
                Toast.LENGTH_SHORT
            ).show()

        }

        orderAscButton = mView?.findViewById(R.id.orderAscQueryButton)
        orderAscButton?.setOnClickListener {
            val query: CloudDBZoneQuery<UserInfo> =
                CloudDBZoneQuery.where(UserInfo::class.java).orderByAsc("verifyCode")
            mCloudDBZoneWrapper?.queryUsers(query)
        }

        orderDescButton = mView?.findViewById(R.id.orderDescQueryButton)
        orderDescButton?.setOnClickListener {
            val query: CloudDBZoneQuery<UserInfo> =
                CloudDBZoneQuery.where(UserInfo::class.java).orderByDesc("verifyCode")
            mCloudDBZoneWrapper?.queryUsers(query)
        }

        limitedButton = mView?.findViewById(R.id.LimitedQueryButton)
        limitedButton?.setOnClickListener {
            val query: CloudDBZoneQuery<UserInfo> =
                CloudDBZoneQuery.where(UserInfo::class.java).orderByDesc("verifyCode").limit(3)
            mCloudDBZoneWrapper?.queryUsers(query)
        }

        return mView
    }


    private fun signIn(credential: AGConnectAuthCredential) {
        AGConnectAuth.getInstance().signIn(credential).addOnSuccessListener {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)

            CloudDBZoneWrapper.initAGConnectCloudDB(activity)
            mCloudDBZoneWrapper =
                CloudDBZoneWrapper()
            mHandler.post {
                mCloudDBZoneWrapper?.addCallBacks(this)
                mCloudDBZoneWrapper?.createObjectType()
                mCloudDBZoneWrapper?.openCloudDBZone()
                mCloudDBZoneWrapper?.addSubscription()
            }

        }
            .addOnFailureListener { e ->
                Toast.makeText(activity, "createUser fail:${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun retrievePerson(userList: MutableList<UserInfo>) {
        val userInfoRecyclerViewAdapter = RecyclerViewAdapter(activity!!, userList)
        recyclerView!!.layoutManager = LinearLayoutManager(activity)
        recyclerView!!.adapter = userInfoRecyclerViewAdapter
    }

    private class MyHandler : Handler() {
        override fun handleMessage(msg: Message) {
            // dummy
        }
    }

    override fun onAddOrQueryUserList(userList: MutableList<UserInfo>?) {
        for (i in 0 until userList!!.size) {
            fullName = userList[i].fullName.toString()
            emailOrPhone = userList[i].emailOrPhone.toString()
            verifyCode = userList[i].verifyCode.toString()
            password = userList[i].password.toString()
            pushToken = userList[i].pushToken.toString()
            Log.i(
                "User List", "$fullName $emailOrPhone $verifyCode " +
                        "$password $pushToken"
            )
        }
        mHandler.post {
            retrievePerson(userList)
        }
    }

    override fun updateUiOnError(errorMessage: String?) {
        Log.w("UpdateUiOnError", "ERROR upsert: $errorMessage")
    }

    override fun isDataUpsert(state: Boolean?) {
        TODO("Not yet implemented")
    }

    override fun onSubscribeUserList(userList: MutableList<UserInfo>) {
        mHandler.post {
            if (userList.isNotEmpty() && activity != null)
            {
                val userInfoRecyclerViewAdapter = RecyclerViewAdapter(activity!!, userList)
                recyclerView!!.layoutManager = LinearLayoutManager(activity)
                recyclerView!!.adapter = userInfoRecyclerViewAdapter
                Log.w("onSubscribeUserList", "onSubscribeUserList")
            }
        }
    }

    override fun onDeleteUserList(userList: MutableList<UserInfo>?) {
        TODO("Not yet implemented")
    }
}