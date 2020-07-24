package com.myapps.clouddbreference.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.myapps.clouddbreference.R
import com.myapps.clouddbreference.model.UserInfo

class RecyclerViewAdapter(context: Context, user: MutableList<UserInfo>) :
    RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder>() {

    private var data: List<UserInfo>? = null
    var mContext: Context? = null

    init {
        data = user
        mContext = context
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerViewAdapter.MyViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.rv_list_item, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data!!.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val user: UserInfo = data!![position]
        holder.userID.text = "ID: " + user.id.toString()
        holder.userEmail.text = "Username: " + user.emailOrPhone
        holder.userVerifyCode.text = "VerifyCode: " + user.verifyCode
    }

    class MyViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        var userID: TextView = itemView.findViewById(R.id.user_id)
        var userEmail: TextView = itemView.findViewById(R.id.user_email)
        var userVerifyCode: TextView = itemView.findViewById(R.id.user_verifyCode)

    }
}