package com.iith.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api

// Row alternating colors
private val rowBlue   = Color(0xFFE8F0FE)
private val rowPurple = Color(0xFFF0EBFF)

@Composable
fun AdminAnalyticsScreen() {
    var tab by remember { mutableStateOf("professors") }

    Column(modifier = Modifier.fillMaxSize().background(BGGray)) {
        // Toggle
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White),
        ) {
            ToggleBtn("Professors", tab == "professors", GBlue,   Modifier.weight(1f)) { tab = "professors" }
            ToggleBtn("Students",   tab == "students",   GPurple, Modifier.weight(1f)) { tab = "students" }
        }

        if (tab == "professors") ProfessorsTable()
        else StudentsTable()
    }
}

@Composable
private fun ToggleBtn(label: String, selected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) color else Color.White,
            contentColor   = if (selected) Color.White else Color.Gray
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) { Text(label, fontWeight = FontWeight.SemiBold) }
}

// ── Professors Table ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfessorsTable() {
    var search       by remember { mutableStateOf("") }
    var showFilter   by remember { mutableStateOf(false) }
    var filterType   by remember { mutableStateOf<ProfType?>(null) }
    var filterOp     by remember { mutableStateOf(">=") }
    var filterPct    by remember { mutableStateOf("") }

    val filtered = sampleAdminProfessors.filter { p ->
        val matchSearch = search.isBlank() ||
            p.name.contains(search, ignoreCase = true) ||
            p.code.contains(search, ignoreCase = true)
        val matchType = filterType == null || p.type == filterType
        val matchPct  = filterPct.toDoubleOrNull()?.let { threshold ->
            if (filterOp == ">=") p.avgAttendance >= threshold else p.avgAttendance <= threshold
        } ?: true
        matchSearch && matchType && matchPct
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search + filter bar
        Row(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                placeholder = { Text("Search name or code", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { showFilter = true }) {
                Icon(Icons.Default.FilterList, null, tint = GBlue)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Table header
        TableRow(
            cells = listOf("S.No", "Name", "Type", "Code", "Classes", "Avg %"),
            weights = listOf(0.6f, 2f, 2f, 1f, 1.2f, 1.2f),
            bg = GBlue, textColor = Color.White, bold = true
        )

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            filtered.forEachIndexed { i, p ->
                TableRow(
                    cells = listOf(
                        "${i + 1}",
                        p.name,
                        p.type.label,
                        p.code,
                        "${p.classesTaken}",
                        "${"%.1f".format(p.avgAttendance)}%"
                    ),
                    weights = listOf(0.6f, 2f, 2f, 1f, 1.2f, 1.2f),
                    bg = if (i % 2 == 0) rowBlue else rowPurple,
                    textColor = Color.Black
                )
            }
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No results found", color = Color.Gray)
                }
            }
        }
    }

    if (showFilter) {
        ProfFilterSheet(
            currentType = filterType, currentOp = filterOp, currentPct = filterPct,
            onApply = { t, op, pct -> filterType = t; filterOp = op; filterPct = pct; showFilter = false },
            onDismiss = { showFilter = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfFilterSheet(
    currentType: ProfType?, currentOp: String, currentPct: String,
    onApply: (ProfType?, String, String) -> Unit, onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf(currentType) }
    var op   by remember { mutableStateOf(currentOp) }
    var pct  by remember { mutableStateOf(currentPct) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Filter Professors", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Text("Professor Type", fontSize = 13.sp, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "All", ProfType.ASSISTANT to "Asst.", ProfType.ASSOCIATE to "Assoc.", ProfType.PROFESSOR to "Prof.").forEach { (t, label) ->
                    FilterChipBtn(label, type == t, GBlue) { type = t }
                }
            }

            Text("Avg Attendance", fontSize = 13.sp, color = Color.Gray)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChipBtn("≥", op == ">=", GBlue) { op = ">=" }
                FilterChipBtn("≤", op == "<=", GBlue) { op = "<=" }
                OutlinedTextField(
                    value = pct, onValueChange = { pct = it },
                    placeholder = { Text("e.g. 75", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(10.dp), singleLine = true,
                    suffix = { Text("%") }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { type = null; op = ">="; pct = ""; onApply(null, ">=", "") },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(10.dp)) {
                    Text("Clear")
                }
                Button(onClick = { onApply(type, op, pct) },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GBlue)) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Students Table ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentsTable() {
    var search     by remember { mutableStateOf("") }
    var showFilter by remember { mutableStateOf(false) }
    var filterOp   by remember { mutableStateOf(">=") }
    var filterPct  by remember { mutableStateOf("") }

    val filtered = sampleAdminStudents.filter { s ->
        val matchSearch = search.isBlank() ||
            s.name.contains(search, ignoreCase = true) ||
            s.roll.contains(search, ignoreCase = true)
        val matchPct = filterPct.toDoubleOrNull()?.let { threshold ->
            if (filterOp == ">=") s.avgAttendance >= threshold else s.avgAttendance <= threshold
        } ?: true
        matchSearch && matchPct
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                placeholder = { Text("Search name or roll no.", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(10.dp), singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { showFilter = true }) {
                Icon(Icons.Default.FilterList, null, tint = GPurple)
            }
        }
        Spacer(Modifier.height(8.dp))

        TableRow(
            cells = listOf("S.No", "Name", "Roll No.", "Avg %"),
            weights = listOf(0.6f, 2.5f, 2.5f, 1.4f),
            bg = GPurple, textColor = Color.White, bold = true
        )

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            filtered.forEachIndexed { i, s ->
                TableRow(
                    cells = listOf("${i + 1}", s.name, s.roll, "${"%.1f".format(s.avgAttendance)}%"),
                    weights = listOf(0.6f, 2.5f, 2.5f, 1.4f),
                    bg = if (i % 2 == 0) rowBlue else rowPurple,
                    textColor = Color.Black
                )
            }
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No results found", color = Color.Gray)
                }
            }
        }
    }

    if (showFilter) {
        StudentFilterSheet(
            currentOp = filterOp, currentPct = filterPct,
            onApply = { op, pct -> filterOp = op; filterPct = pct; showFilter = false },
            onDismiss = { showFilter = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentFilterSheet(
    currentOp: String, currentPct: String,
    onApply: (String, String) -> Unit, onDismiss: () -> Unit
) {
    var op  by remember { mutableStateOf(currentOp) }
    var pct by remember { mutableStateOf(currentPct) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Filter Students", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Avg Attendance", fontSize = 13.sp, color = Color.Gray)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChipBtn("≥", op == ">=", GPurple) { op = ">=" }
                FilterChipBtn("≤", op == "<=", GPurple) { op = "<=" }
                OutlinedTextField(
                    value = pct, onValueChange = { pct = it },
                    placeholder = { Text("e.g. 75", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(10.dp), singleLine = true,
                    suffix = { Text("%") }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { op = ">="; pct = ""; onApply(">=", "") },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(10.dp)) {
                    Text("Clear")
                }
                Button(onClick = { onApply(op, pct) },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GPurple)) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Shared table row ──────────────────────────────────────────────────────────
@Composable
fun TableRow(
    cells: List<String>,
    weights: List<Float>,
    bg: Color,
    textColor: Color,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(bg).padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        cells.forEachIndexed { i, cell ->
            Text(
                cell,
                modifier = Modifier.weight(weights[i]),
                fontSize = 12.sp,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                textAlign = if (i == 0) TextAlign.Center else TextAlign.Start,
                maxLines = 2
            )
        }
    }
    Divider(color = Color.White, thickness = 1.dp)
}

// ── Shared filter chip button ─────────────────────────────────────────────────
@Composable
fun FilterChipBtn(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) color else Color.LightGray.copy(alpha = 0.4f),
            contentColor   = if (selected) Color.White else Color.DarkGray
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(36.dp)
    ) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}
