package com.looker.droidify.compose.repoEdit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.looker.droidify.R
import com.looker.droidify.compose.components.BackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoEditScreen(
    repoId: Int?,
    onBackClick: () -> Unit,
    viewModel: RepoEditViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val errorState by viewModel.errorState.collectAsState()
    val authEnabled by viewModel.authEnabled.collectAsState()
    val isFormValid by remember { derivedStateOf { !errorState.hasError } }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(repoId) {
        repoId?.let { viewModel.loadRepo(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (repoId != null) R.string.edit_repository
                            else R.string.add_repository,
                        ),
                    )
                },
                navigationIcon = { BackButton(onBackClick) },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveRepository() },
                        enabled = isFormValid,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                val hasAddressError by remember { derivedStateOf { errorState.addressError != null } }
                OutlinedTextField(
                    value = viewModel.addressState.text.toString(),
                    onValueChange = { viewModel.addressState.edit { replace(0, length, it) } },
                    label = { Text(stringResource(R.string.address)) },
                    isError = hasAddressError,
                    supportingText = { errorState.addressError?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                val hasFingerprintError by remember { derivedStateOf { errorState.fingerprintError != null } }
                OutlinedTextField(
                    value = viewModel.fingerprintState.text.toString(),
                    onValueChange = { viewModel.fingerprintState.edit { replace(0, length, it) } },
                    label = { Text(stringResource(R.string.fingerprint)) },
                    isError = hasFingerprintError,
                    supportingText = { errorState.fingerprintError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.requires_authentication))
                    Switch(
                        checked = authEnabled,
                        onCheckedChange = { viewModel.setAuthEnabled(it) },
                    )
                }

                AnimatedVisibility(visible = authEnabled) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))

                        val hasUsernameError by remember { derivedStateOf { errorState.usernameError != null } }
                        OutlinedTextField(
                            value = viewModel.usernameState.text.toString(),
                            onValueChange = { viewModel.usernameState.edit { replace(0, length, it) } },
                            label = { Text(stringResource(R.string.username)) },
                            isError = hasUsernameError,
                            supportingText = { errorState.usernameError?.let { Text(it) } },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val hasPasswordError by remember { derivedStateOf { errorState.passwordError != null } }
                        OutlinedTextField(
                            value = viewModel.passwordState.text.toString(),
                            onValueChange = { viewModel.passwordState.edit { replace(0, length, it) } },
                            label = { Text(stringResource(R.string.password)) },
                            isError = hasPasswordError,
                            supportingText = { errorState.passwordError?.let { Text(it) } },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Skip check button
                Button(
                    onClick = { viewModel.saveRepository(skipCheck = true) },
                    enabled = isFormValid || isLoading,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.skip))
                }
            }

            // Loading overlay
            AnimatedVisibility(
                visible = isLoading,
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.checking_repository),
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}
