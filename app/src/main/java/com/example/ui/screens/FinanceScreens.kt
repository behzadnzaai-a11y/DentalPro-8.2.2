package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.DentalViewModel

/**
 * صفحه‌های ماژول «بیشتر»: هزینه‌های مطب، تراکنش‌های شخصی، تعمیر و نگهداری تجهیزات.
 */

// هزینه‌های مطب — چیدمان: (۱) فرمِ «ثبت هزینه جاری مطب»، (۲) «پرداخت هزینه‌ها» زیرِ
// فرم و بالای صفحه؛ انتخابِ طرف‌حساب از بینِ طرف‌حساب‌های موجود و پرداختِ کل/جزئی.
/**
 * هابِ «هزینه‌های مطب»: چهار کارتِ اصلی + کارتِ باریکِ «سایر». هر کارت جمع/تعدادِ
 * هزینه‌های همان گروه را نشان می‌دهد و با لمس، صفحهٔ drill-down همان گروه باز می‌شود.
 * صفحهٔ هر گروه، تراکنش‌ها/فاکتورها را به‌تفکیکِ زیرشاخه (طرف‌حساب) نمایش می‌دهد و
 * از همان‌جا هم می‌توان هزینه/فاکتور ثبت کرد.
 */
@Composable
fun ExpensesScreen(viewModel: DentalViewModel) {
    var selectedGroupKey by rememberSaveable { mutableStateOf<String?>(null) }
    val group = selectedGroupKey?.let { ExpenseGroup.fromKey(it) }
    if (group == null) {
        ExpensesHubScreen(viewModel, onOpenGroup = { selectedGroupKey = it.key })
    } else {
        ExpenseGroupDetailScreen(viewModel, group, onBack = { selectedGroupKey = null })
    }
}

@Composable
private fun ExpensesHubScreen(
    viewModel: DentalViewModel,
    onOpenGroup: (ExpenseGroup) -> Unit
) {
    val isPersian = viewModel.isPersian.collectAsStateWithLifecycle().value
    val expenses by viewModel.clinicExpenses.collectAsStateWithLifecycle()
    val expensePayments by viewModel.allExpensePayments.collectAsStateWithLifecycle()
    val currency = viewModel.label("currency")
    fun t(fa: String, en: String) = if (isPersian) fa else en

    val active = expenses.filter { !it.isDeleted }
    // جمع و تعدادِ هر گروه (با inferFrom تا هزینه‌های قدیمیِ بدونِ گروه هم درست دسته شوند).
    fun groupExpenses(g: ExpenseGroup) = active.filter { ExpenseGroup.inferFrom(it) == g }
    fun groupTotal(g: ExpenseGroup): Long = groupExpenses(g).sumOf { it.amount }
    fun groupPaid(g: ExpenseGroup): Long {
        val ids = groupExpenses(g).map { it.id }.toSet()
        return expensePayments.filter { !it.isDeleted && it.clinicExpenseId in ids }.sumOf { it.amount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = t("هزینه‌های مطب", "Clinic Expenses"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = t(
                "یک بخش را انتخاب کنید تا تراکنش‌ها و فاکتورهای آن به تفکیک طرف‌حساب نمایش داده شود و بتوانید هزینه ثبت کنید.",
                "Pick a section to see its transactions and invoices by counterparty and register expenses."
            ),
            fontSize = 11.sp,
            color = Color.Gray
        )

        // چهار کارتِ اصلی — دو ردیفِ دوتایی
        ExpenseGroup.primary.chunked(2).forEach { rowGroups ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowGroups.forEach { g ->
                    ExpenseGroupCard(
                        group = g,
                        total = groupTotal(g),
                        paid = groupPaid(g),
                        count = groupExpenses(g).size,
                        currency = currency,
                        isPersian = isPersian,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenGroup(g) }
                    )
                }
                if (rowGroups.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        // کارتِ باریکِ «سایر هزینه‌ها» + امکان افزودن سرفصل/هزینهٔ جدید
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            onClick = { onOpenGroup(ExpenseGroup.OTHER) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Column {
                        Text(ExpenseGroup.OTHER.title(isPersian), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            t("افزودن سرفصل و ثبت سایر هزینه‌های مطب", "Add heading & register other expenses"),
                            fontSize = 9.5.sp, color = Color.Gray
                        )
                    }
                }
                Text(
                    "${formatAmount(groupTotal(ExpenseGroup.OTHER))} $currency",
                    fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.error
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ExpenseGroupCard(
    group: ExpenseGroup,
    total: Long,
    paid: Long,
    count: Int,
    currency: String,
    isPersian: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    fun t(fa: String, en: String) = if (isPersian) fa else en
    val remaining = total - paid
    val icon = when (group) {
        ExpenseGroup.LAB -> Icons.Default.Science
        ExpenseGroup.CONSUMABLES -> Icons.Default.MedicalServices
        ExpenseGroup.EQUIPMENT -> Icons.Default.Handyman
        ExpenseGroup.RUNNING -> Icons.Default.Receipt
        ExpenseGroup.OTHER -> Icons.Default.MoreHoriz
    }
    Card(
        modifier = modifier.height(132.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                Text(
                    group.title(isPersian),
                    fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${formatAmount(total)} $currency", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error, maxLines = 1)
                Text(
                    text = if (remaining > 0L) "${t("مانده:", "Due:")} ${formatAmount(remaining)} $currency"
                    else t("تسویه ✓", "Settled ✓"),
                    fontSize = 9.5.sp,
                    color = if (remaining > 0L) Color(0xFFC2410C) else Color(0xFF2E7D32)
                )
                Text("${t("تعداد:", "Items:")} ${if (isPersian) com.example.util.PersianNumberFormatter.toPersianDigits(count.toString()) else count}", fontSize = 9.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun ExpenseGroupDetailScreen(
    viewModel: DentalViewModel,
    expenseGroup: ExpenseGroup,
    onBack: () -> Unit
) {
    val isPersian = viewModel.isPersian.collectAsStateWithLifecycle().value
    val allExpensesRaw by viewModel.clinicExpenses.collectAsStateWithLifecycle()
    // فقط هزینه‌های همین گروه (با inferFrom تا داده‌های قدیمی هم دیده شوند).
    val expenses = allExpensesRaw.filter { ExpenseGroup.inferFrom(it) == expenseGroup }
    val expensePayments by viewModel.allExpensePayments.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val allPayments by viewModel.allPayments.collectAsStateWithLifecycle()
    val currency = viewModel.label("currency")

    fun t(fa: String, en: String) = if (isPersian) fa else en

    // فرم ثبت هزینه جاری
    var showAddForm by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    // نام طرف‌حساب جدا از عنوان ثبت می‌شود تا در همه‌جا «عنوان (نام)» ساخته شود.
    var partyName by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("CARD_TRANSFER") }

    // پرداخت هزینه‌ها
    val activeExpenses = expenses.filter { !it.isDeleted }.sortedByDescending { it.date }
    // طرف‌حساب‌ها با عنوانِ ترکیبی «عنوان (نام)» گروه‌بندی می‌شوند تا دو
    // لابراتوارِ مختلف زیر یک عنوانِ «لابراتوار» با هم قاطی نشوند.
    val counterparties = remember(activeExpenses) {
        activeExpenses.map { com.example.data.partyLabelOf(it) }
            .filter { it.isNotBlank() }.distinct().sorted()
    }
    var selectedCounterparty by remember { mutableStateOf<String?>(null) }
    var expenseToPay by remember { mutableStateOf<ClinicExpense?>(null) }
    // تغییر وضعیت چک‌های هزینه — با دیالوگ تأیید (نه با یک کلیکِ بی‌بازگشت)
    var expenseChequeToClear by remember { mutableStateOf<com.example.data.Payment?>(null) }
    var expenseChequeToBounce by remember { mutableStateOf<com.example.data.Payment?>(null) }
    var expenseToEdit by remember { mutableStateOf<ClinicExpense?>(null) }
    var expenseToDelete by remember { mutableStateOf<ClinicExpense?>(null) }

    // مانده‌ی هر هزینه = مبلغ هزینه − جمع پرداخت‌های ثبت‌شده برای آن.
    // علامت‌دار است: منفی یعنی بیش از مبلغ هزینه پرداخت شده (بستانکاری).
    fun remainingOf(expense: ClinicExpense): Long {
        val paid = expensePayments.filter { !it.isDeleted && it.clinicExpenseId == expense.id }.sumOf { it.amount }
        return expense.amount - paid
    }

    // رفع باگ: این صفحه یک Column ساده بود و با زیاد شدن محتوا اسکرول نمی‌شد؛
    // بخش‌های پایین صفحه اصلاً در دسترس نبودند. حالا کل صفحه verticalScroll دارد.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = onBack, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("بازگشت", "Back"))
                }
                Spacer(Modifier.width(2.dp))
                Text(
                    text = expenseGroup.title(isPersian),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = { showAddForm = !showAddForm },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(if (showAddForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (showAddForm) t("بستن", "Close") else t("ثبت هزینه", "Add"), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        if (showAddForm) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(t("ثبت هزینه جاری مطب", "New Clinic Expense"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = category, onValueChange = { category = it },
                        label = { Text(t("عنوان طرف‌حساب (مثلاً لابراتوار، فروشگاه، فنی مهندسی)", "Party title (e.g. Lab, Store)")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = partyName, onValueChange = { partyName = it },
                        label = { Text(t("نام طرف‌حساب (مثلاً نیازی، حاتمی، وحیدی)", "Party name (e.g. Niazi)")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    if (category.isNotBlank() || partyName.isNotBlank()) {
                        Text(
                            text = t(
                                "در گزارش‌ها و صورتحساب چنین نوشته می‌شود: ${com.example.data.partyDisplayLabel(category, partyName)}",
                                "Shown everywhere as: ${com.example.data.partyDisplayLabel(category, partyName)}"
                            ),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedTextField(
                        value = amountStr, onValueChange = { amountStr = it.filter(Char::isDigit) },
                        label = { Text("${t("مبلغ", "Amount")} ($currency)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    RealTimeAmountHelper(amountStr, isPersian)
                    OutlinedTextField(
                        value = desc, onValueChange = { desc = it },
                        label = { Text(t("توضیحات", "Description")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "CASH" to t("نقدی", "Cash"),
                            "CARD_TRANSFER" to t("کارت به کارت", "Card Transfer"),
                            "POS" to t("پوز", "POS"),
                            "CHEQUE" to t("چک", "Cheque")
                        ).forEach { (key, labelTxt) ->
                            FilterChip(
                                selected = method == key,
                                onClick = { method = key },
                                label = { Text(labelTxt, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val amount = amountStr.trim().toLongOrNull() ?: 0L
                            if (category.isNotBlank() && amount > 0) {
                                viewModel.addExpense(
                                    category = category.trim(),
                                    amount = amount,
                                    desc = desc.trim(),
                                    paymentMethod = method,
                                    subAccountName = partyName.trim(),
                                    expenseGroup = expenseGroup.key
                                )
                                category = ""; amountStr = ""; desc = ""; partyName = ""
                                showAddForm = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = category.isNotBlank() && (amountStr.trim().toLongOrNull() ?: 0L) > 0
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(t("ثبت هزینه", "Save Expense"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ——— «پرداخت هزینه ها» — بالای صفحه، زیر فرم ثبت (درخواست کاربر) ———
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Payment, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                    Text(t("پرداخت هزینه ها", "Pay Expenses"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2E7D32))
                }
                Text(
                    text = t(
                        "طرف‌حساب را از بین طرف‌حساب‌های هزینه‌های ثبت‌شده انتخاب کنید:",
                        "Pick the counterparty among existing expense counterparties:"
                    ),
                    fontSize = 10.5.sp,
                    color = Color.Gray
                )
                if (counterparties.isEmpty()) {
                    Text(
                        text = t("هنوز هزینه‌ای ثبت نشده است.", "No expenses registered yet."),
                        fontSize = 10.5.sp,
                        color = Color.Gray
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScrollCompat(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        counterparties.forEach { cp ->
                            FilterChip(
                                selected = selectedCounterparty == cp,
                                onClick = { selectedCounterparty = if (selectedCounterparty == cp) null else cp },
                                label = { Text(cp, fontSize = 10.sp, maxLines = 1) }
                            )
                        }
                    }
                }
                selectedCounterparty?.let { cp ->
                    val partyExpenses = activeExpenses.filter { com.example.data.partyLabelOf(it) == cp }
                    val partyExpenseIds = partyExpenses.map { it.id }.toSet()
                    val partyPayments = expensePayments.filter {
                        !it.isDeleted && it.clinicExpenseId in partyExpenseIds
                    }

                    // سرصفحهٔ صورتحساب — نام طرف‌حساب کنار عنوان، دقیقاً مثل
                    // «صورتحساب (لابراتوار نیازی)». مانده هم علامت‌دار است تا
                    // اضافه‌پرداخت به‌صورت «بستانکار» دیده شود.
                    val partyTotalDebt = partyExpenses.sumOf { it.amount }
                    val partyTotalPaid = partyPayments.sumOf { it.amount }
                    val partyLedger = com.example.data.PartyLedger(partyTotalDebt, partyTotalPaid)
                    val partyBalanceColor = when {
                        partyLedger.isPartyDebtor -> Color(0xFF6A1B9A)
                        partyLedger.remaining > 0L -> Color(0xFFC2410C)
                        else -> Color(0xFF2E7D32)
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = partyBalanceColor.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, partyBalanceColor.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isPersian) "صورتحساب ($cp)" else "Statement ($cp)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${t("جمع فاکتورها:", "Invoices:")} ${formatAmount(partyTotalDebt)} $currency" +
                                    "  |  ${t("پرداختی:", "Paid:")} ${formatAmount(partyTotalPaid)} $currency",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = when {
                                    partyLedger.isPartyDebtor ->
                                        if (isPersian) "$cp به ما بدهکار است: ${formatAmount(partyLedger.partyDebtAmount)} $currency"
                                        else "$cp owes us: ${formatAmount(partyLedger.partyDebtAmount)} $currency"
                                    partyLedger.remaining > 0L ->
                                        "${t("مانده بدهی:", "Outstanding:")} ${formatAmount(partyLedger.debtAmount)} $currency"
                                    else -> t("حساب تسویه است ✓", "Settled ✓")
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = partyBalanceColor
                            )
                            if (partyLedger.isPartyDebtor) {
                                Text(
                                    text = t(
                                        "پرداختی بیش از مبلغ فاکتورهاست؛ این مبلغ را از این طرف‌حساب طلب داریم.",
                                        "Payments exceed the invoiced amount; this sum is receivable from the party."
                                    ),
                                    fontSize = 9.5.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // ارسال صورت‌حساب و فاکتور این طرف‌حساب — با همان مولد و ابعاد
                    // استاندارد اسناد برنامه (VendorLedger / LabInvoice).
                    ExpensePartySendActionsRow(
                        partyName = cp,
                        expenses = partyExpenses,
                        payments = partyPayments,
                        viewModel = viewModel,
                        isPersian = isPersian
                    )

                    val rows = partyExpenses.take(10)
                    if (rows.isEmpty()) {
                        Text(t("موردی برای این طرف‌حساب نیست.", "No items for this counterparty."), fontSize = 10.5.sp, color = Color.Gray)
                    }
                    rows.forEach { expense ->
                        val remaining = remainingOf(expense)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                remaining < 0L -> Color(0xFF6A1B9A).copy(alpha = 0.10f)
                                remaining == 0L -> Color(0xFF2E7D32).copy(alpha = 0.08f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            onClick = { expenseToPay = expense }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = expense.description.ifBlank { t("بدون توضیح", "No description") },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(viewModel.formatDate(expense.date), fontSize = 9.sp, color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${formatAmount(expense.amount)} $currency", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = when {
                                            remaining < 0L -> "${t("بدهکار به ما", "Owes us")}: ${formatAmount(-remaining)} $currency"
                                            remaining == 0L -> t("تسویه‌شده ✓", "Settled ✓")
                                            else -> "${t("مانده", "Remaining")}: ${formatAmount(remaining)} $currency"
                                        },
                                        fontSize = 9.sp,
                                        color = when {
                                            remaining < 0L -> Color(0xFF6A1B9A)
                                            remaining == 0L -> Color(0xFF2E7D32)
                                            else -> Color(0xFFC2410C)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // چک‌هایی که به هزینه وصل‌اند — تغییر وضعیت همین‌جا روی حسابداری هزینه اعمال می‌شود
        val linkedChequeNumbers = remember(expenses, expensePayments) {
            (expenses.mapNotNull { it.chequeNumber?.takeIf { n -> n.isNotBlank() } } +
                expensePayments.mapNotNull { it.chequeNumber?.takeIf { n -> n.isNotBlank() } }).toSet()
        }
        val expenseCheques = remember(allPayments, linkedChequeNumbers) {
            allPayments.filter { !it.isDeleted && it.type == "CHEQUE" && (
                (!it.chequeNumber.isNullOrBlank() && it.chequeNumber in linkedChequeNumbers) ||
                    // فقط چکِ انتقال‌یافته «به هزینهٔ مطب» اینجا دیده شود؛ چکی که به شخص/
                    // حساب اشخاص اندورس شده (transferChildrenExpenseId یا فقط transferTransactionId)
                    // نباید در هزینه‌های مطب ظاهر شود — رفعِ باگِ ورودِ اشتباهِ چک اشخاص.
                    (it.chequeStatus == "TRANSFERRED" && it.transferExpensePaymentId != null)
                )
            }
        }
        if (expenseCheques.isNotEmpty()) {
            Text(t("چک‌های هزینه‌ها — تغییر وضعیت", "Expense cheques — status"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            expenseCheques.take(12).forEach { cheque ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${cheque.chequeBank.orEmpty().ifBlank { t("چک", "Cheque") }} ${cheque.chequeNumber.orEmpty()} — ${formatAmount(cheque.amount)} $currency",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = viewModel.chequeStatusLabel(cheque, isPersian),
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        // تغییر وضعیت با تأیید انجام می‌شود؛ ضمناً چکِ انتقال‌یافته
                        // را نمی‌شود از اینجا «وصول» کرد — آن چک دیگر در اختیار مطب نیست
                        // و وصولش فقط از راه لغو انتقال معنا دارد.
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (cheque.chequeStatus == "PENDING") {
                                FilterChip(
                                    selected = false,
                                    onClick = { expenseChequeToClear = cheque },
                                    label = { Text(t("وصول", "Clear"), fontSize = 9.sp) }
                                )
                            }
                            if (cheque.chequeStatus != "BOUNCED") {
                                FilterChip(
                                    selected = false,
                                    onClick = { expenseChequeToBounce = cheque },
                                    label = { Text(t("برگشت", "Bounce"), fontSize = 9.sp) }
                                )
                            }
                            FilterChip(
                                selected = cheque.chequeStatus == "CLEARED",
                                enabled = false,
                                onClick = {},
                                label = { Text(t("وصول‌شده", "Cleared"), fontSize = 9.sp) }
                            )
                        }
                    }
                }
            }
        }

        // ——— لیست همه‌ی هزینه‌ها ———
        Text(
            text = t("همه‌ی هزینه‌ها (${activeExpenses.size})", "All Expenses (${activeExpenses.size})"),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        // نکته: داخل یک Column اسکرول‌شونده نمی‌توان LazyColumn گذاشت (ارتفاع
        // بی‌نهایت و کرش). چون خودِ صفحه اسکرول دارد، ردیف‌ها مستقیم رندر می‌شوند.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activeExpenses.forEach { expense ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(com.example.data.partyLabelOf(expense), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                text = listOfNotNull(
                                    expense.description.takeIf { it.isNotBlank() },
                                    viewModel.formatDate(expense.date)
                                ).joinToString(" — "),
                                fontSize = 10.sp,
                                color = Color.Gray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${formatAmount(expense.amount)} $currency",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            IconButton(
                                onClick = { expenseToEdit = expense },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Expense", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                            }
                            IconButton(
                                onClick = { expenseToDelete = expense },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Expense", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    // دیالوگ ویرایش هزینه
    if (expenseToEdit != null) {
        EditClinicExpenseDialog(
            expense = expenseToEdit!!,
            currency = currency,
            isPersian = isPersian,
            viewModel = viewModel,
            onDismiss = { expenseToEdit = null }
        )
    }

    // دیالوگ تایید حذف هزینه
    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = {
                Text(
                    t("حذف هزینه", "Delete Expense"),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    t(
                        "آیا از حذف هزینه «${expenseToDelete!!.category}» به مبلغ ${formatAmount(expenseToDelete!!.amount)} $currency اطمینان دارید؟",
                        "Are you sure you want to delete expense '${expenseToDelete!!.category}' for ${formatAmount(expenseToDelete!!.amount)} $currency?"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense(expenseToDelete!!)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(t("حذف", "Delete"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text(t("انصراف", "Cancel"))
                }
            }
        )
    }

    // دیالوگ پرداخت یک هزینه (تمام یا بخشی از مانده)
    if (expenseToPay != null) {
        PayExpenseDialog(
            expense = expenseToPay!!,
            alreadyPaid = expensePayments.filter { !it.isDeleted && it.clinicExpenseId == expenseToPay!!.id }.sumOf { it.amount },
            bankAccounts = bankAccounts.filter { !it.isDeleted },
            isPersian = isPersian,
            currency = currency,
            viewModel = viewModel,
            onDismiss = { expenseToPay = null }
        )
    }

    // تأیید «وصول» چکِ هزینه
    if (expenseChequeToClear != null) {
        val c = expenseChequeToClear!!
        AlertDialog(
            onDismissRequest = { expenseChequeToClear = null },
            title = { Text(t("تأیید وصول چک", "Confirm cheque cleared"), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    t(
                        "چک ${c.chequeNumber.orEmpty().ifBlank { "—" }} به مبلغ ${formatAmount(c.amount)} $currency وصول‌شده ثبت شود؟",
                        "Mark cheque ${c.chequeNumber.orEmpty()} for ${formatAmount(c.amount)} $currency as cleared?"
                    ),
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.markChequeCleared(c); expenseChequeToClear = null }) {
                    Text(t("بله، وصول شد", "Yes, cleared"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { expenseChequeToClear = null }) { Text(t("انصراف", "Cancel")) } }
        )
    }

    // تأیید «برگشت» چکِ هزینه
    if (expenseChequeToBounce != null) {
        val c = expenseChequeToBounce!!
        AlertDialog(
            onDismissRequest = { expenseChequeToBounce = null },
            title = { Text(t("تأیید برگشت چک", "Confirm cheque bounced"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Text(
                    t(
                        "چک ${c.chequeNumber.orEmpty().ifBlank { "—" }} به مبلغ ${formatAmount(c.amount)} $currency برگشتی ثبت شود؟ تسویه‌ای که این چک انجام داده بود برمی‌گردد.",
                        "Mark cheque ${c.chequeNumber.orEmpty()} for ${formatAmount(c.amount)} $currency as bounced? Any settlement it made will be reversed."
                    ),
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.markChequeBounced(c); expenseChequeToBounce = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(t("بله، برگشت خورد", "Yes, bounced"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { expenseChequeToBounce = null }) { Text(t("انصراف", "Cancel")) } }
        )
    }
}

@Composable
private fun EditClinicExpenseDialog(
    expense: ClinicExpense,
    currency: String,
    isPersian: Boolean,
    viewModel: DentalViewModel,
    onDismiss: () -> Unit
) {
    fun t(fa: String, en: String) = if (isPersian) fa else en

    var category by remember { mutableStateOf(expense.category) }
    var partyName by remember { mutableStateOf(expense.subAccountName) }
    var amountStr by remember { mutableStateOf(expense.amount.toString()) }
    var desc by remember { mutableStateOf(expense.description) }
    var method by remember { mutableStateOf(expense.paymentMethod) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(t("ویرایش هزینه مطب", "Edit Clinic Expense"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(t("عنوان طرف‌حساب", "Party title")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text(t("نام طرف‌حساب", "Party name")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (category.isNotBlank() || partyName.isNotBlank()) {
                    Text(
                        text = com.example.data.partyDisplayLabel(category, partyName),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter(Char::isDigit) },
                    label = { Text("${t("مبلغ", "Amount")} ($currency)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                RealTimeAmountHelper(amountStr, isPersian)
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(t("توضیحات", "Description")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(t("نحوه پرداخت:", "Payment method:"), fontSize = 11.sp, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScrollCompat(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "CARD_TRANSFER" to t("کارت به کارت / شبا", "Card/IBAN"),
                        "POS" to t("کارتخوان", "POS"),
                        "CASH" to t("نقدی", "Cash"),
                        "CHEQUE" to t("چک", "Cheque"),
                        "OTHER" to t("سایر", "Other")
                    ).forEach { (key, labelTxt) ->
                        FilterChip(
                            selected = method == key,
                            onClick = { method = key },
                            label = { Text(labelTxt, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.trim().toLongOrNull() ?: 0L
                    if (amount > 0 && category.isNotBlank()) {
                        viewModel.updateExpense(
                            expense.copy(
                                category = category.trim(),
                                subAccountName = partyName.trim(),
                                amount = amount,
                                description = desc.trim(),
                                paymentMethod = method
                            )
                        )
                        onDismiss()
                    }
                },
                enabled = (amountStr.trim().toLongOrNull() ?: 0L) > 0 && category.isNotBlank()
            ) {
                Text(t("ذخیره تغییرات", "Save Changes"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(t("انصراف", "Cancel"))
            }
        }
    )
}

@Composable
private fun PayExpenseDialog(
    expense: ClinicExpense,
    alreadyPaid: Long,
    bankAccounts: List<BankAccount>,
    isPersian: Boolean,
    currency: String,
    viewModel: DentalViewModel,
    onDismiss: () -> Unit
) {
    fun t(fa: String, en: String) = if (isPersian) fa else en
    val remaining = expense.amount - alreadyPaid
    var payAmountStr by remember(expense.id) { mutableStateOf(remaining.coerceAtLeast(0L).toString()) }
    var method by remember(expense.id) { mutableStateOf("CARD_TRANSFER") }
    var selectedAccountId by remember(expense.id) { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t("پرداخت هزینه", "Pay Expense"), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    text = "${com.example.data.partyLabelOf(expense)} — ${expense.description.ifBlank { "—" }}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (remaining < 0L) {
                        t(
                            "مبلغ کل: ${formatAmount(expense.amount)} $currency | پرداخت‌شده: ${formatAmount(alreadyPaid)} $currency | بدهکار به ما: ${formatAmount(-remaining)} $currency",
                            "Total: ${formatAmount(expense.amount)} | Paid: ${formatAmount(alreadyPaid)} | Owes us: ${formatAmount(-remaining)} $currency"
                        )
                    } else {
                        t(
                            "مبلغ کل: ${formatAmount(expense.amount)} $currency | پرداخت‌شده: ${formatAmount(alreadyPaid)} $currency | مانده: ${formatAmount(remaining)} $currency",
                            "Total: ${formatAmount(expense.amount)} | Paid: ${formatAmount(alreadyPaid)} | Remaining: ${formatAmount(remaining)} $currency"
                        )
                    },
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                // هشدار اضافه‌پرداخت — ثبت مجاز است ولی کاربر باید آگاهانه انجامش دهد
                val enteredAmount = payAmountStr.trim().toLongOrNull() ?: 0L
                val overpay = alreadyPaid + enteredAmount - expense.amount
                if (enteredAmount > 0L && overpay > 0L) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF6A1B9A).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = t(
                                "این پرداخت ${formatAmount(overpay)} $currency بیشتر از مبلغ هزینه است؛ طرف‌حساب به همین اندازه به ما بدهکار می‌شود.",
                                "This payment exceeds the expense by ${formatAmount(overpay)} $currency; the party ends up owing us that amount."
                            ),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6A1B9A),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = payAmountStr, onValueChange = { payAmountStr = it.filter(Char::isDigit) },
                    label = { Text("${t("مبلغ پرداخت", "Payment Amount")} ($currency)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "CASH" to t("نقدی", "Cash"),
                        "CARD_TRANSFER" to t("کارت به کارت", "Card"),
                        "POS" to t("پوز", "POS"),
                        "CHEQUE" to t("چک", "Cheque")
                    ).forEach { (key, labelTxt) ->
                        FilterChip(
                            selected = method == key,
                            onClick = { method = key },
                            label = { Text(labelTxt, fontSize = 9.5.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (method == "CARD_TRANSFER" || method == "POS") {
                    Text(t("حساب بانکی کسر شونده:", "Debit bank account:"), fontSize = 10.sp, color = Color.Gray)
                    bankAccounts.forEach { acc ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedAccountId == acc.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, if (selectedAccountId == acc.id) MaterialTheme.colorScheme.primary else Color.Transparent),
                            onClick = { selectedAccountId = acc.id }
                        ) {
                            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(acc.bankName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("${formatAmount(acc.balance)} $currency", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = payAmountStr.trim().toLongOrNull() ?: 0L
                    // اضافه‌پرداخت مجاز است: طرف‌حساب بستانکار می‌شود و مانده منفی
                    // نمایش داده می‌شود. قبلاً هر مبلغ بیشتر از مانده کلاً رد می‌شد.
                    if (amount >= 1L) {
                        viewModel.insertExpensePayment(
                            ExpensePayment(
                                clinicExpenseId = expense.id,
                                amount = amount,
                                method = method,
                                bankAccountId = if (method == "CASH" || method == "CHEQUE") null else selectedAccountId,
                                note = t("پرداخت دستی هزینه: ${com.example.data.partyLabelOf(expense)}", "Manual expense payment: ${com.example.data.partyLabelOf(expense)}")
                            )
                        )
                        onDismiss()
                    }
                },
                enabled = (payAmountStr.trim().toLongOrNull() ?: 0L) >= 1L
            ) { Text(t("ثبت پرداخت", "Pay"), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t("انصراف", "Cancel")) }
        }
    )
}

// ==========================================
// تراکنش‌های شخصی (برداشت/درآمد شخصی/هزینه شخصی)
// ==========================================
@Composable
fun PersonalScreen(viewModel: DentalViewModel) {
    val isPersian = viewModel.isPersian.collectAsStateWithLifecycle().value
    val transactions by viewModel.personalTransactions.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val subAccounts by viewModel.personalSubAccounts.collectAsStateWithLifecycle()
    val currency = viewModel.label("currency")

    fun t(fa: String, en: String) = if (isPersian) fa else en

    var showForm by remember { mutableStateOf(false) }
    var showManageSubAccounts by remember { mutableStateOf(false) }
    var showPersonsDialog by remember { mutableStateOf(false) }
    var txToEdit by remember { mutableStateOf<PersonalTransaction?>(null) }
    var txToDelete by remember { mutableStateOf<PersonalTransaction?>(null) }

    var type by remember { mutableStateOf("DRAW") }
    var subAccount by remember { mutableStateOf(if (isPersian) "سایر" else "Other") }
    var subAccountExpanded by remember { mutableStateOf(false) }
    var amountStr by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }

    // فقط «حساب»‌ها (برگ‌های درخت) در تراکنش‌ها شرکت می‌کنند: گره‌ای که نامش والدِ
    // کسی نیست (نه parentName و نه grandParentNameِ گرهٔ دیگری). سرشاخه/شاخه‌ها که
    // صرفاً گروه‌بندی‌اند از فهرستِ انتخاب کنار گذاشته می‌شوند.
    val distinctSubs = remember(subAccounts) {
        val alive = subAccounts.filter { !it.isDeleted }
        val usedAsParent = (alive.map { it.parentName.trim() } + alive.map { it.grandParentName.trim() })
            .filter { it.isNotBlank() }.toSet()
        alive
            .filter { it.name.trim() !in usedAsParent }
            .distinctBy { "${it.category.trim()}|${it.grandParentName.trim()}|${it.parentName.trim()}|${it.name.trim()}" }
    }
    fun displayNameOf(sub: PersonalSubAccount): String =
        listOf(sub.grandParentName.trim(), sub.parentName.trim(), sub.name.trim())
            .filter { it.isNotBlank() }
            .joinToString(" - ")
    val starredSubs = remember(distinctSubs) { distinctSubs.filter { it.isQuickSelect } }
    val restSubs = remember(distinctSubs) { distinctSubs.filter { !it.isQuickSelect } }

    val active = transactions.filter { !it.isDeleted }.sortedByDescending { it.date }
    val totalIncome = active.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalDraw = active.filter { it.type == "DRAW" }.sumOf { it.amount }
    val totalExpense = active.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // «تراکنش جدید» طبق درخواست کاربر به یک سطر بالاتر و به‌صورت تمام‌عرض منتقل شد.
        Button(
            onClick = { showForm = !showForm },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            Icon(if (showForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (showForm) t("بستن فرم تراکنش", "Close Form") else t("تراکنش جدید", "New Transaction"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(t("تراکنش‌های شخصی", "Personal Transactions"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { showManageSubAccounts = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(t("سرفصل‌ها", "Categories"), fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = { showPersonsDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(t("حساب اشخاص", "Persons"), fontSize = 11.sp)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PersonalKpi(t("درآمد", "Income"), totalIncome, Color(0xFF2E7D32), currency, Modifier.weight(1f))
                PersonalKpi(t("برداشت", "Draw"), totalDraw, MaterialTheme.colorScheme.primary, currency, Modifier.weight(1f))
                PersonalKpi(t("هزینه", "Expense"), totalExpense, MaterialTheme.colorScheme.error, currency, Modifier.weight(1f))
            }
        }

        if (showForm) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "DRAW" to t("برداشت از مطب", "Draw"),
                            "INCOME" to t("درآمد شخصی", "Income"),
                            "EXPENSE" to t("هزینه شخصی", "Expense")
                        ).forEach { (key, labelTxt) ->
                            FilterChip(
                                selected = type == key,
                                onClick = { type = key },
                                label = { Text(labelTxt, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    // سرفصل‌های ستاردار (اولویت پیشنهادی) — با یک لمس انتخاب می‌شوند
                    if (starredSubs.isNotEmpty()) {
                        Text(
                            text = t("⭐ اولویت پیشنهادی (ستاردار):", "⭐ Priority (starred):"),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            starredSubs.forEach { sub ->
                                val dn = displayNameOf(sub)
                                FilterChip(
                                    selected = subAccount == dn,
                                    onClick = { subAccount = dn },
                                    label = { Text(dn, fontSize = 10.sp, maxLines = 1) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFF9A825))
                                    }
                                )
                            }
                        }
                    }
                    // بقیه‌ی سرفصل‌ها — در فیلد کشویی، قابل انتخاب (و همچنان قابل تایپ آزاد)
                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(
                        expanded = subAccountExpanded,
                        onExpandedChange = { subAccountExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = subAccount, onValueChange = { subAccount = it },
                            label = { Text(t("سرفصل (از لیست انتخاب کنید یا تایپ کنید)", "Ledger head (pick or type)")) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subAccountExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = subAccountExpanded,
                            onDismissRequest = { subAccountExpanded = false }
                        ) {
                            if (restSubs.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(t("سرفصل دیگری ثبت نشده — نام دلخواه را تایپ کنید", "No more headings — type one"), fontSize = 11.sp, color = Color.Gray) },
                                    onClick = { subAccountExpanded = false }
                                )
                            }
                            restSubs.forEach { sub ->
                                val dn = displayNameOf(sub)
                                DropdownMenuItem(
                                    text = { Text(dn, fontSize = 12.sp) },
                                    onClick = { subAccount = dn; subAccountExpanded = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = amountStr, onValueChange = { amountStr = it.filter(Char::isDigit) },
                        label = { Text("${t("مبلغ", "Amount")} ($currency)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    RealTimeAmountHelper(amountStr, isPersian)
                    OutlinedTextField(
                        value = desc, onValueChange = { desc = it },
                        label = { Text(t("توضیحات", "Description")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    if (bankAccounts.isNotEmpty()) {
                        Text(t("حساب بانکی مرتبط (اختیاری):", "Related bank account (optional):"), fontSize = 10.sp, color = Color.Gray)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScrollCompat(),
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
                    }
                    Button(
                        onClick = {
                            val amount = amountStr.trim().toLongOrNull() ?: 0L
                            if (amount > 0) {
                                viewModel.addPersonalTxDetailed(
                                    type = type,
                                    subAccountName = subAccount.trim().ifBlank { if (isPersian) "سایر" else "Other" },
                                    bankAccountId = selectedAccountId,
                                    amount = amount,
                                    desc = desc.trim()
                                )
                                amountStr = ""; desc = ""
                                showForm = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = (amountStr.trim().toLongOrNull() ?: 0L) > 0
                    ) { Text(t("ثبت تراکنش", "Save Transaction"), fontWeight = FontWeight.Bold) }
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(active, key = { it.id }) { tx ->
                val color = when (tx.type) {
                    "INCOME" -> Color(0xFF2E7D32)
                    "DRAW" -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                }
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.subAccountName.ifBlank { t("سایر", "Other") }, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                text = listOfNotNull(
                                    tx.description.takeIf { it.isNotBlank() },
                                    viewModel.formatDate(tx.date)
                                ).joinToString(" — "),
                                fontSize = 10.sp,
                                color = Color.Gray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${if (tx.type == "EXPENSE" || tx.type == "DRAW") "-" else "+"}${formatAmount(tx.amount)} $currency",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = color
                            )
                            IconButton(
                                onClick = { txToEdit = tx },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                            }
                            IconButton(
                                onClick = { txToDelete = tx },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    if (showManageSubAccounts) {
        ManageSubAccountsDialog(
            viewModel = viewModel,
            isPersian = isPersian,
            onDismiss = { showManageSubAccounts = false }
        )
    }

    if (showPersonsDialog) {
        PersonsAccountsDialog(
            viewModel = viewModel,
            isPersian = isPersian,
            onDismiss = { showPersonsDialog = false }
        )
    }

    if (txToEdit != null) {
        EditPersonalTxDialog(
            tx = txToEdit!!,
            bankAccounts = bankAccounts.filter { !it.isDeleted },
            currency = currency,
            isPersian = isPersian,
            viewModel = viewModel,
            onDismiss = { txToEdit = null }
        )
    }

    if (txToDelete != null) {
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text(t("حذف تراکنش شخصی", "Delete Transaction"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text(t("آیا از حذف این تراکنش به مبلغ ${formatAmount(txToDelete!!.amount)} $currency اطمینان دارید؟", "Delete transaction for ${formatAmount(txToDelete!!.amount)} $currency?")) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePersonalTx(txToDelete!!)
                        txToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(t("حذف", "Delete"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) { Text(t("انصراف", "Cancel")) }
            }
        )
    }
}

@Composable
private fun EditPersonalTxDialog(
    tx: PersonalTransaction,
    bankAccounts: List<BankAccount>,
    currency: String,
    isPersian: Boolean,
    viewModel: DentalViewModel,
    onDismiss: () -> Unit
) {
    fun t(fa: String, en: String) = if (isPersian) fa else en

    var type by remember { mutableStateOf(tx.type) }
    var subAccount by remember { mutableStateOf(tx.subAccountName) }
    var amountStr by remember { mutableStateOf(tx.amount.toString()) }
    var desc by remember { mutableStateOf(tx.description) }
    var selectedAccountId by remember { mutableStateOf(tx.bankAccountId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(t("ویرایش تراکنش شخصی", "Edit Personal Transaction"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "DRAW" to t("برداشت از مطب", "Draw"),
                        "INCOME" to t("درآمد شخصی", "Income"),
                        "EXPENSE" to t("هزینه شخصی", "Expense")
                    ).forEach { (key, labelTxt) ->
                        FilterChip(
                            selected = type == key,
                            onClick = { type = key },
                            label = { Text(labelTxt, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = subAccount, onValueChange = { subAccount = it },
                    label = { Text(t("سرفصل", "Ledger Head")) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = amountStr, onValueChange = { amountStr = it.filter(Char::isDigit) },
                    label = { Text("${t("مبلغ", "Amount")} ($currency)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                RealTimeAmountHelper(amountStr, isPersian)
                OutlinedTextField(
                    value = desc, onValueChange = { desc = it },
                    label = { Text(t("توضیحات", "Description")) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                if (bankAccounts.isNotEmpty()) {
                    Text(t("حساب بانکی مرتبط (اختیاری):", "Related bank account (optional):"), fontSize = 10.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScrollCompat(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedAccountId == null,
                            onClick = { selectedAccountId = null },
                            label = { Text(t("بدون حساب", "None"), fontSize = 10.sp) }
                        )
                        bankAccounts.forEach { acc ->
                            FilterChip(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id },
                                label = { Text(acc.bankName, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.trim().toLongOrNull() ?: 0L
                    if (amount > 0) {
                        viewModel.updatePersonalTx(
                            tx.copy(
                                type = type,
                                subAccountName = subAccount.trim().ifBlank { if (isPersian) "سایر" else "Other" },
                                bankAccountId = selectedAccountId,
                                amount = amount,
                                description = desc.trim()
                            )
                        )
                        onDismiss()
                    }
                },
                enabled = (amountStr.trim().toLongOrNull() ?: 0L) > 0
            ) {
                Text(t("ذخیره تغییرات", "Save Changes"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(t("انصراف", "Cancel"))
            }
        }
    )
}

@Composable
private fun PersonalKpi(title: String, value: Long, color: Color, currency: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            Text(formatAmount(value), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(currency, fontSize = 8.5.sp, color = color.copy(alpha = 0.8f))
        }
    }
}

// ==========================================
// تعمیر و نگهداری تجهیزات
// ==========================================
@Composable
fun MaintenanceScreen(viewModel: DentalViewModel) {
    val isPersian = viewModel.isPersian.collectAsStateWithLifecycle().value
    val maintenanceList by viewModel.maintenanceList.collectAsStateWithLifecycle()
    val currency = viewModel.label("currency")

    fun t(fa: String, en: String) = if (isPersian) fa else en

    var showForm by remember { mutableStateOf(false) }
    var equipment by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }

    val active = maintenanceList.filter { !it.isDeleted }.sortedByDescending { it.date }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(t("تعمیر و نگهداری", "Maintenance"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { showForm = !showForm }) {
                Icon(if (showForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (showForm) t("بستن", "Close") else t("ثبت سرویس جدید", "New Service"), fontWeight = FontWeight.Bold)
            }
        }

        if (showForm) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = equipment, onValueChange = { equipment = it },
                        label = { Text(t("نام تجهیزات (مثلاً یونیت ۱، اتوکلاو)", "Equipment (e.g. Unit 1, Autoclave)")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = desc, onValueChange = { desc = it },
                        label = { Text(t("شرح سرویس/تعمیر", "Service description")) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = costStr, onValueChange = { costStr = it.filter(Char::isDigit) },
                        label = { Text("${t("هزینه", "Cost")} ($currency)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Button(
                        onClick = {
                            val cost = costStr.trim().toLongOrNull() ?: 0L
                            if (equipment.isNotBlank()) {
                                viewModel.addMaintenance(
                                    equipment = equipment.trim(),
                                    desc = desc.trim(),
                                    cost = cost,
                                    date = System.currentTimeMillis(),
                                    nextCheck = null
                                )
                                equipment = ""; desc = ""; costStr = ""
                                showForm = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = equipment.isNotBlank()
                    ) { Text(t("ثبت سرویس", "Save Service"), fontWeight = FontWeight.Bold) }
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (active.isEmpty()) {
                item {
                    Text(
                        text = t("هنوز سرویسی ثبت نشده است.", "No maintenance records yet."),
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
            items(active, key = { it.id }) { m ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(m.equipmentName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                text = listOfNotNull(
                                    m.description.takeIf { it.isNotBlank() },
                                    m.technicianName?.takeIf { it.isNotBlank() }?.let { if (isPersian) "تکنسین: $it" else "Tech: $it" },
                                    viewModel.formatDate(m.date)
                                ).joinToString(" — "),
                                fontSize = 10.sp,
                                color = Color.Gray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            m.nextServiceDate?.let {
                                Text(
                                    text = "${t("سرویس بعدی:", "Next service:")} ${viewModel.formatDate(it)}",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Text(
                            text = "${formatAmount(m.cost)} $currency",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

/** اسکرول افقی برای ردیف چیپ‌ها — کمک‌تابع کوچک برای جلوگیری از تکرار. */
@Composable
private fun Modifier.horizontalScrollCompat(): Modifier = this.horizontalScroll(rememberScrollState())

// حساب اشخاص — بخشِ مستقل با افزودنِ شخصِ جدید در انتخاب‌کننده + سوابقِ گزارش‌پذیر.
// هر تراکنش دو سند دارد: (۱) رکوردِ children_expenses (مبنای گزارش) و (۲) تراکنشِ
// شخصیِ «حساب اشخاص: نام». سندهای هم‌مبلغِ هم‌تاریخ برای جلوگیری از دوباره‌شماری تجمیع می‌شوند.
private const val PERSON_TX_PREFIX = "حساب اشخاص: "

private data class PersonLedgerRecord(val date: Long, val description: String, val amount: Long)

@Composable
fun PersonsAccountsDialog(
    viewModel: DentalViewModel,
    isPersian: Boolean,
    onDismiss: () -> Unit
) {
    val currency = viewModel.label("currency")
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val childrenExpenses by viewModel.childrenExpenses.collectAsStateWithLifecycle()
    val personalTransactions by viewModel.personalTransactions.collectAsStateWithLifecycle()
    val personProfiles by viewModel.personProfiles.collectAsStateWithLifecycle()

    fun t(fa: String, en: String) = if (isPersian) fa else en

    // ——— فرم ثبت شخص: نام، نام خانوادگی، عنوان و ماهیت ———
    var newFirstName by remember { mutableStateOf("") }
    var newLastName by remember { mutableStateOf("") }
    var newTitle by remember { mutableStateOf("") }
    var newNature by remember { mutableStateOf(PersonNature.DEBTOR) }
    var editingPersonId by remember { mutableStateOf(0L) }

    var justAdded by remember { mutableStateOf<String?>(null) }
    var selectedPerson by remember { mutableStateOf<String?>(null) }

    fun resetPersonForm() {
        newFirstName = ""; newLastName = ""; newTitle = ""
        newNature = PersonNature.DEBTOR; editingPersonId = 0L
    }

    // جریات هر شخص — سندهای یکتا و مرتب‌شده بر اساس تاریخ
    val personRecords: Map<String, List<PersonLedgerRecord>> = remember(childrenExpenses, personalTransactions) {
        val map = LinkedHashMap<String, MutableList<PersonLedgerRecord>>()
        val seen = mutableSetOf<String>()
        fun put(person: String, r: PersonLedgerRecord) {
            val key = "${com.example.util.PersianCalendarHelper.jalaliKeyOf(r.date)}|${r.amount}|${r.description}"
            if (seen.add(key)) {
                map.getOrPut(person) { mutableListOf() }.add(r)
            }
        }
        childrenExpenses
            .filter { !it.isDeleted && it.childName.isNotBlank() }
            .forEach { e ->
                put(e.childName.trim(), PersonLedgerRecord(e.date, e.title.trim().ifBlank { t("برداشت بانکی", "Bank withdrawal") }, e.amount))
            }
        personalTransactions
            .filter { !it.isDeleted && it.subAccountName.startsWith(PERSON_TX_PREFIX) }
            .forEach { tx ->
                val person = tx.subAccountName.removePrefix(PERSON_TX_PREFIX).trim()
                if (person.isBlank()) return@forEach
                val rawDesc = tx.description.trim()
                val desc = rawDesc.substringBefore(" — $person").trim().ifBlank { t("برداشت بانکی", "Bank withdrawal") }
                put(person, PersonLedgerRecord(tx.date, desc, tx.amount))
            }
        map.mapValues { entry -> entry.value.sortedBy { it.date } }
    }

    // لیست اشخاص: پرونده‌های ثبت‌شده + نام‌های قدیمی تنظیمات + سوابق تراکنش‌ها
    val personNames = remember(settings, personRecords, personProfiles) {
        (personProfiles.map { it.fullName } +
            settings.childrenNames.split(",", "،").map { it.trim() }.filter { it.isNotBlank() } +
            personRecords.keys)
            .filter { it.isNotBlank() }
            .distinctBy { it.trim().lowercase() }
            .sorted()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(t("حساب اشخاص", "Person Accounts"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val currentPerson = selectedPerson
                if (currentPerson == null) {
                    // ——— فرم ثبت/ویرایش شخص: نام، نام خانوادگی، عنوان، ماهیت ———
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (editingPersonId == 0L) t("ثبت شخص جدید", "Add a new person")
                                       else t("ویرایش مشخصات شخص", "Edit person"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = newFirstName,
                                    onValueChange = { newFirstName = it },
                                    label = { Text(t("نام", "First name"), fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                                OutlinedTextField(
                                    value = newLastName,
                                    onValueChange = { newLastName = it },
                                    label = { Text(t("نام خانوادگی", "Last name"), fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                            }
                            OutlinedTextField(
                                value = newTitle,
                                onValueChange = { newTitle = it },
                                label = { Text(t("عنوان شخص (مثلاً تأمین‌کننده، همکار)", "Person title"), fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )

                            // ——— ماهیت شخص: بدهکار / بستانکار ———
                            Text(t("ماهیت شخص", "Person nature"), fontSize = 10.sp, color = Color.Gray)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(PersonNature.DEBTOR, PersonNature.CREDITOR).forEach { nature ->
                                    val isSel = newNature == nature
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { newNature = nature },
                                        label = {
                                            Text(
                                                nature.label(isPersian),
                                                fontSize = 10.5.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (nature == PersonNature.DEBTOR)
                                                MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                                            else
                                                Color(0xFF2E7D32).copy(alpha = 0.18f)
                                        )
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val profile = PersonProfile(
                                            id = editingPersonId,
                                            firstName = newFirstName.trim(),
                                            lastName = newLastName.trim(),
                                            title = newTitle.trim(),
                                            nature = newNature
                                        )
                                        if (profile.fullName.isNotBlank()) {
                                            viewModel.savePersonProfile(profile)
                                            justAdded = profile.fullName
                                            resetPersonForm()
                                        }
                                    },
                                    enabled = newFirstName.isNotBlank() || newLastName.isNotBlank(),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (editingPersonId == 0L) t("ثبت شخص", "Save person") else t("به‌روزرسانی", "Update"),
                                        fontSize = 11.sp
                                    )
                                }
                                if (editingPersonId != 0L) {
                                    OutlinedButton(
                                        onClick = { resetPersonForm() },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(t("انصراف", "Cancel"), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    justAdded?.let { name ->
                        Text(
                            text = t("✅ شخص «$name» به حساب اشخاص اضافه شد.", "✅ Person \"$name\" added."),
                            fontSize = 10.5.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = t("لیست اشخاص — برای مشاهده جریات، روی نام شخص لمس کنید:", "Persons — tap a name to see its ledger:"),
                        fontSize = 10.5.sp,
                        color = Color.Gray
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f, fill = false)) {
                        if (personNames.isEmpty()) {
                            item {
                                Text(
                                    text = t("هنوز شخصي ثبت نشده است؛ نام شخص جدید را در فیلد بالا وارد کنید.", "No persons yet; type a new person name above."),
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                        items(personNames, key = { it }) { person ->
                            val recs = personRecords[person].orEmpty()
                            val total = recs.sumOf { it.amount }
                            // پروندهٔ شخص (عنوان و ماهیت) — اگر شخص فقط از سوابق قدیمی
                            // آمده باشد، پرونده‌ای ندارد و مقادیر خالی می‌مانند.
                            val profile = personProfiles.firstOrNull {
                                it.fullName.equals(person, ignoreCase = true)
                            }
                            val natureColor = when (profile?.nature) {
                                PersonNature.CREDITOR -> Color(0xFF2E7D32)
                                PersonNature.DEBTOR -> MaterialTheme.colorScheme.error
                                else -> Color.Gray
                            }
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { selectedPerson = person },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("👤 $person", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        val personTitle = profile?.title.orEmpty()
                                        if (personTitle.isNotBlank()) {
                                            Text(personTitle, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            if (profile != null && profile.nature != PersonNature.NONE) {
                                                Surface(
                                                    color = natureColor.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = profile.nature.label(isPersian),
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = natureColor,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = t("${recs.size} تراکنش مستند", "${recs.size} documented transactions"),
                                                fontSize = 9.5.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${formatAmount(total)} $currency",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = natureColor
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    val p = profile ?: PersonProfile.fromLegacyName(person)
                                                    newFirstName = p.firstName
                                                    newLastName = p.lastName
                                                    newTitle = p.title
                                                    newNature = if (p.nature == PersonNature.NONE) PersonNature.DEBTOR else p.nature
                                                    editingPersonId = p.id
                                                },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = t("ویرایش", "Edit"),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ——— ریز حساب شخص انتخاب‌شده (سوابق مستند) ———
                    val recs = personRecords[currentPerson].orEmpty()
                    val currentProfile = personProfiles.firstOrNull {
                        it.fullName.equals(currentPerson, ignoreCase = true)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(onClick = { selectedPerson = null }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text("👤 $currentPerson", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            val subtitle = listOfNotNull(
                                currentProfile?.title?.takeIf { it.isNotBlank() },
                                currentProfile?.nature?.takeIf { it != PersonNature.NONE }?.label(isPersian)
                            ).joinToString(" • ")
                            if (subtitle.isNotBlank()) {
                                Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Text(
                        text = t("ریز حساب شخص — به ترتیب تاریخ، با تراز تجمعی:", "Person sub-ledger — by date, with running balance:"),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f, fill = false)) {
                        if (recs.isEmpty()) {
                            item {
                                Text(
                                    text = t("تراکنشی برای این شخص ثبت نشده است.", "No transactions recorded for this person."),
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                        val withRunning = recs.mapIndexed { i, r -> r to recs.take(i + 1).sumOf { it.amount } }
                        items(withRunning, key = { "${it.first.date}-${it.first.amount}-${it.first.description}" }) { (rec, running) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = viewModel.formatDate(rec.date),
                                    fontSize = 9.5.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.width(78.dp)
                                )
                                Text(
                                    text = rec.description,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "-${formatAmount(rec.amount)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "${formatAmount(running)} $currency",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (recs.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = t("جمع کل حساب", "Account total"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                    Text(
                                        text = "${formatAmount(recs.sumOf { it.amount })} $currency",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
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
}
