package com.myapps.clouddbreference.fragment


import android.os.Bundle
import android.os.Handler
import android.os.Message
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
import com.myapps.clouddbreference.model.UserSurvey
import com.myapps.clouddbreference.adapter.RecyclerViewAdapter
import com.myapps.clouddbreference.cloudDB.CloudDBZoneWrapper


class QueryFragment : Fragment(), CloudDBZoneWrapper.UiCallBack {

    private var mView: View? = null

    private var retrieveUserButton: Button? = null
    private var compoundButton: Button? = null
    private var averageButton: Button? = null
    private var orderAscButton: Button? = null
    private var orderDescButton: Button? = null
    private var limitedButton: Button? = null
    private var recyclerView: RecyclerView? = null

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

            mCloudDBZoneWrapper?.queryAllUsers()

            /*
            val query: CloudDBZoneQuery<UserSurvey> =
                CloudDBZoneQuery.where(UserSurvey::class.java).equalTo("gender", "Female")
            mCloudDBZoneWrapper?.queryUsers(query)

             */
        }

        compoundButton = mView?.findViewById(R.id.compoundQueryButton)
        compoundButton?.setOnClickListener {
            val query: CloudDBZoneQuery<UserSurvey> =
                CloudDBZoneQuery.where(UserSurvey::class.java).greaterThan("pay", 1000)
                    .lessThan("pay", 8000)
            mCloudDBZoneWrapper?.queryUsers(query)
        }

        averageButton = mView?.findViewById(R.id.averageQueryButton)
        averageButton?.setOnClickListener {
            Toast.makeText(
                activity,
                "Age Average : ${mCloudDBZoneWrapper?.average()}",
                Toast.LENGTH_LONG
            ).show()
        }

        orderAscButton = mView?.findViewById(R.id.orderAscQueryButton)
        orderAscButton?.setOnClickListener {
            val query: CloudDBZoneQuery<UserSurvey> =
                CloudDBZoneQuery.where(UserSurvey::class.java).orderByAsc("age")
            mCloudDBZoneWrapper?.queryUsers(query)
        }
        orderDescButton = mView?.findViewById(R.id.orderDescQueryButton)
        orderDescButton?.setOnClickListener {
            val query: CloudDBZoneQuery<UserSurvey> =
                CloudDBZoneQuery.where(UserSurvey::class.java).orderByDesc("pay")
            mCloudDBZoneWrapper?.queryUsers(query)
        }

        limitedButton = mView?.findViewById(R.id.LimitedQueryButton)
        limitedButton?.setOnClickListener {
            val query: CloudDBZoneQuery<UserSurvey> =
                CloudDBZoneQuery.where(UserSurvey::class.java).orderByDesc("age").limit(4)
            mCloudDBZoneWrapper?.queryUsers(query)
        }

        return mView
    }


    private fun signIn(credential: AGConnectAuthCredential) {
        AGConnectAuth.getInstance().signIn(credential).addOnSuccessListener {

            CloudDBZoneWrapper.initAGConnectCloudDB(activity)
            mCloudDBZoneWrapper = CloudDBZoneWrapper()
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

    private class MyHandler : Handler() {
        override fun handleMessage(msg: Message) {
            // dummy
        }
    }

    override fun onAddOrQuery(userInfoList: List<UserSurvey>) {
        mHandler.post {
            val userInfoRecyclerViewAdapter = RecyclerViewAdapter(activity!!,
                userInfoList as MutableList<UserSurvey>
            )
            recyclerView!!.layoutManager = LinearLayoutManager(activity)
            recyclerView!!.adapter = userInfoRecyclerViewAdapter
        }
    }

    override fun onSubscribe(userInfoList: List<UserSurvey>?) {
        mHandler.post {
            if (activity != null){
                val userInfoRecyclerViewAdapter = RecyclerViewAdapter(activity!!,
                    userInfoList as MutableList<UserSurvey>
                )
                recyclerView!!.layoutManager = LinearLayoutManager(activity)
                recyclerView!!.adapter = userInfoRecyclerViewAdapter
                Log.w("onSubscribeUserList", "onSubscribeUserList")
            }
        }
    }

    override fun onDelete(userInfoList: List<UserSurvey>?) {
        TODO("Not yet implemented")
    }

    override fun updateUiOnError(errorMessage: String?) {
        Log.w("UpdateUiOnError", "ERROR : $errorMessage")
    }
}