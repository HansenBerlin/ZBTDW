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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MainScreen(createContacts: (Int, String, Boolean) -> Unit, progress: Int) {
    var emailInput by remember { mutableStateOf("test.de") }
    var deleteContacts by remember { mutableStateOf(true) }
    var numContacts by remember { mutableStateOf(0) }
    var isCreatingContacts by remember { mutableStateOf(false) } // New state for tracking contact creation

    LinearProgressIndicator(
        progress = progress / 100f,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Zweitbeste Tool der Welt: \nAdd and remove dummy contacts",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp, start = 8.dp, end = 8.dp),
        )

        TextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text("Email Domain") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        )

        TextField(
            value = numContacts.toString(),
            onValueChange = { newValue ->
                numContacts = newValue.toIntOrNull() ?: 1
            },
            label = { Text("Number of contacts to add") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        )

        Row(
            modifier = Modifier
                .align(Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = deleteContacts,
                onCheckedChange = { deleteContacts = it },
            )
            Text("delete contacts before adding")
        }

        Spacer(modifier = Modifier.weight(1f)) // Spacer to push the Button to the bottom

        Button(
            onClick = {
                if (!isCreatingContacts) { // Prevent duplicate clicks
                    isCreatingContacts = true
                    CoroutineScope(Dispatchers.Main).launch {
                        createContacts(numContacts, emailInput, deleteContacts)
                        isCreatingContacts = false
                    }
                }
            },
            enabled = !isCreatingContacts, // Disable the button while contacts are being created
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .align(Alignment.End),
        ) {
            Text(text = "CREATE / NUKE CONTACTS")
        }

        // LinearProgressIndicator to show progress
    }
}
