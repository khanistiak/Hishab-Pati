package com.example.data.backup

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.DebtType
import com.example.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CsvBackupEngine {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    fun exportToCsv(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        debts: List<DebtEntity>,
        settings: AppSettingsEntity? = null
    ): String {
        val sb = StringBuilder()
        val accountMap = accounts.associateBy { it.id }
        val currency = settings?.currencyCode ?: "BDT"

        sb.append("# Hishab Pati Export\n")
        sb.append("# Generated: ${isoDateFormat.format(Date())}\n")
        sb.append("# Currency: $currency\n\n")

        // 1. TRANSACTIONS SECTION
        sb.append("SECTION,TRANSACTIONS\n")
        sb.append("ID,Date,Timestamp,Type,Amount,Currency,AccountID,AccountName,ToAccountID,ToAccountName,Category,Subcategory,Title,PayeeOrPayer,Notes,IsRecurring\n")

        for (tx in transactions) {
            val dateStr = isoDateFormat.format(Date(tx.timestamp))
            val accName = accountMap[tx.accountId]?.name ?: tx.accountId
            val toAccName = tx.toAccountId?.let { accountMap[it]?.name ?: it } ?: ""

            sb.append(escapeCsv(tx.id)).append(",")
            sb.append(escapeCsv(dateStr)).append(",")
            sb.append(tx.timestamp).append(",")
            sb.append(escapeCsv(tx.type.name)).append(",")
            sb.append(String.format(Locale.US, "%.2f", tx.amount)).append(",")
            sb.append(escapeCsv(currency)).append(",")
            sb.append(escapeCsv(tx.accountId)).append(",")
            sb.append(escapeCsv(accName)).append(",")
            sb.append(escapeCsv(tx.toAccountId ?: "")).append(",")
            sb.append(escapeCsv(toAccName)).append(",")
            sb.append(escapeCsv(tx.category)).append(",")
            sb.append(escapeCsv(tx.subcategory)).append(",")
            sb.append(escapeCsv(tx.title)).append(",")
            sb.append(escapeCsv(tx.payeeOrPayer)).append(",")
            sb.append(escapeCsv(tx.notes)).append(",")
            sb.append(tx.isRecurring).append("\n")
        }

        // 2. ACCOUNTS SECTION
        sb.append("\nSECTION,ACCOUNTS\n")
        sb.append("ID,Name,Type,Institution,Balance,OpeningBalance,AccountNumberLast4,Notes\n")
        for (acc in accounts) {
            sb.append(escapeCsv(acc.id)).append(",")
            sb.append(escapeCsv(acc.name)).append(",")
            sb.append(escapeCsv(acc.type.name)).append(",")
            sb.append(escapeCsv(acc.institution)).append(",")
            sb.append(String.format(Locale.US, "%.2f", acc.balance)).append(",")
            sb.append(String.format(Locale.US, "%.2f", acc.openingBalance)).append(",")
            sb.append(escapeCsv(acc.accountNumberLast4)).append(",")
            sb.append(escapeCsv(acc.notes)).append("\n")
        }

        // 3. DEBTS SECTION
        sb.append("\nSECTION,DEBTS\n")
        sb.append("ID,PersonName,Type,Amount,TotalAmount,PaidAmount,DueDate,Notes,IsSettled,AccountID\n")
        for (d in debts) {
            sb.append(escapeCsv(d.id)).append(",")
            sb.append(escapeCsv(d.personName)).append(",")
            sb.append(escapeCsv(d.type.name)).append(",")
            sb.append(String.format(Locale.US, "%.2f", d.amount)).append(",")
            sb.append(String.format(Locale.US, "%.2f", d.totalAmount)).append(",")
            sb.append(String.format(Locale.US, "%.2f", d.paidAmount)).append(",")
            sb.append(escapeCsv(d.dueDate)).append(",")
            sb.append(escapeCsv(d.notes)).append(",")
            sb.append(d.isSettled).append(",")
            sb.append(escapeCsv(d.accountId ?: "")).append("\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }

    fun parseCsv(csvText: String): NormalizedBackupPayload {
        if (csvText.isBlank()) {
            throw BackupRestoreException(
                BackupDiagnosticError(
                    errorCode = BackupErrorCode.ERR_EMPTY_OR_CORRUPTED,
                    failedStage = RestoreStage.READING_FILE,
                    technicalDetails = "The CSV file is empty."
                )
            )
        }

        val lines = parseCsvLines(csvText)
        if (lines.isEmpty()) {
            throw BackupRestoreException(
                BackupDiagnosticError(
                    errorCode = BackupErrorCode.ERR_INVALID_JSON,
                    failedStage = RestoreStage.VALIDATING_STRUCTURE,
                    technicalDetails = "No readable data rows found in CSV."
                )
            )
        }

        val parsedTransactions = mutableListOf<TransactionEntity>()
        val parsedAccounts = mutableListOf<AccountEntity>()
        val parsedDebts = mutableListOf<DebtEntity>()
        val detectedCategories = mutableMapOf<String, Boolean>() // name -> isIncome

        var currentSection = "TRANSACTIONS"
        var txHeaders: Map<String, Int>? = null
        var accHeaders: Map<String, Int>? = null
        var debtHeaders: Map<String, Int>? = null

        for (row in lines) {
            if (row.isEmpty()) continue
            val firstCell = row[0].trim()

            // Skip comments
            if (firstCell.startsWith("#")) continue

            // Detect section headers
            if (firstCell.equals("SECTION", ignoreCase = true) && row.size > 1) {
                currentSection = row[1].trim().uppercase(Locale.US)
                continue
            }

            // Check if this row is a column header
            val rowLower = row.map { it.trim().lowercase(Locale.US) }
            if (rowLower.contains("personname") || (rowLower.contains("person") && rowLower.contains("totalamount")) || (currentSection == "DEBTS" && debtHeaders == null)) {
                debtHeaders = rowLower.mapIndexed { idx, name -> name to idx }.toMap()
                currentSection = "DEBTS"
                continue
            } else if ((rowLower.contains("name") && (rowLower.contains("balance") || rowLower.contains("institution"))) || (currentSection == "ACCOUNTS" && accHeaders == null)) {
                accHeaders = rowLower.mapIndexed { idx, name -> name to idx }.toMap()
                currentSection = "ACCOUNTS"
                continue
            } else if ((rowLower.contains("amount") && (rowLower.contains("category") || rowLower.contains("date") || rowLower.contains("title"))) || (currentSection == "TRANSACTIONS" && txHeaders == null)) {
                txHeaders = rowLower.mapIndexed { idx, name -> name to idx }.toMap()
                currentSection = "TRANSACTIONS"
                continue
            }

            when (currentSection) {
                "TRANSACTIONS" -> {
                    val map = txHeaders
                    if (map != null) {
                        try {
                            val id = getCell(row, map, "id").ifBlank { "csv_tx_${java.util.UUID.randomUUID().toString().take(8)}" }
                            val typeStr = getCell(row, map, "type").uppercase(Locale.US)
                            val type = try {
                                TransactionType.valueOf(typeStr)
                            } catch (_: Exception) {
                                if (typeStr.contains("INCOME")) TransactionType.INCOME
                                else if (typeStr.contains("TRANSFER")) TransactionType.TRANSFER
                                else TransactionType.EXPENSE
                            }
                            val amount = getCell(row, map, "amount").toDoubleOrNull() ?: 0.0
                            val timestampStr = getCell(row, map, "timestamp")
                            val dateStr = getCell(row, map, "date")
                            val timestamp = timestampStr.toLongOrNull()
                                ?: tryParseDate(dateStr)
                                ?: System.currentTimeMillis()

                            val accId = getCell(row, map, "accountid").ifBlank {
                                val accName = getCell(row, map, "accountname").ifBlank { "Cash Wallet" }
                                "acc_${accName.lowercase(Locale.US).replace(" ", "_")}"
                            }
                            val toAccId = getCell(row, map, "toaccountid").ifBlank {
                                val toName = getCell(row, map, "toaccountname")
                                if (toName.isNotBlank()) "acc_${toName.lowercase(Locale.US).replace(" ", "_")}" else null
                            }

                            val category = getCell(row, map, "category").ifBlank {
                                if (type == TransactionType.INCOME) "Income" else "Miscellaneous"
                            }
                            val subcategory = getCell(row, map, "subcategory")
                            val title = getCell(row, map, "title").ifBlank {
                                if (type == TransactionType.TRANSFER) "Transfer" else category
                            }
                            val payeeOrPayer = getCell(row, map, "payeeorpayer")
                            val notes = getCell(row, map, "notes")
                            val isRecurring = getCell(row, map, "isrecurring").toBooleanStrictOrNull() ?: false

                            if (amount > 0.0) {
                                parsedTransactions.add(
                                    TransactionEntity(
                                        id = id,
                                        type = type,
                                        amount = amount,
                                        title = title,
                                        category = category,
                                        subcategory = subcategory,
                                        accountId = accId,
                                        toAccountId = toAccId,
                                        timestamp = timestamp,
                                        notes = notes,
                                        isRecurring = isRecurring,
                                        payeeOrPayer = payeeOrPayer
                                    )
                                )
                                detectedCategories[category] = (type == TransactionType.INCOME)
                            }
                        } catch (_: Exception) {
                            // Skip unparseable row
                        }
                    }
                }
                "ACCOUNTS" -> {
                    val map = accHeaders
                    if (map != null) {
                        try {
                            val id = getCell(row, map, "id").ifBlank { "acc_${java.util.UUID.randomUUID().toString().take(8)}" }
                            val name = getCell(row, map, "name").ifBlank { "Account" }
                            val typeStr = getCell(row, map, "type").uppercase(Locale.US)
                            val type = try {
                                AccountType.valueOf(typeStr)
                            } catch (_: Exception) {
                                AccountType.CASH
                            }
                            val institution = getCell(row, map, "institution")
                            val balance = getCell(row, map, "balance").toDoubleOrNull() ?: 0.0
                            val openingBalance = getCell(row, map, "openingbalance").toDoubleOrNull() ?: balance
                            val last4 = getCell(row, map, "accountnumberlast4")
                            val notes = getCell(row, map, "notes")

                            parsedAccounts.add(
                                AccountEntity(
                                    id = id,
                                    name = name,
                                    type = type,
                                    institution = institution,
                                    balance = balance,
                                    openingBalance = openingBalance,
                                    accountNumberLast4 = last4,
                                    notes = notes
                                )
                            )
                        } catch (_: Exception) {
                            // Skip row
                        }
                    }
                }
                "DEBTS" -> {
                    val map = debtHeaders
                    if (map != null) {
                        try {
                            val id = getCell(row, map, "id").ifBlank { "debt_${java.util.UUID.randomUUID().toString().take(8)}" }
                            val personName = getCell(row, map, "personname").ifBlank { "Contact" }
                            val typeStr = getCell(row, map, "type").uppercase(Locale.US)
                            val type = try {
                                DebtType.valueOf(typeStr)
                            } catch (_: Exception) {
                                DebtType.BORROWED
                            }
                            val amount = getCell(row, map, "amount").toDoubleOrNull() ?: 0.0
                            val totalAmount = getCell(row, map, "totalamount").toDoubleOrNull() ?: amount
                            val paidAmount = getCell(row, map, "paidamount").toDoubleOrNull() ?: 0.0
                            val dueDate = getCell(row, map, "duedate")
                            val notes = getCell(row, map, "notes")
                            val isSettled = getCell(row, map, "issettled").toBooleanStrictOrNull() ?: false
                            val accId = getCell(row, map, "accountid")

                            parsedDebts.add(
                                DebtEntity(
                                    id = id,
                                    personName = personName,
                                    amount = amount,
                                    totalAmount = totalAmount,
                                    paidAmount = paidAmount,
                                    type = type,
                                    dueDate = dueDate,
                                    notes = notes,
                                    isSettled = isSettled,
                                    accountId = accId
                                )
                            )
                        } catch (_: Exception) {
                            // Skip row
                        }
                    }
                }
            }
        }

        // Synthesize accounts if none were declared in accounts section
        val finalAccounts = parsedAccounts.toMutableList()
        val accountIds = finalAccounts.map { it.id }.toMutableSet()
        for (tx in parsedTransactions) {
            if (!accountIds.contains(tx.accountId)) {
                accountIds.add(tx.accountId)
                finalAccounts.add(
                    AccountEntity(
                        id = tx.accountId,
                        name = tx.accountId.replace("acc_", "").replace("_", " ").replaceFirstChar { it.uppercase() },
                        type = AccountType.CASH,
                        balance = 0.0,
                        openingBalance = 0.0
                    )
                )
            }
            tx.toAccountId?.let { toId ->
                if (!accountIds.contains(toId)) {
                    accountIds.add(toId)
                    finalAccounts.add(
                        AccountEntity(
                            id = toId,
                            name = toId.replace("acc_", "").replace("_", " ").replaceFirstChar { it.uppercase() },
                            type = AccountType.CASH,
                            balance = 0.0,
                            openingBalance = 0.0
                        )
                    )
                }
            }
        }

        if (finalAccounts.isEmpty() && parsedTransactions.isNotEmpty()) {
            finalAccounts.add(
                AccountEntity(
                    id = "acc_cash_wallet",
                    name = "Cash Wallet",
                    type = AccountType.CASH,
                    balance = 0.0
                )
            )
        }

        // Synthesize categories
        val finalCategories = mutableListOf<CategoryEntity>()
        detectedCategories.forEach { (catName, isIncome) ->
            finalCategories.add(
                CategoryEntity(
                    id = "cat_${catName.lowercase(Locale.US).replace(" ", "_")}",
                    name = catName,
                    isIncome = isIncome,
                    iconName = "category",
                    colorHex = if (isIncome) "#10B981" else "#EF4444"
                )
            )
        }

        if (finalAccounts.isEmpty() && parsedTransactions.isEmpty() && parsedDebts.isEmpty()) {
            throw BackupRestoreException(
                BackupDiagnosticError(
                    errorCode = BackupErrorCode.ERR_MISSING_REQUIRED_FIELDS,
                    failedStage = RestoreStage.VALIDATING_STRUCTURE,
                    technicalDetails = "No valid transactions or accounts found in CSV file."
                )
            )
        }

        return NormalizedBackupPayload(
            appName = "Hishab Pati",
            schemaVersion = BackupMigrationEngine.CURRENT_SCHEMA_VERSION,
            formatType = BackupFormatType.MY_WALLET_CURRENT,
            backupDate = isoDateFormat.format(Date()),
            settings = null,
            accounts = finalAccounts,
            accountsHaveExplicitBalance = parsedAccounts.map { it.id }.toSet(),
            categories = finalCategories,
            subcategories = emptyList(),
            transactions = parsedTransactions,
            budgets = emptyList(),
            debts = parsedDebts,
            savingsGoals = emptyList(),
            skippedDuplicatesCount = 0,
            migratedRecordsCount = 0
        )
    }

    private fun getCell(row: List<String>, map: Map<String, Int>, key: String): String {
        val idx = map[key] ?: return ""
        if (idx in row.indices) {
            return row[idx].trim()
        }
        return ""
    }

    private fun tryParseDate(dateStr: String): Long? {
        if (dateStr.isBlank()) return null
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy",
            "MM/dd/yyyy"
        )
        for (pattern in formats) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US)
                val d = parser.parse(dateStr)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return null
    }

    private fun parseCsvLines(csv: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        var currentField = StringBuilder()
        var currentRow = mutableListOf<String>()
        var inQuotes = false
        var i = 0

        while (i < csv.length) {
            val c = csv[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < csv.length && csv[i + 1] == '"') {
                        currentField.append('"')
                        i++ // Skip escaped quote
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    currentRow.add(currentField.toString())
                    currentField = StringBuilder()
                }
                (c == '\n' || c == '\r') && !inQuotes -> {
                    if (c == '\r' && i + 1 < csv.length && csv[i + 1] == '\n') {
                        i++
                    }
                    currentRow.add(currentField.toString())
                    if (currentRow.any { it.isNotBlank() }) {
                        result.add(currentRow)
                    }
                    currentRow = mutableListOf()
                    currentField = StringBuilder()
                }
                else -> {
                    currentField.append(c)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            if (currentRow.any { it.isNotBlank() }) {
                result.add(currentRow)
            }
        }

        return result
    }
}
