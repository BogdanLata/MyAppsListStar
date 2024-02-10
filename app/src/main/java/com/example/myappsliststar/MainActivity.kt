package com.example.myappsliststar

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val saveFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                val contentResolver: ContentResolver = contentResolver
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val installedApps = getInstalledApps()
                    for (app in installedApps) {
                        outputStream.write("${app.uppercase(Locale.ROOT)} \n".toByteArray())
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the List Apps Button by its ID and set the click listener
        val listAppsButton: Button = findViewById(R.id.listAppsButton)
        listAppsButton.setOnClickListener {
            // Handle List Apps button click
            val installedApps = getInstalledApps()
            updateListView(installedApps)
        }

        // Find the Save to File Button by its ID and set the click listener
        val saveToFileButton: Button = findViewById(R.id.saveToFileButton)
        saveToFileButton.setOnClickListener {
            // Handle Save to File button click
            createFile()
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun getInstalledApps(): List<String> {
        val packageManager: PackageManager = packageManager
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

        return nonSystemApps.sorted() + systemApps.sorted()
    }

    private fun updateListView(apps: List<String>) {
        // Display the sorted list in a ListView
        val listView: ListView = findViewById(R.id.appListView)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, apps)
        listView.adapter = adapter
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
