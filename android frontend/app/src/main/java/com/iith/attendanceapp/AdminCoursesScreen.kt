package com.iith.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api

private sealed class AdminCourseNav {
    object List : AdminCourseNav()
    data class Students(val course: ProfCourse) : AdminCourseNav()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCoursesScreen(token: String) {
    var courses     by remember { mutableStateOf<List<ProfCourse>>(emptyList()) }
    var loading     by remember { mutableStateOf(true) }
    var nav         by remember { mutableStateOf<AdminCourseNav>(AdminCourseNav.List) }
    var search      by remember { mutableStateOf("") }
    var filterSlot  by remember { mutableStateOf<String?>(null) }
    var filterRoom  by remember { mutableStateOf("") }
    var showFilter  by remember { mutableStateOf(false) }
    var showAddEdit by remember { mutableStateOf(false) }
    var editCourse  by remember { mutableStateOf<ProfCourse?>(null) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }

    fun reload() {
        loading = true
        // Admin gets all courses — use empty string as professorId to get all
        apiGetProfCourses("", token) { result, err ->
            loading = false
            if (result != null) courses = result else errorMsg = err
        }
    }

    LaunchedEffect(token) { reload() }

    when (val n = nav) {
        is AdminCourseNav.Students -> {
            AdminCourseStudentsScreen(course = n.course, token = token, onBack = { nav = AdminCourseNav.List })
            return
        }
        else -> {}
    }

    val filtered = courses.filter { c ->
        val matchSearch = search.isBlank() || c.name.contains(search, true) || c.id.contains(search, true)
        val matchSlot   = filterSlot == null || c.slot == filterSlot
        val matchRoom   = filterRoom.isBlank() || c.venue.contains(filterRoom, true)
        matchSearch && matchSlot && matchRoom
    }

    Column(modifier = Modifier.fillMaxSize().background(BGGray)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = search, onValueChange = { search = it },
                placeholder = { Text("Search course name or code", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(10.dp), singleLine = true)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { showFilter = true }) { Icon(Icons.Default.FilterList, null, tint = GBlue) }
        }

        Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { editCourse = null; showAddEdit = true },
                modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GBlue)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Course", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(8.dp))

        if (errorMsg != null) Text("Error: $errorMsg", color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GBlue) }
        } else {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.height(4.dp))
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No courses found", color = Color.Gray)
                    }
                }
                filtered.forEachIndexed { i, course ->
                    AdminCourseCard(
                        course  = course, index = i,
                        onClick = { nav = AdminCourseNav.Students(course) },
                        onEdit  = { editCourse = course; showAddEdit = true },
                        onDelete = {
                            apiDeleteCourse(token, course.id) { ok, _ -> if (ok) reload() }
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showFilter) {
        AdminCourseFilterSheet(currentSlot = filterSlot, currentRoom = filterRoom,
            onApply = { slot, room -> filterSlot = slot; filterRoom = room; showFilter = false },
            onDismiss = { showFilter = false })
    }

    if (showAddEdit) {
        AdminAddEditCourseSheet(
            existing  = editCourse,
            onSave    = { name, slot, venue, instructors ->
                if (editCourse == null) {
                    apiCreateCourse(token, name, slot, venue, instructors) { ok, _ -> if (ok) reload() }
                } else {
                    apiUpdateCourse(token, editCourse!!.id, name, slot, venue) { ok, _ -> if (ok) reload() }
                }
            },
            onDismiss = { showAddEdit = false; editCourse = null }
        )
    }
}

@Composable
private fun AdminCourseCard(course: ProfCourse, index: Int, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val color = courseBannerColors[index % courseBannerColors.size]
    Column(modifier = Modifier.shadow(3.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))
        .background(Color.White).clickable(onClick = onClick)) {
        Box(modifier = Modifier.fillMaxWidth().height(48.dp).background(color), contentAlignment = Alignment.CenterStart) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(course.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                InfoRow("Slot",  course.slot)
                InfoRow("Room",  course.venue)
                InfoRow("Schedules", "${course.schedules.size}")
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, null, tint = GBlue, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text("$label: ", fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminAddEditCourseSheet(
    existing: ProfCourse?,
    onSave: (name: String, slot: String, venue: String, instructors: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var name         by remember { mutableStateOf(existing?.name ?: "") }
    var slot         by remember { mutableStateOf(existing?.slot ?: "A") }
    var venue        by remember { mutableStateOf(existing?.venue ?: "") }
    var instructors  by remember { mutableStateOf("") }
    var slotExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (existing == null) "Add Course" else "Edit Course", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            AdminTextField("Course Name", name) { name = it }
            AdminTextField("Room / Venue", venue) { venue = it }
            AdminTextField("Instructor IDs (comma separated)", instructors) { instructors = it }
            ExposedDropdownMenuBox(expanded = slotExpanded, onExpandedChange = { slotExpanded = it }) {
                OutlinedTextField(value = slot, onValueChange = {}, readOnly = true, label = { Text("Slot") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(slotExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(10.dp))
                ExposedDropdownMenu(expanded = slotExpanded, onDismissRequest = { slotExpanded = false }) {
                    allSlots.forEach { s -> DropdownMenuItem(text = { Text(s) }, onClick = { slot = s; slotExpanded = false }) }
                }
            }
            Button(onClick = {
                if (name.isNotBlank()) {
                    onSave(name.trim(), slot, venue.trim(), instructors.split(",").map { it.trim() }.filter { it.isNotBlank() })
                    onDismiss()
                }
            }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GBlue)) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminCourseFilterSheet(currentSlot: String?, currentRoom: String,
    onApply: (String?, String) -> Unit, onDismiss: () -> Unit) {
    var slot by remember { mutableStateOf(currentSlot) }
    var room by remember { mutableStateOf(currentRoom) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Filter Courses", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Slot", fontSize = 13.sp, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                (listOf(null) + allSlots).forEach { s -> FilterChipBtn(s ?: "All", slot == s, GBlue) { slot = s } }
            }
            AdminTextField("Room / Venue", room) { room = it }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { slot = null; room = ""; onApply(null, "") },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(10.dp)) { Text("Clear") }
                Button(onClick = { onApply(slot, room) }, modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = GBlue)) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Course students screen ────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCourseStudentsScreen(course: ProfCourse, token: String, onBack: () -> Unit) {
    var students    by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var loading     by remember { mutableStateOf(true) }
    var search      by remember { mutableStateOf("") }
    var showAdd     by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }

    fun reload() {
        loading = true
        apiGetCourseStudents(token, course.id) { result, err ->
            loading = false
            if (result != null) students = result else errorMsg = err
        }
    }

    LaunchedEffect(course.id) { reload() }

    val filtered = students.filter { s ->
        search.isBlank() || s.name.contains(search, true) || s.email.contains(search, true) || s.id.contains(search, true)
    }

    Column(modifier = Modifier.fillMaxSize().background(BGGray)) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) { Text("← Back", color = GBlue) }
        Text(course.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Text("${students.size} students", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(value = search, onValueChange = { search = it },
            placeholder = { Text("Search name, email or ID", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
            shape = RoundedCornerShape(10.dp), singleLine = true)
        Spacer(Modifier.height(8.dp))

        if (errorMsg != null) Text("Error: $errorMsg", color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))

        TableRow(listOf("S.No", "Name", "Email / ID"), listOf(0.6f, 2f, 3f), GBlue, Color.White, bold = true)

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GBlue) }
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                filtered.forEachIndexed { i, student ->
                    Row(modifier = Modifier.fillMaxWidth()
                        .background(if (i % 2 == 0) Color(0xFFE8F0FE) else Color(0xFFF0EBFF))
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", modifier = Modifier.weight(0.6f), fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text(student.name, modifier = Modifier.weight(2f), fontSize = 12.sp)
                        Text(student.email.ifBlank { student.id }, modifier = Modifier.weight(3f), fontSize = 11.sp, color = Color.Gray)
                    }
                    Divider(color = Color.White, thickness = 1.dp)
                }
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No students found", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true)
}
