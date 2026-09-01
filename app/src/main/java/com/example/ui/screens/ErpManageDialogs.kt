package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FixedIncome
import com.example.data.PersonalSubAccount
import com.example.ui.DentalViewModel

/**
 * دیالوگ‌های مدیریتی بخش تنظیمات: درآمد‌های ثابت و سرفصل‌های شخصی.
 */

// ==========================================
// مدیریت درآمدهای ثابت
// ==========================================
@Composable
fun ManageFixedIncomesDialog(
    viewModel: DentalViewModel,
    isPersian: Boolean,
    onDismiss: () -> Unit
) {
    val fixedIncomes by viewModel.fixedIncomes.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val currency = viewModel.label("currency")

    fun t(fa: String, en: String) = if (isPersian) fa else en

    var showForm by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var dayStr by remember { mutableStateOf("1") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var incomeToDelete by remember { mutableStateOf<FixedIncome?>(null) }

    val active = fixedIncomes.filter { !it.isDeleted }.sortedByDescending { it.amount }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(t("مدیریت درآمدهای ثابت", "Manage Fixed Incomes"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showForm = !showForm },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(if (showForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showForm) t("بستن فرم", "Close Form") else t("افزودن درآمد ثابت", "Add Fixed Income"), fontWeight = FontWeight.Bold)
                }

                if (showForm) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            OutlinedTextField(
                                value = title, onValueChange = { title = it },
                                label = { Text(t("عنوان (مثلاً اجاره مطب)", "Title (e.g. Clinic Rent)")) },
                                modifier = Modifier.fillMaxWidth(), singleLine = true
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = amountStr, onValueChange = { amountStr = it.filter(Char::isDigit) },
                                    label = { Text("${t("مبلغ", "Amount")} ($currency)") },
                                    modifier = Modifier.weight(1f), singleLine = true
                                )
                                OutlinedTextField(
                                    value = dayStr, onValueChange = { dayStr = it.filter(Char::isDigit).take(2) },
                                    label = { Text(t("روز ماه", "Day")) },
                                    modifier = Modifier.weight(1f), singleLine = true
                                )
                            }
                            Text(t("حساب بانکی واریز (اختیاری):", "Bank account (optional):"), fontSize = 10.sp, color = Color.Gray)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScrollCompat2(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedAccountId == null,
                                    onClick = { selectedAccountId = null },
                                    label = { Text(t("بدون حساب", "None"), fontSize = 10.sp) }
                                )
                                bankAccounts.filter { !it.isDeleted }.forEach { acc ->
                                    FilterChip(
                                        selected = selectedAccountId == acc.id,
                                        onClick = { selectedAccountId = acc.id },
                                        label = { Text(acc.bankName, fontSize = 10.sp) }
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    val amount = amountStr.trim().toLongOrNull() ?: 0L
                                    val day = dayStr.trim().toIntOrNull() ?: 1
                                    if (title.isNotBlank() && amount > 0) {
                                        val accName = bankAccounts.find { it.id == selectedAccountId }?.bankName ?: ""
                                        viewModel.addFixedIncome(
                                            title = title.trim(),
                                            amount = amount,
                                            dayOfMonth = day.coerceIn(1, 31),
                                            bankAccountId = selectedAccountId,
                                            bankAccountName = accName,
                                            subAccountName = if (isPersian) "درآمد ثابت" else "Fixed Income"
                                        )
                                        title = ""; amountStr = ""; dayStr = "1"; selectedAccountId = null
                                        showForm = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = title.isNotBlank() && (amountStr.trim().toLongOrNull() ?: 0L) > 0
                            ) { Text(t("ثبت درآمد ثابت", "Save"), fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f, fill = false)) {
                    if (active.isEmpty()) {
                        item {
                            Text(
                                text = t("هنوز درآمد ثابتی تعریف نشده است.", "No fixed incomes yet."),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    items(active, key = { it.id }) { income ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(income.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        text = t(
                                            "روز ${income.dayOfMonth} هر ماه${if (income.bankAccountName.isNotBlank()) " — ${income.bankAccountName}" else ""}",
                                            "Day ${income.dayOfMonth} of each month"
                                        ),
                                        fontSize = 9.5.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    text = "${formatAmount(income.amount)} $currency",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E7D32)
                                )
                                IconButton(onClick = { incomeToDelete = income }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(t("بستن", "Close")) }
        }
    )

    if (incomeToDelete != null) {
        AlertDialog(
            onDismissRequest = { incomeToDelete = null },
            title = { Text(t("حذف درآمد ثابت", "Delete Fixed Income"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text(t("«${incomeToDelete!!.title}» حذف شود؟", "Delete ${incomeToDelete!!.title}?")) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFixedIncome(incomeToDelete!!)
                        incomeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(t("حذف", "Delete"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { incomeToDelete = null }) { Text(t("انصراف", "Cancel")) }
            }
        )
    }
}

// ==========================================
// مدیریت سرفصل‌های شخصی (زیرحساب‌ها)
// ==========================================
@Composable
fun ManageSubAccountsDialog(
    viewModel: DentalViewModel,
    isPersian: Boolean,
    onDismiss: () -> Unit
) {
    val subAccounts by viewModel.personalSubAccounts.collectAsStateWithLifecycle()

    fun t(fa: String, en: String) = if (isPersian) fa else en

    var showForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("EXPENSE") }
    var parentName by remember { mutableStateOf("") }
    var grandParentName by remember { mutableStateOf("") }
    var subToEdit by remember { mutableStateOf<PersonalSubAccount?>(null) }
    var subToDelete by remember { mutableStateOf<PersonalSubAccount?>(null) }
    // حافظهٔ وضعیتِ باز/بستهٔ هر گره در درخت (کلید = نام سرشاخه/شاخه). با
    // rememberSaveable بین چرخش صفحه و باز/بست دیالوگ حفظ می‌شود.
    val expandedNodes = rememberSaveable(saver = androidx.compose.runtime.saveable.listSaver(
        save = { it.toList() }, restore = { it.toMutableStateList() }
    )) { mutableStateListOf<String>() }
    fun toggleNode(key: String) { if (key in expandedNodes) expandedNodes.remove(key) else expandedNodes.add(key) }

    val active = remember(subAccounts) {
        // Deduplicate by (category|parent|name) so repeated headings never appear in the list,
        // even if leftover duplicates from older builds are still in the local database.
        subAccounts
            .filter { !it.isDeleted }
            .distinctBy { "${it.category.trim()}|${it.parentName.trim()}|${it.name.trim()}" }
            .sortedBy { it.name }
    }
    val availableParents = remember(subAccounts) {
        subAccounts.map { it.parentName.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableGrandParents = remember(subAccounts) {
        subAccounts.map { it.grandParentName.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(t("مدیریت سرفصل‌های شخصی", "Manage Personal Sub-Accounts"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        subToEdit = null
                        name = ""
                        parentName = ""
                        grandParentName = ""
                        showForm = !showForm
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(if (showForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showForm) t("بستن فرم", "Close Form") else t("افزودن سرفصل", "Add Sub-Account"), fontWeight = FontWeight.Bold)
                }

                if (showForm) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text(
                                text = if (subToEdit != null) t("ویرایش سرفصل و لینک به بالا", "Edit Sub-Account & Link to Parent") else t("افزودن سرفصل جدید", "New Sub-Account"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                t("ساختار سه‌سطحی: سرشاخه ▸ شاخه ▸ حساب. فقط «حساب»‌ها در تراکنش‌ها شرکت می‌کنند.",
                                    "Three levels: Root ▸ Branch ▸ Account. Only accounts appear in transactions."),
                                fontSize = 9.5.sp, color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = name, onValueChange = { name = it },
                                label = { Text(t("نام حساب (سطح آخر، مثلاً هزینه خانه)", "Account name (leaf, e.g. Home)")) },
                                modifier = Modifier.fillMaxWidth(), singleLine = true
                            )
                            OutlinedTextField(
                                value = parentName, onValueChange = { parentName = it },
                                label = { Text(t("شاخه (سرفصل والد، اختیاری)", "Branch (parent, optional)")) },
                                placeholder = { Text(t("مثلاً: هزینه‌های عمومی", "e.g. General Expenses")) },
                                modifier = Modifier.fillMaxWidth(), singleLine = true
                            )
                            if (availableParents.isNotEmpty()) {
                                Text(t("انتخاب از شاخه‌های موجود:", "Pick existing branch:"), fontSize = 10.sp, color = Color.Gray)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScrollCompat2(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    availableParents.take(6).forEach { p ->
                                        SuggestionChip(
                                            onClick = { parentName = p },
                                            label = { Text(p, fontSize = 9.5.sp) }
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = grandParentName, onValueChange = { grandParentName = it },
                                label = { Text(t("سرشاخه (سطح ریشه، اختیاری)", "Root (grandparent, optional)")) },
                                placeholder = { Text(t("مثلاً: هزینه‌ها", "e.g. Expenses")) },
                                modifier = Modifier.fillMaxWidth(), singleLine = true
                            )
                            if (availableGrandParents.isNotEmpty()) {
                                Text(t("انتخاب از سرشاخه‌های موجود:", "Pick existing root:"), fontSize = 10.sp, color = Color.Gray)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScrollCompat2(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    availableGrandParents.take(6).forEach { gp ->
                                        SuggestionChip(
                                            onClick = { grandParentName = gp },
                                            label = { Text(gp, fontSize = 9.5.sp) }
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScrollCompat2(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "EXPENSE" to t("هزینه", "Expense"),
                                    "INCOME" to t("درآمد", "Income"),
                                    "DRAW" to t("برداشت", "Draw"),
                                    "ASSET" to t("دارایی", "Asset"),
                                    "PERSONS" to t("اشخاص", "Persons")
                                ).forEach { (key, labelTxt) ->
                                    FilterChip(
                                        selected = category == key,
                                        onClick = { category = key },
                                        label = { Text(labelTxt, fontSize = 10.sp) }
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    if (name.isNotBlank()) {
                                        if (subToEdit == null) {
                                            viewModel.addPersonalSubAccount(name.trim(), category, parentName.trim(), grandParentName = grandParentName.trim())
                                        } else {
                                            viewModel.updatePersonalSubAccount(subToEdit!!.copy(name = name.trim(), category = category, parentName = parentName.trim(), grandParentName = grandParentName.trim()))
                                        }
                                        name = ""
                                        parentName = ""
                                        grandParentName = ""
                                        subToEdit = null
                                        showForm = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = name.isNotBlank()
                            ) {
                                Text(
                                    if (subToEdit != null) t("ذخیره تغییرات و لینک", "Save Changes & Link") else t("ثبت سرفصل", "Save"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Text(
                    text = t(
                        "درخت به‌صورت کرکره‌ای است: سرشاخه‌ها و شاخه‌ها جمع/باز می‌شوند (وضعیت به‌خاطر سپرده می‌شود) و فقط «حساب»‌ها در تراکنش‌ها شرکت می‌کنند. ⭐/ویرایش/حذف فقط برای حساب‌ها.",
                        "Collapsible tree: roots & branches fold/unfold (state is remembered); only accounts participate in transactions. ⭐/edit/delete apply to accounts only."
                    ),
                    fontSize = 9.5.sp,
                    color = Color.Gray
                )

                fun categoryLabel(c: String) = when (c) {
                    "EXPENSE" -> t("هزینه", "Expense"); "INCOME" -> t("درآمد", "Income")
                    "DRAW" -> t("برداشت", "Draw"); "ASSET" -> t("دارایی", "Asset")
                    "DEBT" -> t("بدهی", "Debt"); "CAPITAL" -> t("سرمایه", "Capital")
                    "PERSONS" -> t("اشخاص", "Persons"); else -> c
                }

                // ساخت درختِ سه‌سطحی: سرشاخه (grandParent) ▸ شاخه (parent) ▸ حساب (برگ).
                // گره‌های بدونِ سرشاخه/شاخه زیرِ کلیدِ خالی «(بدون گروه)» می‌آیند.
                val rootKeyless = t("(بدون سرشاخه)", "(No root)")
                val branchKeyless = t("(بدون شاخه)", "(No branch)")
                val tree = remember(active) {
                    active.groupBy { it.grandParentName.trim() }
                        .toSortedMap(compareBy { it.ifBlank { "\uFFFF" } })
                        .mapValues { (_, branchList) ->
                            branchList.groupBy { it.parentName.trim() }
                                .toSortedMap(compareBy { it.ifBlank { "\uFFFF" } })
                        }
                }

                @Composable
                fun LeafRow(sub: PersonalSubAccount, indent: Int) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(start = (indent * 14).dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sub.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(t("دسته: ", "Category: ") + categoryLabel(sub.category), fontSize = 9.sp, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = { viewModel.updatePersonalSubAccount(sub.copy(isQuickSelect = !sub.isQuickSelect)) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        if (sub.isQuickSelect) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = t("اولویت پیشنهادی", "Priority"),
                                        tint = if (sub.isQuickSelect) Color(0xFFF9A825) else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        subToEdit = sub
                                        name = sub.name
                                        category = sub.category
                                        parentName = sub.parentName
                                        grandParentName = sub.grandParentName
                                        showForm = true
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp)) }
                                IconButton(onClick = { subToDelete = sub }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }

                @Composable
                fun NodeHeader(label: String, count: Int, expanded: Boolean, indent: Int, onToggle: () -> Unit) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(start = (indent * 14).dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (indent == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                        onClick = onToggle
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowLeft,
                                contentDescription = null, modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("(${if (isPersian) com.example.util.PersianNumberFormatter.toPersianDigits(count.toString()) else count})", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f, fill = false)) {
                    if (active.isEmpty()) {
                        item {
                            Text(
                                text = t("هنوز سرفصلی تعریف نشده است.", "No sub-accounts yet."),
                                fontSize = 11.sp, color = Color.Gray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    tree.forEach { (grand, branches) ->
                        val grandLabel = grand.ifBlank { rootKeyless }
                        val grandKey = "G:$grandLabel"
                        val grandCount = branches.values.sumOf { it.size }
                        val grandExpanded = grandKey in expandedNodes
                        item(key = grandKey) {
                            NodeHeader(grandLabel, grandCount, grandExpanded, 0) { toggleNode(grandKey) }
                        }
                        if (grandExpanded) {
                            branches.forEach { (branch, leaves) ->
                                val branchLabel = branch.ifBlank { branchKeyless }
                                val branchKey = "$grandKey|B:$branchLabel"
                                val branchExpanded = branchKey in expandedNodes
                                item(key = branchKey) {
                                    NodeHeader(branchLabel, leaves.size, branchExpanded, 1) { toggleNode(branchKey) }
                                }
                                if (branchExpanded) {
                                    items(leaves, key = { "L:${it.id}" }) { sub -> LeafRow(sub, 2) }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(t("بستن", "Close")) }
        }
    )

    if (subToDelete != null) {
        AlertDialog(
            onDismissRequest = { subToDelete = null },
            title = { Text(t("حذف سرفصل", "Delete Sub-Account"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text(t("سرفصل «${subToDelete!!.name}» حذف شود؟", "Delete ${subToDelete!!.name}?")) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePersonalSubAccount(subToDelete!!)
                        subToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(t("حذف", "Delete"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { subToDelete = null }) { Text(t("انصراف", "Cancel")) }
            }
        )
    }
}

/** اسکرول افقی برای ردیف چیپ‌ها (کمک‌تابع). */
@Composable
private fun Modifier.horizontalScrollCompat2(): Modifier = this.horizontalScroll(rememberScrollState())
