package com.myapps.clouddbreference

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.navigation.NavigationView
import com.myapps.clouddbreference.cloudDB.CloudDBZoneWrapper
import com.myapps.clouddbreference.fragment.InsertOrDeleteFragment
import com.myapps.clouddbreference.fragment.QueryFragment

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var drawerLayout: DrawerLayout? = null
    private var toggle: ActionBarDrawerToggle? = null
    private var navigationView: NavigationView? = null

    private var fragmentManager: FragmentManager? = null
    private var fragmentTransaction: FragmentTransaction? = null

    private val mHandler = MyHandler()
    private var mCloudDBZoneWrapper: CloudDBZoneWrapper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer)
        navigationView = findViewById(R.id.nav_view)
        navigationView?.setNavigationItemSelectedListener(this)

        navigationView?.bringToFront()
        navigationView?.setCheckedItem(R.id.menu_query)

        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close)

        drawerLayout?.addDrawerListener(toggle!!)
        toggle?.isDrawerIndicatorEnabled
        toggle?.syncState()

        //load default fragment
        if (fragmentTransaction == null) {
            fragmentManager = supportFragmentManager
            fragmentTransaction = fragmentManager?.beginTransaction()
            fragmentTransaction?.add(R.id.fragment_container, QueryFragment())
            fragmentTransaction?.commit()
        }

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout?.closeDrawer(GravityCompat.START)

        when (item.itemId) {
            R.id.menu_query -> supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, QueryFragment()).commit()

            R.id.menu_insertOrDelete -> supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, InsertOrDeleteFragment()).commit()

        }

        return true
    }

    override fun onBackPressed() {
        if (drawerLayout?.isDrawerOpen(GravityCompat.START)!!) {
            drawerLayout?.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private class MyHandler : Handler() {
        override fun handleMessage(msg: Message) {
            // dummy
        }
    }

    override fun onDestroy() {
        mHandler.post { mCloudDBZoneWrapper?.closeCloudDBZone() }
        super.onDestroy()
    }
}