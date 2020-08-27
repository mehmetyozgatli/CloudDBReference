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
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.huawei.agconnect.cloud.database.CloudDBZoneQuery
import com.myapps.clouddbreference.R
import com.myapps.clouddbreference.model.UserSurvey
import com.myapps.clouddbreference.cloudDB.CloudDBZoneWrapper


class InsertOrDeleteFragment : Fragment(), CloudDBZoneWrapper.UiCallBack {

    private var mView: View? = null

    private var userId: Int? = null
    private var fullName: String? = null
    private var gender: String? = null
    private var age: Int? = null
    private var isMarried: Boolean? = null
    private var pay: Int? = null

    private val mHandler = MyHandler()
    private var mCloudDBZoneWrapper: CloudDBZoneWrapper? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mView = inflater.inflate(R.layout.fragment_insert_or_delete, container, false)

        val mUserId : TextView? = mView?.findViewById(R.id.userId)
        val mFullName: TextView? = mView?.findViewById(R.id.fullName)
        val maleRadioButton: RadioButton? = mView?.findViewById(R.id.radioMale)
        val femaleRadioButton: RadioButton? = mView?.findViewById(R.id.radioFemale)
        val mAge: TextView? = mView?.findViewById(R.id.age)
        val marriedYesRadioButton: RadioButton? = mView?.findViewById(R.id.radioYes)
        val marriedNoRadioButton: RadioButton? = mView?.findViewById(R.id.radioNo)
        val mPay: TextView? = mView?.findViewById(R.id.salary)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        CloudDBZoneWrapper.initAGConnectCloudDB(activity)
        mCloudDBZoneWrapper = CloudDBZoneWrapper()
        mHandler.post {
            mCloudDBZoneWrapper?.addCallBacks(this)
            mCloudDBZoneWrapper?.createObjectType()
            mCloudDBZoneWrapper?.openCloudDBZone()
        }

        val upsertButton: Button? = mView?.findViewById(R.id.upsertBtn)
        upsertButton?.setOnClickListener {
            try {
                userId = mUserId?.text.toString().toInt()

                fullName = mFullName?.text.toString().trim()

                if (maleRadioButton!!.isChecked) {
                    gender = "Male"
                } else if (femaleRadioButton!!.isChecked) {
                    gender = "Female"
                }

                age = mAge?.text.toString().toInt()

                if (marriedYesRadioButton!!.isChecked) {
                    isMarried = true
                } else if (marriedNoRadioButton!!.isChecked) {
                    isMarried = false
                }

                pay = mPay?.text.toString().toInt()

                val user = UserSurvey()
                user.id = userId
                user.fullName = fullName
                user.gender = gender
                user.age = age
                user.isMarried = isMarried
                user.pay = pay

                mHandler.post { mCloudDBZoneWrapper?.insertUser(user) }

                Toast.makeText(activity, "Success", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(activity, "error: " + e.message, Toast.LENGTH_LONG).show()
                Log.i("Error", "Error:$e")
            }
        }

        val deleteButton: Button? = mView?.findViewById(R.id.deleteBtn)
        deleteButton?.setOnClickListener {

            try {

                userId = mUserId?.text.toString().toInt()

                val query: CloudDBZoneQuery<UserSurvey> = CloudDBZoneQuery.where(UserSurvey::class.java)
                    .equalTo("id", userId!!)
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

    override fun onAddOrQueryUserList(userList: MutableList<UserSurvey>?) {
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

    override fun onSubscribeUserList(userList: MutableList<UserSurvey>) {
        Log.w("onSubscribeUserList", "onSubscribeUserList $userId")
    }

    override fun onDeleteUserList(userList: MutableList<UserSurvey>?) {
        Log.w("DeleteUser", "Deleted User ID:")
    }
}