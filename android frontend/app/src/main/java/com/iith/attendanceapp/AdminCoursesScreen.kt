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

// ── Screen state ──────────────────────────────────────────────────────────────
private sealed class AdminCourseScreen {
    object List : AdminCourseScreen()
    data class Students(val course: AdminCourse) : AdminCourseScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCoursesScreen() {
    var courses     by remember { mutableStateOf(sampleAdminCourses.toMutableList()) }
    var screen      by remember { mutableStateOf<AdminCourseScreen>(AdminCourseScreen.List) }
    var search      by remember { mutableStateOf("") }
    var showFilter  by remember { mutableStateOf(false) }
    var filterSlot  by remember { mutableStateOf<String?>(null) }
    var filterRoom  by remember { mutableStateOf("") }
    var showAddEdit by remember { mutableStateOf(false) }
    var editCourse  by remember { mutableStateOf<AdminCourse?>(null) }

    when (val s = screen) {
        is AdminCourseScreen.Students -> {
            CourseStudentsScreen(
                course = s.course,
                onBack = { screen = AdminCourseScreen.List }
            )
            return
        }
        else -> {}
    }

    val filtered = courses.filter { c ->
        val matchSearch = search.isBlank() ||
            c.name.contains(search, ignoreCase = true) ||
            c.code.contains(search, ignoreCase = true) ||
            c.professors.any { it.contains(search, ignoreCase = true) }
        val matchSlot = filterSlot == null || c.slot == filterSlot
        val matchRoom = filterRoom.isBlank() || c.room.contains(filterRoom, ignoreCase = true)
        matchSearch && matchSlot && matchRoom
    }

    Column(modifier = Modifier.fillMaxSize().background(BGGray)) {
        // Search bar
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                placeholder = { Text("Search course, code, professor", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(10.dp), singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { showFilter = true }) {
                Icon(Icons.Default.FilterList, null, tint = GBlue)
            }
        }

        // Add buttons row
        Row(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { editCourse = null; showAddEdit = true },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GBlue)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Course", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = { /* TODO: CSV file picker */ },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Upload CSV", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No courses found", color = Color.Gray)
                }
            }
            filtered.forEach { course ->
                AdminCourseCard(
                    course = course,
                    onClick = { screen = AdminCourseScreen.Students(course) },
                    onEdit = { editCourse = course; showAddEdit = true },
                    onDelete = { courses = courses.filter { it.id != course.id }.toMutableList() },
                    onArchive = {
                        courses = courses.map {
                            if (it.id == course.id) it.copy(archived = !it.archived) else it
                        }.toMutableList()
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showFilter) {
        CourseFilterSheet(
            currentSlot = filterSlot, currentRoom = filterRoom,
            onApply = { slot, room -> filterSlot = slot; filterRoom = room; showFilter = false },
            onDismiss = { showFilter = false }
        )
    }

    if (showAddEdit) {
        AddEditCourseSheet(
            existing = editCourse,
            onSave = { c ->
                courses = if (editCourse == null) {
                    (courses + c).toMutableList()
                } else {
                    courses.map { if (it.id == c.id) c else it }.toMutableList()
                }
            },
            onDismiss = { showAddEdit = false; editCourse = null },
            nextId = (courses.maxOfOrNull { it.id } ?: 0) + 1
        )
    }
}

// ── Course card ───────────────────────────────────────────────────────────────
@Composable
private fun AdminCourseCard(
    course: AdminCourse,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit
) {
    val alpha = if (course.archived) 0.5f else 1f
    Column(
        modifier = Modifier
            .shadow(3.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = alpha))
            .clickable(onClick = onClick)
    ) {
        // Colored header
        Box(
            modifier = Modifier.fillMaxWidth().height(48.dp).background(GBlue.copy(alpha = alpha)),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(course.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                if (course.archived) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.25f)).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) { Text("Archived", fontSize = 10.sp, color = Color.White) }
                }
            }
        }
        // Body
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                InfoChip("Code", course.code)
                InfoChip("Slot", course.slot)
                InfoChip("Room", course.room)
                InfoChip("Prof", course.professors.joinToString(", "))
                InfoChip("Students", "${course.students.size}")
            }
            Column {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, null, tint = GBlue, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onArchive, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (course.archived) Icons.Default.Unarchive else Icons.Default.Archive,
                        null, tint = Color.Gray, modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Row {
        Text("$label: ", fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
    }
}

// ── Add / Edit course sheet ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditCourseSheet(
    existing: AdminCourse?,
    onSave: (AdminCourse) -> Unit,
    onDismiss: () -> Unit,
    nextId: Int
) {
    var name  by remember { mutableStateOf(existing?.name ?: "") }
    var code  by remember { mutableStateOf(existing?.code ?: "") }
    var profs by remember { mutableStateOf(existing?.professors?.joinToString(", ") ?: "") }
    var slot  by remember { mutableStateOf(existing?.slot ?: "A") }
    var room  by remember { mutableStateOf(existing?.room ?: "") }
    var slotExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (existing == null) "Add Course" else "Edit Course",
                fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            AdminTextField("Course Name", name) { name = it }
            AdminTextField("Course Code", code) { code = it }
            AdminTextField("Professor(s) — comma separated", profs) { profs = it }

            // Slot dropdown
            ExposedDropdownMenuBox(expanded = slotExpanded, onExpandedChange = { slotExpanded = it }) {
                OutlinedTextField(
                    value = slot, onValueChange = {},
                    readOnly = true, label = { Text("Slot") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(slotExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(expanded = slotExpanded, onDismissRequest = { slotExpanded = false }) {
                    allSlots.forEach { s ->
                        DropdownMenuItem(text = { Text(s) }, onClick = { slot = s; slotExpanded = false })
                    }
                }
            }

            AdminTextField("Room Number", room) { room = it }

            Button(
                onClick = {
                    if (name.isNotBlank() && code.isNotBlank()) {
                        onSave(
                            AdminCourse(
                                id = existing?.id ?: nextId,
                                name = name.trim(), code = code.trim(),
                                professors = profs.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                slot = slot, room = room.trim(),
                                archived = existing?.archived ?: false,
                                students = existing?.students ?: mutableListOf()
                            )
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GBlue)
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        }
    }
}

// ── Course filter sheet ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseFilterSheet(
    currentSlot: String?, currentRoom: String,
    onApply: (String?, String) -> Unit, onDismiss: () -> Unit
) {
    var slot by remember { mutableStateOf(currentSlot) }
    var room by remember { mutableStateOf(currentRoom) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Filter Courses", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Text("Slot", fontSize = 13.sp, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                (listOf(null) + allSlots).forEach { s ->
                    FilterChipBtn(s ?: "All", slot == s, GBlue) { slot = s }
                }
            }

            AdminTextField("Room Number", room) { room = it }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { slot = null; room = ""; onApply(null, "") },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(10.dp)) {
                    Text("Clear")
                }
                Button(onClick = { onApply(slot, room) },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GBlue)) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Course students screen ────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseStudentsScreen(course: AdminCourse, onBack: () -> Unit) {
    var students    by remember { mutableStateOf(course.students.toMutableList()) }
    var search      by remember { mutableStateOf("") }
    var showAddEdit by remember { mutableStateOf(false) }
    var editStudent by remember { mutableStateOf<AdminStudent?>(null) }

    val filtered = students.filter { s ->
        search.isBlank() ||
            s.name.contains(search, ignoreCase = true) ||
            s.roll.contains(search, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().background(BGGray)) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Text("← Back", color = GBlue)
        }
        Text(course.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Text("${course.code}  •  ${students.size} students", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(10.dp))

        // Search
        OutlinedTextField(
            value = search, onValueChange = { search = it },
            placeholder = { Text("Search name or roll no.", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
            shape = RoundedCornerShape(10.dp), singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        // Add buttons
        Row(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { editStudent = null; showAddEdit = true },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GBlue)
            ) {
                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Student", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = { /* TODO: CSV file picker */ },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Upload CSV", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Table header
        TableRow(
            cells = listOf("S.No", "Name", "Roll No.", ""),
            weights = listOf(0.6f, 2.5f, 2.5f, 1f),
            bg = GBlue, textColor = Color.White, bold = true
        )

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            filtered.forEachIndexed { i, student ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (i % 2 == 0) Color(0xFFE8F0FE) else Color(0xFFF0EBFF))
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${i + 1}", modifier = Modifier.weight(0.6f), fontSize = 12.sp, textAlign = TextAlign.Center)
                    Text(student.name, modifier = Modifier.weight(2.5f), fontSize = 12.sp)
                    Text(student.roll, modifier = Modifier.weight(2.5f), fontSize = 12.sp)
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { editStudent = student; showAddEdit = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, null, tint = GBlue, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { students = students.filter { it.id != student.id }.toMutableList() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        }
                    }
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

    if (showAddEdit) {
        AddEditStudentSheet(
            existing = editStudent,
            onSave = { s ->
                students = if (editStudent == null) {
                    (students + s).toMutableList()
                } else {
                    students.map { if (it.id == s.id) s else it }.toMutableList()
                }
                course.students.clear()
                course.students.addAll(students)
            },
            onDismiss = { showAddEdit = false; editStudent = null },
            nextId = (students.maxOfOrNull { it.id } ?: 0) + 1
        )
    }
}

// ── Add / Edit student sheet ──────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditStudentSheet(
    existing: AdminStudent?,
    onSave: (AdminStudent) -> Unit,
    onDismiss: () -> Unit,
    nextId: Int
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var roll by remember { mutableStateOf(existing?.roll ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (existing == null) "Add Student" else "Edit Student", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            AdminTextField("Student Name", name) { name = it }
            AdminTextField("Roll Number", roll) { roll = it }
            Button(
                onClick = {
                    if (name.isNotBlank() && roll.isNotBlank()) {
                        onSave(AdminStudent(existing?.id ?: nextId, name.trim(), roll.trim()))
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GBlue)
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        }
    }
}

// ── Shared text field ─────────────────────────────────────────────────────────
@Composable
fun AdminTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        singleLine = true
    )
}
