package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CbtisCatalog
import com.example.data.model.StudentProfile
import com.example.ui.components.CredentialScannerDialog
import com.example.ui.theme.MonoFontFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CredencialSetupScreen(
    existingProfile: StudentProfile?,
    defaultName: String,
    userEmail: String?,
    isSubmitting: Boolean,
    canNavigateBack: Boolean,
    onBack: () -> Unit,
    onSaveCredential: (
        numeroControl: String,
        fullName: String,
        specialty: String,
        semester: Long,
        shift: String
    ) -> Unit,
    onSignOut: () -> Unit
) {
    var numeroControl by remember(existingProfile) {
        mutableStateOf(existingProfile?.numeroControl.orEmpty())
    }
    var fullName by remember(existingProfile, defaultName) {
        mutableStateOf(existingProfile?.fullName?.ifBlank { defaultName } ?: defaultName)
    }
    var specialty by remember(existingProfile) {
        mutableStateOf(existingProfile?.specialty?.ifBlank { CbtisCatalog.specialties.first() } ?: CbtisCatalog.specialties.first())
    }
    var semester by remember(existingProfile) {
        mutableLongStateOf(existingProfile?.semester?.coerceIn(1L, 6L) ?: 3L)
    }
    var shift by remember(existingProfile) {
        mutableStateOf(existingProfile?.shift?.ifBlank { "Matutino" } ?: "Matutino")
    }
    var showScanner by remember { mutableStateOf(false) }
    var specialtyExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    if (showScanner) {
        CredentialScannerDialog(
            onCodeScanned = { code ->
                numeroControl = code
                validationError = null
                showScanner = false
            },
            onDismiss = { showScanner = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canNavigateBack) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("credential_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Regresar"
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (existingProfile == null) "Credencial Escolar" else "Actualizar Credencial",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Vincula tu Número de Control del CBTis 134",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_credential_guide),
                            contentDescription = "Guía visual de credencial escolar CBTis 134",
                            modifier = Modifier
                                .size(width = 112.dp, height = 84.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Escanea tu credencial",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Usa la cámara para leer el código de barras o QR impreso en tu credencial escolar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showScanner = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("scan_credential_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Escanear código QR / barras",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = numeroControl,
                onValueChange = { input ->
                    numeroControl = input.filter { it.isLetterOrDigit() || it == '-' }.take(20).uppercase()
                    validationError = null
                },
                label = { Text("Número de Control (Credencial)") },
                placeholder = { Text("Ej. 23320051340123") },
                leadingIcon = {
                    Icon(Icons.Rounded.Badge, contentDescription = null)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("numero_control_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it.take(80)
                    validationError = null
                },
                label = { Text("Nombre completo del alumno") },
                placeholder = { Text("Ej. Sofía Hernández López") },
                leadingIcon = {
                    Icon(Icons.Rounded.Person, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = specialtyExpanded,
                onExpandedChange = { specialtyExpanded = !specialtyExpanded }
            ) {
                OutlinedTextField(
                    value = specialty,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Especialidad") },
                    leadingIcon = {
                        Icon(Icons.Rounded.School, contentDescription = null)
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = specialtyExpanded)
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("specialty_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = specialtyExpanded,
                    onDismissRequest = { specialtyExpanded = false }
                ) {
                    CbtisCatalog.specialties.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                specialty = option
                                specialtyExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Semestre",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                (1L..6L).forEach { sem ->
                    FilterChip(
                        selected = semester == sem,
                        onClick = { semester = sem },
                        label = { Text("${sem}°") },
                        modifier = Modifier.testTag("semester_chip_$sem")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Turno",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CbtisCatalog.shifts.forEach { shiftOption ->
                    FilterChip(
                        selected = shift == shiftOption,
                        onClick = { shift = shiftOption },
                        label = { Text(shiftOption) },
                        modifier = Modifier.testTag("shift_chip_${shiftOption.lowercase()}")
                    )
                }
            }

            if (!userEmail.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cuenta vinculada: $userEmail",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (validationError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val cleanControl = numeroControl.trim()
                    val cleanName = fullName.trim()
                    when {
                        cleanControl.length < 6 -> {
                            validationError = "Ingresa o escanea un Número de Control válido (mínimo 6 caracteres)."
                        }
                        cleanName.length < 2 -> {
                            validationError = "Ingresa tu nombre completo tal como aparece en tu credencial."
                        }
                        else -> {
                            validationError = null
                            onSaveCredential(cleanControl, cleanName, specialty, semester, shift)
                        }
                    }
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_credential_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (existingProfile == null) "Ingresar a la Cafetería" else "Guardar cambios",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (existingProfile == null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("credential_sign_out_button")
                ) {
                    Text("Cambiar cuenta de Google")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
