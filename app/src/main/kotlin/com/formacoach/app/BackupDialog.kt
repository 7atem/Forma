package com.formacoach.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.formacoach.backup.PortableBackup
import com.formacoach.model.FitnessSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun BackupDialog(vm:FitnessViewModel,state:AppState,onDismiss:()->Unit,onRestored:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var password by remember {mutableStateOf("")}
    var confirmation by remember {mutableStateOf("")}
    var mode by remember {mutableIntStateOf(0)}
    var busy by remember {mutableStateOf(false)}
    var pendingExport by remember {mutableStateOf<ByteArray?>(null)}
    var selectedUri by remember {mutableStateOf<Uri?>(null)}
    var preview by remember {mutableStateOf<FitnessSnapshot?>(null)}
    var message by remember {mutableStateOf<Int?>(null)}
    DisposableEffect(Unit) {onDispose {pendingExport?.fill(0)}}
    val createFile=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {uri ->
        val bytes=pendingExport;pendingExport=null
        if(uri==null) {bytes?.fill(0);busy=false}
        else if(bytes==null) {message=R.string.backup_interrupted;busy=false}
        else scope.launch {
            busy=true
            try {
                withContext(Dispatchers.IO) {checkNotNull(context.contentResolver.openOutputStream(uri,"wt")).use {it.write(bytes);it.flush()}}
                message=R.string.backup_exported
            } catch(failure:CancellationException) {throw failure
            } catch(_:Exception) {message=R.string.backup_failed
            } finally {bytes.fill(0);busy=false;mode=0}
        }
    }
    val openFile=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri ->
        if(uri!=null) {selectedUri=uri;password="";mode=2;message=null}
    }
    AlertDialog(onDismissRequest={if(!busy&&!state.busy) onDismiss()},title={Text(stringResource(R.string.backup))},
        text={Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.backup_description))
            message?.let {Text(stringResource(it),modifier=Modifier.testTag("backup_message"))}
            if(busy||state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            val restore=preview
            when {
                restore!=null -> {
                    Text(stringResource(R.string.restore_preview),style=MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.restore_counts,number(restore.sessions.size.toDouble(),0),number(restore.meals.size.toDouble(),0),number(restore.measurements.size.toDouble(),0),number(restore.programs.size.toDouble(),0),number(restore.foods.size.toDouble(),0)))
                    Text(stringResource(R.string.restore_replace_note))
                    Text(stringResource(R.string.restore_exercise_preferences,number(restore.exercisePreferences.size.toDouble(),0)))
                    Button(onClick={vm.restore(restore){preview=null;onRestored()}},enabled=!state.busy,modifier=Modifier.fillMaxWidth().testTag("confirm_restore")) {Text(stringResource(R.string.confirm_restore))}
                    if(state.error) Text(stringResource(R.string.restore_failed))
                    TextButton(onClick={preview=null;mode=0},enabled=!state.busy) {Text(stringResource(R.string.cancel))}
                }
                mode==1 || mode==2 -> {
                    OutlinedTextField(password,onValueChange={password=it},label={Text(stringResource(R.string.backup_password))},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,imeAction=ImeAction.Done),singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("backup_password"))
                    if(mode==1) OutlinedTextField(confirmation,onValueChange={confirmation=it},label={Text(stringResource(R.string.confirm_password))},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,imeAction=ImeAction.Done),singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("confirm_password"))
                    Text(stringResource(R.string.password_note),style=MaterialTheme.typography.bodySmall)
                    Button(onClick={scope.launch {
                        busy=true;message=null
                        val secret=password.toCharArray();password="";confirmation=""
                        try {
                            if(mode==1) {
                                pendingExport=vm.export(secret)
                                createFile.launch("forma-${java.time.LocalDate.now(vm.clock)}.formabackup")
                            } else {
                                val bytes=withContext(Dispatchers.IO) {checkNotNull(context.contentResolver.openInputStream(checkNotNull(selectedUri))).use {PortableBackup.readBounded(it)}}
                                preview=vm.previewRestore(bytes,secret)
                            }
                        } catch(failure:CancellationException) {throw failure
                        } catch(_:Exception) {message=if(mode==1) R.string.backup_failed else R.string.restore_failed
                        } finally {secret.fill('\u0000');busy=false}
                    }},enabled=!busy && password.length in 12..1024 && (mode==2||password==confirmation),modifier=Modifier.fillMaxWidth().testTag("prepare_backup")) {Text(stringResource(if(mode==1) R.string.export_backup else R.string.read_backup))}
                    TextButton(onClick={password="";confirmation="";mode=0},enabled=!busy) {Text(stringResource(R.string.cancel))}
                }
                else -> {
                    Button(onClick={mode=1;message=null},enabled=state.snapshot.profile!=null&&!busy&&!state.busy,modifier=Modifier.fillMaxWidth().testTag("export_backup")) {Text(stringResource(R.string.export_backup))}
                    OutlinedButton(onClick={openFile.launch(arrayOf("*/*"))},enabled=!busy&&!state.busy,modifier=Modifier.fillMaxWidth().testTag("open_backup")) {Text(stringResource(R.string.restore_backup))}
                }
            }
        }},confirmButton={},dismissButton={TextButton(onClick=onDismiss,enabled=!busy&&!state.busy) {Text(stringResource(R.string.close))}})
}
