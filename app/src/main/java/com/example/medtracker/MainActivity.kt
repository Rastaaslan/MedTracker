package com.example.medtracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medtracker.domain.*
import com.example.medtracker.notifications.AlarmReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter

data class DoseRow(val plan:ScheduledDose,val item:TreatmentWithRules,val intake:IntakeEvent?)
class MainViewModel(app:android.app.Application):AndroidViewModel(app){
    private val repository=(app as MedTrackerApplication).repository
    private val engine=SchedulingEngine()
    val treatments=repository.treatments.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),emptyList())
    val intakes=repository.intakes.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),emptyList())
    val today=combine(treatments,intakes){ts,ins->
        val date=LocalDate.now(); val zone=ZoneId.systemDefault()
        ts.filter{it.treatment.active}.flatMap { item ->
            val latest=ins.filter{it.treatmentId==item.treatment.id}.maxByOrNull{it.takenAt}
            engine.dosesForDay(item,date,zone,latest).map { dose -> DoseRow(dose,item,ins.firstOrNull{it.scheduledDoseKey==dose.key}) }
        }.sortedBy{it.plan.scheduledAt}
    }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),emptyList())
    init { viewModelScope.launch { today.collect { rows -> val scheduler=AlarmReminderScheduler(app);rows.filter{it.intake==null&&it.plan.scheduledAt>Instant.now()}.forEach{scheduler.schedule(it.plan,it.item.medication.name,it.item.treatment.doseText)} } } }
    fun take(row:DoseRow,at:Instant=Instant.now())=viewModelScope.launch{repository.markTaken(row.item.treatment.id,row.plan.key,at)}
    fun takeNow(treatmentId:Long)=viewModelScope.launch{repository.markTaken(treatmentId,null,Instant.now())}
    fun cancel(id:Long)=viewModelScope.launch{repository.cancel(id)}
    fun correct(event:IntakeEvent,at:Instant)=viewModelScope.launch{repository.correct(event,at)}
    fun archive(id:Long)=viewModelScope.launch{repository.archive(id)}
    fun create(name:String,notes:String,dose:String,start:LocalDate,end:LocalDate?,mode:ScheduleMode,interval:Long?,rules:List<Pair<String,LocalTime?>>,done:()->Unit)=viewModelScope.launch{repository.create(name,notes,dose,start,end,mode,interval,rules);done()}
    fun update(item:TreatmentWithRules,name:String,notes:String,dose:String,start:LocalDate,end:LocalDate?,mode:ScheduleMode,interval:Long?,rules:List<Pair<String,LocalTime?>>,done:()->Unit)=viewModelScope.launch{repository.update(item,name,notes,dose,start,end,mode,interval,rules);done()}
    fun warning(row:DoseRow,at:Instant):String?=engine.intervalWarning(row.item.treatment,intakes.value.filter{it.treatmentId==row.item.treatment.id}.maxByOrNull{it.takenAt},at)
}

class MainActivity:ComponentActivity(){
    private val vm by viewModels<MainViewModel>()
    private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MedTrackerTheme{MedTrackerApp(vm){if(Build.VERSION.SDK_INT>=33)permission.launch(Manifest.permission.POST_NOTIFICATIONS)}}}}
}

@Composable fun MedTrackerTheme(content:@Composable()->Unit)=MaterialTheme(colorScheme=if(androidx.compose.foundation.isSystemInDarkTheme())darkColorScheme() else lightColorScheme(primary=androidx.compose.ui.graphics.Color(0xFF006C4C)),content=content)

enum class Screen(val title:String){TODAY("Aujourd’hui"),TREATMENTS("Traitements"),HISTORY("Historique"),SETTINGS("Réglages")}
@Composable fun MedTrackerApp(vm:MainViewModel,requestNotifications:()->Unit){
    var screen by remember{mutableStateOf(Screen.TODAY)}
    Scaffold(bottomBar={NavigationBar{Screen.entries.forEach{item->NavigationBarItem(selected=screen==item,onClick={screen=item},icon={Icon(when(item){Screen.TODAY->Icons.Default.Today;Screen.TREATMENTS->Icons.Default.Medication;Screen.HISTORY->Icons.Default.History;Screen.SETTINGS->Icons.Default.Settings},null)},label={Text(item.title)})}}}){padding->
        Box(Modifier.padding(padding)){when(screen){Screen.TODAY->TodayScreen(vm);Screen.TREATMENTS->TreatmentsScreen(vm);Screen.HISTORY->HistoryScreen(vm);Screen.SETTINGS->SettingsScreen(requestNotifications)}}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun Page(title:String,actions:@Composable RowScope.()->Unit={},content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize()){TopAppBar(title={Text(title,fontWeight=FontWeight.Bold)},actions=actions);Column(Modifier.fillMaxSize().padding(horizontal=16.dp),content=content)}}

@Composable fun TodayScreen(vm:MainViewModel){val rows by vm.today.collectAsStateWithLifecycle();Page("Aujourd’hui"){
    Text(rows.firstOrNull{it.intake==null}?.let{"Prochaine prise : ${time(it.plan.scheduledAt)} — ${it.plan.label}"}?:"Aucune prochaine prise aujourd’hui",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(vertical=8.dp))
    if(rows.isEmpty()) Empty("Aucune prise planifiée aujourd’hui.") else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(rows,key={it.plan.key}){row->DoseCard(row,vm)}}
}}
@Composable fun DoseCard(row:DoseRow,vm:MainViewModel){var correct by remember{mutableStateOf(false)};var warning by remember{mutableStateOf<String?>(null)}
    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text("${time(row.plan.scheduledAt)} — ${row.plan.label}",fontWeight=FontWeight.Bold);Text("${row.item.medication.name} — ${row.item.treatment.doseText}")
        row.intake?.let{Text("Pris à ${time(it.takenAt)}",color=MaterialTheme.colorScheme.primary);Row{TextButton(onClick={correct=true}){Text("Corriger l’heure")};TextButton(onClick={vm.cancel(it.id)}){Text("Annuler")}}}?:Button(onClick={warning=vm.warning(row,Instant.now());vm.take(row)},modifier=Modifier.fillMaxWidth().semantics{contentDescription="Marquer ${row.plan.label} comme prise"}){Text("Marquer comme prise")}
        warning?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }}
    if(correct&&row.intake!=null) TimeDialog("Corriger l’heure",row.intake.takenAt){correct=false;it?.let{at->vm.correct(row.intake,at)}}
}

@Composable fun TreatmentsScreen(vm:MainViewModel){val values by vm.treatments.collectAsStateWithLifecycle();var form by remember{mutableStateOf(false)};var editing by remember{mutableStateOf<TreatmentWithRules?>(null)};Page("Traitements",actions={IconButton(onClick={form=true}){Icon(Icons.Default.Add,"Ajouter un traitement")}}){
    if(values.isEmpty()) Empty("Aucun traitement. Touchez + pour en créer un.") else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(values,key={it.treatment.id}){item->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(item.medication.name,fontWeight=FontWeight.Bold);Text(item.treatment.doseText);Text(if(item.treatment.active)"Actif" else "Archivé");if(item.treatment.active&&item.treatment.scheduleMode==ScheduleMode.INTERVAL_FROM_LAST)Button(onClick={vm.takeNow(item.treatment.id)}){Text("Enregistrer une prise maintenant")};Row{if(item.treatment.active)TextButton(onClick={editing=item}){Text("Modifier")};if(item.treatment.active)TextButton(onClick={vm.archive(item.treatment.id)}){Text("Archiver")}}}}}}
    if(form)TreatmentDialog(vm){form=false};editing?.let{TreatmentDialog(vm,it){editing=null}}
}}

@Composable fun HistoryScreen(vm:MainViewModel){val events by vm.intakes.collectAsStateWithLifecycle();val treatments by vm.treatments.collectAsStateWithLifecycle();Page("Historique"){
    if(events.isEmpty())Empty("Aucune prise enregistrée.") else LazyColumn{items(events,key={it.id}){event->val item=treatments.firstOrNull{it.treatment.id==event.treatmentId};ListItem(headlineContent={Text(item?.medication?.name?:"Traitement")},supportingContent={Text("Pris le ${dateTime(event.takenAt)}")},leadingContent={Icon(Icons.Default.CheckCircle,null)})}}
}}
@Composable fun SettingsScreen(request:()->Unit)=Page("Réglages"){
    Text("Rappels locaux",fontWeight=FontWeight.Bold);Text("Les notifications sont facultatives. Un refus n’empêche jamais l’accès aux données ou à l’historique.");Button(onClick=request,modifier=Modifier.padding(vertical=12.dp)){Text("Autoriser les notifications")}
    HorizontalDivider();Text("Confidentialité",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp));Text("Toutes les données restent sur cet appareil. Aucun compte, suivi ou service cloud.");Text("MedTracker suit uniquement les informations que vous saisissez et ne fournit aucun conseil médical.",modifier=Modifier.padding(top=12.dp))
}
@Composable fun Empty(text:String)=Box(Modifier.fillMaxWidth().padding(32.dp),contentAlignment=Alignment.Center){Text(text)}
private fun time(at:Instant)=DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(at)
private fun dateTime(at:Instant)=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault()).format(at)

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TimeDialog(title:String,initial:Instant,onDone:(Instant?)->Unit){
    val local=initial.atZone(ZoneId.systemDefault());val state=rememberTimePickerState(local.hour,local.minute,true)
    AlertDialog(onDismissRequest={onDone(null)},title={Text(title)},text={TimePicker(state)},confirmButton={TextButton(onClick={onDone(LocalDate.now().atTime(state.hour,state.minute).atZone(ZoneId.systemDefault()).toInstant())}){Text("Enregistrer")}},dismissButton={TextButton(onClick={onDone(null)}){Text("Annuler")}})
}

@Composable fun TreatmentDialog(vm:MainViewModel,existing:TreatmentWithRules?=null,onDismiss:()->Unit){
    var name by remember{mutableStateOf(existing?.medication?.name?:"")};var dose by remember{mutableStateOf(existing?.treatment?.doseText?:"")};var notes by remember{mutableStateOf(existing?.medication?.optionalNotes?:"")};var rulesText by remember{mutableStateOf(existing?.rules?.joinToString("; "){"${it.label} ${it.timeOfDay?:""}"}?:"Matin 08:00")};var interval by remember{mutableStateOf(existing?.treatment?.minIntervalMinutes?.toString()?:"")};var mode by remember{mutableStateOf(existing?.treatment?.scheduleMode?:ScheduleMode.FIXED_TIMES)};var start by remember{mutableStateOf(existing?.treatment?.startDate?.toString()?:LocalDate.now().toString())};var end by remember{mutableStateOf(existing?.treatment?.endDate?.toString()?:"")};var error by remember{mutableStateOf<String?>(null)}
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(existing==null)"Nouveau traitement" else "Modifier le traitement")},text={LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{Text("Toutes les informations ci-dessous sont saisies par vous.",style=MaterialTheme.typography.bodySmall)}
        item{OutlinedTextField(name,{name=it},label={Text("Médicament ou libellé *")},singleLine=true)}
        item{OutlinedTextField(dose,{dose=it},label={Text("Dose textuelle *")},singleLine=true)}
        item{OutlinedTextField(notes,{notes=it},label={Text("Notes (facultatif)")})}
        item{Text("Mode",fontWeight=FontWeight.Bold);Row{RadioButton(mode==ScheduleMode.FIXED_TIMES,{mode=ScheduleMode.FIXED_TIMES});Text("Horaires fixes",Modifier.align(Alignment.CenterVertically))};Row{RadioButton(mode==ScheduleMode.INTERVAL_FROM_LAST,{mode=ScheduleMode.INTERVAL_FROM_LAST});Text("Depuis la dernière prise",Modifier.align(Alignment.CenterVertically))}}
        item{OutlinedTextField(rulesText,{rulesText=it},label={Text(if(mode==ScheduleMode.FIXED_TIMES)"Prises (ex. Matin 08:00; Soir 20:00) *" else "Libellé de prise *")})}
        item{OutlinedTextField(interval,{interval=it.filter(Char::isDigit)},label={Text(if(mode==ScheduleMode.INTERVAL_FROM_LAST)"Intervalle en minutes *" else "Intervalle minimal en minutes (facultatif)")},singleLine=true)}
        item{OutlinedTextField(start,{start=it},label={Text("Date de début (AAAA-MM-JJ) *")},singleLine=true)}
        item{OutlinedTextField(end,{end=it},label={Text("Date de fin (facultative)")},singleLine=true)}
        error?.let{item{Text(it,color=MaterialTheme.colorScheme.error)}}
    }},confirmButton={Button(onClick={
        val startDate=runCatching{LocalDate.parse(start)}.getOrNull();val endDate=end.takeIf{it.isNotBlank()}?.let{runCatching{LocalDate.parse(it)}.getOrNull()};val minutes=interval.toLongOrNull()
        val rules=if(mode==ScheduleMode.FIXED_TIMES)rulesText.split(';').mapNotNull{part->val bits=part.trim().split(Regex("\\s+(?=\\d{2}:\\d{2}$)"));if(bits.size==2)runCatching{bits[0] to LocalTime.parse(bits[1])}.getOrNull() else null}else listOf(rulesText.trim() to null)
        if(name.isBlank()||dose.isBlank()||rules.isEmpty()||startDate==null||(end.isNotBlank()&&endDate==null)||(endDate!=null&&endDate<startDate)||(mode==ScheduleMode.INTERVAL_FROM_LAST&&(minutes?:0)<=0)) error="Vérifiez les champs obligatoires et les dates." else if(existing==null)vm.create(name,notes,dose,startDate,endDate,mode,minutes,rules,onDismiss) else vm.update(existing,name,notes,dose,startDate,endDate,mode,minutes,rules,onDismiss)
    }){Text(if(existing==null)"Créer" else "Enregistrer")}},dismissButton={TextButton(onClick=onDismiss){Text("Annuler")}})
}
