package com.example.myappsliststar

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import android.graphics.Color
import android.net.Uri
import android.view.ViewGroup
import android.widget.BaseAdapter


class MainActivity : AppCompatActivity() {

    private val saveFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                contentResolver
                val (nonSystemApps, systemApps) = getInstalledApps()
                writeAppsToOutputStream(uri, nonSystemApps ,systemApps)
            }
        }
    }
    private fun writeAppsToOutputStream(uri: Uri, nonSystemApps: List<String>, systemApps: List<String>) {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            val allApps = nonSystemApps + systemApps
            for (app in allApps) {
                outputStream.write("${app.uppercase(Locale.ROOT)} \n".toByteArray())
            }
        }
    }override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the List Apps Button by its ID and set the click listener
        val listAppsButton: Button = findViewById(R.id.listAppsButton)
        listAppsButton.setOnClickListener {
            // Handle List Apps button click
            val (nonSystemApps, systemApps) = getInstalledApps()
            updateListView(nonSystemApps, systemApps)
        }

        // Find the Save to File Button by its ID and set the click listener
        val saveToFileButton: Button = findViewById(R.id.saveToFileButton)
        saveToFileButton.setOnClickListener {
            // Handle Save to File button click
            createFile()
        }
    }




    class AppsAdapter(context: Context, userApps: List<String>, systemApps: List<String>) : BaseAdapter() {
        private val context: Context = context
        private val userApps: List<String> = userApps
        private val systemApps: List<String> = systemApps

        override fun getCount(): Int {
            return userApps.size + systemApps.size + 2 // Add 2 for the titles
        }

        override fun getItem(position: Int): Any {
            return when (position) {
                0 -> "User Apps"
                userApps.size + 1 -> "System Apps"
                in 1 until userApps.size + 1 -> userApps[position - 1]
                else -> systemApps[position - userApps.size - 2]
            }
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val titleView = TextView(context)
            titleView.apply {
                textSize = 20f
                setTextColor(Color.RED)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }

            return when (position) {
                0 -> {
                    titleView.text = "User Apps"
                    titleView
                }
                userApps.size + 1 -> {
                    titleView.text = "System Apps"
                    titleView
                }
                in 1 until userApps.size + 1 -> {
                    val userAppView = TextView(context)
                    userAppView.text = userApps[position - 1]
                    userAppView
                }
                else -> {
                    val systemAppView = TextView(context)
                    systemAppView.text = systemApps[position - userApps.size - 2]
                    systemAppView
                }
            }
        }
    }




    @SuppressLint("QueryPermissionsNeeded")
    private fun getInstalledApps(): Pair<List<String>, List<String>> {
        val packageManager = packageManager
        val nonSystemApps = mutableListOf<String>()
        val systemApps = mutableListOf<String>()

        val installedPackages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA or PackageManager.GET_SHARED_LIBRARY_FILES)

        for (packageInfo in installedPackages) {
            val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()
            val isSystemApp = packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0

            if (!isSystemApp && !nonSystemApps.contains(appName)) {
                nonSystemApps.add(appName)
            } else if (isSystemApp && !systemApps.contains(appName)) {
                systemApps.add(appName)
            }
        }

        return Pair(nonSystemApps.sorted(), systemApps.sorted())
    }


    private fun updateListView(userApps: List<String>, systemApps: List<String>) {
        // Display the sorted list in a ListView
        val listView: ListView = findViewById(R.id.appListView)

        // Create a custom adapter for your apps
        val appsAdapter = AppsAdapter(this, userApps, systemApps)

        // Set the custom adapter
        listView.adapter = appsAdapter
    }


    private fun createFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "Installed_apps.txt")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                putExtra(MediaStore.EXTRA_SIZE_LIMIT, 1024 * 1024) // Optional: Specify a size limit
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
        }

        saveFileLauncher.launch(intent)
    }
}
