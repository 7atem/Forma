package com.formacoach.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.formacoach.model.StarterTemplates
import com.formacoach.wearables.*
import kotlinx.coroutines.*

@Composable
internal fun WatchTransferDialog(vm:FitnessViewModel,state:AppState,onDismiss:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var busy by remember {mutableStateOf(false)}
    var preview by remember {mutableStateOf<WatchTransfer?>(null)}
    var message by remember {mutableStateOf<Int?>(null)}
    var pending by remember {mutableStateOf<ByteArray?>(null)}
    var selected by remember {mutableStateOf(setOf<String>())}
    val a=stringResource(R.string.template_a);val b=stringResource(R.string.template_b)
    val programs=state.snapshot.programs.map {WatchProgram(it.id,it.name,it.exercises,it.restSeconds)}+
        StarterTemplates.all.map {WatchProgram("starter-${it.id}",if(it.id=="A") a else b,it.exercises,90)}
    DisposableEffect(Unit) {onDispose {pending?.fill(0)}}
    val create=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {uri ->
        val bytes=pending;pending=null
        if(uri!=null&&bytes!=null) scope.launch {
            busy=true
            try {withContext(Dispatchers.IO) {checkNotNull(context.contentResolver.openOutputStream(uri,"wt")).use {it.write(bytes)}};message=R.string.watch_exported}
            catch(e:CancellationException) {throw e} catch(_:Exception) {message=R.string.watch_failed}
            finally {bytes.fill(0);busy=false}
        } else bytes?.fill(0)
    }
    val open=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri ->
        if(uri!=null) scope.launch {
            busy=true;message=null;preview=null
            try {preview=withContext(Dispatchers.IO) {
                checkNotNull(context.contentResolver.openInputStream(uri)).use {WatchTransferCodec.decode(WatchTransferCodec.read(it))}.also {require(it.workouts.isNotEmpty())}
            }} catch(e:CancellationException) {throw e} catch(_:Exception) {message=R.string.watch_failed}
            finally {busy=false}
        }
    }
    AlertDialog(onDismissRequest={if(!busy&&!state.busy) onDismiss()},title={Text(stringResource(R.string.apple_watch))},text={
        Column(Modifier.heightIn(max=560.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.watch_bridge_note))
            Text(stringResource(R.string.watch_plaintext_note),style=MaterialTheme.typography.bodySmall)
            message?.let {Text(stringResource(it))}
            if(busy||state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            val transfer=preview
            if(transfer!=null) {
                Text(stringResource(R.string.watch_preview,number(transfer.workouts.size.toDouble(),0),number(transfer.workouts.sumOf {it.sets.size}.toDouble(),0)))
                transfer.workouts.forEach {Text("${it.programName} · ${dateLabel(it.date)}")}
                Text(stringResource(R.string.watch_import_note))
                Button(onClick={vm.importWatch(transfer){preview=null;message=R.string.watch_imported}},enabled=!state.busy,modifier=Modifier.testTag("watch_confirm_import")) {Text(stringResource(R.string.watch_import))}
                if(state.error) Text(stringResource(R.string.watch_conflict))
                TextButton(onClick={preview=null}) {Text(stringResource(R.string.cancel))}
            } else {
                Text(stringResource(R.string.watch_select_programs))
                programs.forEach {program -> FilterChip(selected=program.id in selected,onClick={selected=if(program.id in selected) selected-program.id else (selected+program.id).take(20).toSet()},label={Text(program.name)})}
                Button(onClick={
                    runCatching {WatchTransferCodec.encode(WatchTransfer(programs=programs.filter {it.id in selected}))}.onSuccess {pending=it;create.launch("forma-programs.formaworkouts")}.onFailure {message=R.string.watch_failed}
                },enabled=!busy&&!state.busy&&selected.isNotEmpty(),modifier=Modifier.testTag("watch_export")) {Text(stringResource(R.string.watch_export))}
                OutlinedButton(onClick={open.launch(arrayOf("*/*"))},enabled=!busy&&!state.busy,modifier=Modifier.testTag("watch_open_import")) {Text(stringResource(R.string.watch_open))}
            }
        }
    },confirmButton={},dismissButton={TextButton(onClick=onDismiss,enabled=!busy&&!state.busy) {Text(stringResource(R.string.close))}})
}
