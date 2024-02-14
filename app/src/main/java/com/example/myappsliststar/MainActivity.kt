package com.example.myappsliststar

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
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
import kotlin.Pair



class MainActivity : AppCompatActivity() {

    private val saveFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                val (nonSystemApps, systemApps) = getInstalledApps()
                val allApps = nonSystemApps + systemApps
                writeAppsToOutputStream(uri, allApps)
            }
        }
    }

    private fun writeAppsToOutputStream(uri: Uri, apps: List<Pair<String, String>>) {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            for ((appName, packageName) in apps) {
                outputStream.write("${appName.uppercase(Locale.ROOT)} ($packageName)\n".toByteArray())
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
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





    class AppsAdapter(context: Context,
                      nonSystemApps: List<Pair<String, String>>, systemApps: List<Pair<String, String>>
    ) : BaseAdapter() {
        private val context: Context = context
        private val nonSystemApps: List<Pair<String, String>> = nonSystemApps
        private val systemApps: List<Pair<String, String>> = systemApps

        override fun getCount(): Int {
            return nonSystemApps.size + systemApps.size + 2 // Add 2 for the titles
        }

        override fun getItem(position: Int): Any {
            return when (position) {
                0 -> "User Apps"
                nonSystemApps.size + 1 -> "System Apps"
                in 1 until nonSystemApps.size + 1 -> nonSystemApps[position - 1].first
                else -> systemApps[position - nonSystemApps.size - 2].first
            }
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val titleView = TextView(context).apply {
                textSize = 20f
                setTextColor(Color.RED)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }
            val textview = TextView(context).apply{
                textSize=18f
                setTextColor(Color.GRAY)
            }

            return when (position) {
                0 -> {
                    titleView.text = "User Apps"
                    titleView
                }
                nonSystemApps.size + 1 -> {
                    titleView.text = "System Apps"
                    titleView
                }
                in 1 until nonSystemApps.size + 1 -> {
                    textview.text = "${nonSystemApps[position - 1].first} (${nonSystemApps[position - 1].second})"
                    textview
                }
                else -> {
                    textview.text = "${systemApps[position - nonSystemApps.size - 2].first} (${systemApps[position - nonSystemApps.size - 2].second})"
                    textview
                }
            }
        }
    }


    @SuppressLint("QueryPermissionsNeeded")
    private fun getInstalledApps(): Pair<List<Pair<String, String>>, List<Pair<String, String>>> {
        val packageManager: PackageManager = packageManager
        val nonSystemApps = mutableListOf<Pair<String, String>>()
        val systemApps = mutableListOf<Pair<String, String>>()

        val installedPackages = packageManager.getInstalledPackages(
            PackageManager.GET_META_DATA or PackageManager.GET_SHARED_LIBRARY_FILES
        )

        for (packageInfo in installedPackages) {
            val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()
            val packageName = packageInfo.packageName
            val isSystemApp = packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0

            val appPair = Pair(appName, packageName)

            if (!isSystemApp && !nonSystemApps.contains(appPair)) {
                nonSystemApps.add(appPair)
            } else if (isSystemApp && !systemApps.contains(appPair)) {
                systemApps.add(appPair)
            }
        }

        return Pair(nonSystemApps.sortedBy { it.first }, systemApps.sortedBy { it.first })
    }




    private fun updateListView(nonSystemApps: List<Pair<String, String>>, systemApps: List<Pair<String,String>>) {
        // Display the sorted list in a ListView
        val listView: ListView = findViewById(R.id.appListView)

        // Create a custom adapter for your apps
        val appsAdapter = AppsAdapter(this, nonSystemApps, systemApps)

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
