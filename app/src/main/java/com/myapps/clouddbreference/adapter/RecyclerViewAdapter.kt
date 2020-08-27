package com.myapps.clouddbreference.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.myapps.clouddbreference.R
import com.myapps.clouddbreference.model.UserSurvey

class RecyclerViewAdapter(context: Context, user: MutableList<UserSurvey>) :
    RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder>() {

    private var data: List<UserSurvey>? = null
    private var mContext: Context? = null

    init {
        data = user
        mContext = context
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.rv_list_item, parent, false)

        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data!!.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val user: UserSurvey = data!![position]
        holder.userID.text = "User ID: " + user.id.toString()
        holder.userName.text = "Full Name: " + user.fullName
        holder.userGender.text = "Gender: " + user.gender
        holder.userAge.text = "Age: " + user.age.toString()
        if (user.isMarried)
        {
            holder.userIsMarried.text = "Marriage: Yes"
        }else{
            holder.userIsMarried.text = "Marriage: No"
        }

        holder.userSalary.text = "Salary: " + user.pay

    }

    class MyViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        var userID: TextView = itemView.findViewById(R.id.user_id)
        var userName: TextView = itemView.findViewById(R.id.user_name)
        var userGender: TextView = itemView.findViewById(R.id.user_gender)
        var userAge: TextView = itemView.findViewById(R.id.user_age)
        var userIsMarried: TextView = itemView.findViewById(R.id.user_isMarried)
        var userSalary: TextView = itemView.findViewById(R.id.user_salary)
    }
}