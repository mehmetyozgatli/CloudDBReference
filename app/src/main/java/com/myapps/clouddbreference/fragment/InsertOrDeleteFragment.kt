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
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.huawei.agconnect.cloud.database.CloudDBZoneQuery
import com.myapps.clouddbreference.R
import com.myapps.clouddbreference.cloudDB.CloudDBZoneWrapper
import com.myapps.clouddbreference.model.UserInfo


class InsertOrDeleteFragment : Fragment(), CloudDBZoneWrapper.UiCallBack {

    private var mView: View? = null

    private var userID: String? = null
    private var fullName: String? = null
    private var emailOrPhone: String? = null
    private var verifyCode: String? = null
    private var password: String? = null

    private var mUserID: TextView? = null
    private var mFullName: TextView? = null
    private var mEmail: TextView? = null
    private var mPassword: TextView? = null
    private var mVerifyCode: TextView? = null

    private var registerButton: Button? = null
    private var deleteButton: Button? = null

    private val mHandler = MyHandler()
    private var mCloudDBZoneWrapper: CloudDBZoneWrapper? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mView = inflater.inflate(R.layout.fragment_insert_or_delete, container, false)

        mUserID = mView?.findViewById(R.id.userIdTxt)
        mFullName = mView?.findViewById(R.id.fullNameTxt)
        mEmail = mView?.findViewById(R.id.emailTxt)
        mPassword = mView?.findViewById(R.id.passwordTxt)
        mVerifyCode = mView?.findViewById(R.id.verifyCode)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        CloudDBZoneWrapper.initAGConnectCloudDB(activity)
        mCloudDBZoneWrapper =
            CloudDBZoneWrapper()
        mHandler.post {
            mCloudDBZoneWrapper?.addCallBacks(this)
            mCloudDBZoneWrapper?.createObjectType()
            mCloudDBZoneWrapper?.openCloudDBZone()
        }

        registerButton = mView?.findViewById(R.id.registerBtn)
        registerButton?.setOnClickListener {
            try {
                userID = mUserID?.text.toString().trim()
                fullName = mFullName?.text.toString().trim()
                emailOrPhone = mEmail?.text.toString().trim()
                verifyCode = mVerifyCode?.text.toString().trim()
                password = mPassword?.text.toString().trim()

                val id = userID!!.toInt()

                val user = UserInfo()
                user.id = id
                user.fullName = fullName
                user.emailOrPhone = emailOrPhone
                user.verifyCode = verifyCode
                user.password = password
                user.pushToken = "test"

                Log.d("Test", "${userID!!.toInt()} $fullName $emailOrPhone $verifyCode $password")

                mHandler.post { mCloudDBZoneWrapper?.insertUser(user) }

                Toast.makeText(activity, "Success", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(activity, "error: " + e.message, Toast.LENGTH_LONG).show()
                Log.i("Error", "Error:$e")
            }
        }

        deleteButton = mView?.findViewById(R.id.deleteBtn)
        deleteButton?.setOnClickListener {

            try {
                userID = mUserID?.text.toString().trim()
                val id = userID!!.toInt()

                val query: CloudDBZoneQuery<UserInfo> = CloudDBZoneQuery.where(UserInfo::class.java)
                    .equalTo("id", id)
                mCloudDBZoneWrapper?.queryUsers(query)
                Toast.makeText(activity, "Deleted", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(activity, "error: " + e.message, Toast.LENGTH_LONG).show()
                Log.i("Error", "Error:$e")
            }
        }

        return mView
    }

    private class MyHandler : Handler() {
        override fun handleMessage(msg: Message) {
            // dummy
        }
    }

    override fun onAddOrQueryUserList(userList: MutableList<UserInfo>?) {
        mHandler.post { mCloudDBZoneWrapper?.deleteUserInfo(userList) }
    }

    override fun updateUiOnError(errorMessage: String?) {
        Log.w("UpdateUiOnError", "ERROR: $errorMessage")
    }

    override fun isDataUpsert(state: Boolean?) {
        if (state!!) {
            Log.w("DataUpsert", "INSERT USER : upsert")
        } else {
            Log.w("DataUpsert", "ERROR upsert")
        }
    }

    override fun onSubscribeUserList(userList: MutableList<UserInfo>?) {
        Log.w("onSubscribeUserList", "onSubscribeUserList")
    }

    override fun onDeleteUserList(userList: MutableList<UserInfo>?) {
        Log.w("DeleteUser", "Deleted User ID:")
    }
}