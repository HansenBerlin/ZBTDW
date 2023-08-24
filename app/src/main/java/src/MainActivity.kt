/*
 * Copyright 2023 Vincent Tsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package src

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import src.theme.NewEmptyComposeAppTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val REQUEST_PERMISSIONS = 123
        const val BATCH_SIZE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            val (progressValue, setProgressValue) = remember { mutableStateOf(0) }

            NewEmptyComposeAppTheme(useSystemUIController = false) {
                MainScreen(
                    createContacts = { numContacts, domain, deleteContacts ->
                        CoroutineScope(Dispatchers.Main).launch {
                            createContactsAsync(numContacts, domain, deleteContacts, setProgressValue)
                        }
                    },
                    progress = progressValue,
                )
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_CONTACTS), REQUEST_PERMISSIONS)
        }
    }

    private fun setupSplashScreen() {
        var keepSplashScreenOn = true
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                delay(2000)
                keepSplashScreenOn = false
            }
        }

        installSplashScreen().setKeepOnScreenCondition {
            keepSplashScreenOn
        }
    }

    private fun deleteAllContacts() {
        val contactsUri = ContactsContract.RawContacts.CONTENT_URI
        contentResolver.delete(contactsUri, null, null)
    }

    private suspend fun createContactsAsync(numContacts: Int, domain: String, deleteContacts: Boolean, updateProgress: (Int) -> Unit) {
        updateProgress(0)
        if (deleteContacts) {
            deleteAllContacts()
        }
        val batchSize = if (numContacts < BATCH_SIZE) numContacts else BATCH_SIZE

        val totalProgressSteps = numContacts / batchSize
        var currentProgressStep = 0
        val deferredJobs = mutableListOf<Job>()

        for (i in 1..numContacts step batchSize) {
            val endRange = minOf(i + batchSize - 1, numContacts)
            val job = GlobalScope.launch(Dispatchers.IO) {
                for (j in i..endRange) {
                    val contactValues = ContentValues()
                    contactValues.put(ContactsContract.RawContacts.ACCOUNT_TYPE, null as String?)
                    contactValues.put(ContactsContract.RawContacts.ACCOUNT_NAME, null as String?)

                    val rawContactUri = contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, contactValues)
                    val rawContactId = ContentUris.parseId(rawContactUri ?: return@launch)

                    val nameValues = ContentValues()
                    nameValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    nameValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)

                    val randomPrefix = ('a'..'z').random()

                    nameValues.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, "$randomPrefix contact $j")

                    contentResolver.insert(ContactsContract.Data.CONTENT_URI, nameValues)

                    val emailValues = ContentValues()
                    emailValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    emailValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    // emailValues.put(ContactsContract.CommonDataKinds.Email.ADDRESS, "contact$j@example.com")
                    val email = "contact$j@$domain"
                    emailValues.put(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                    emailValues.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)

                    contentResolver.insert(ContactsContract.Data.CONTENT_URI, emailValues)
                    currentProgressStep++
                    val currentProgress = (currentProgressStep * 100) / totalProgressSteps
                    updateProgress(currentProgress)
                }
            }
            deferredJobs.add(job)
        }

        deferredJobs.forEach { it.join() }
    }
}
