package com.example.data.backup

import com.example.data.local.InitialData
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.SubcategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.data.model.BackgroundTextureOption
import com.example.data.model.DebtType
import com.example.data.model.ThemeColorOption
import com.example.data.model.ThemeMode
import com.example.data.model.TransactionType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

data class NormalizedBackupPayload(
    val appName: String,
    val schemaVersion: Int,
    val formatType: BackupFormatType,
    val backupDate: String,
    val settings: AppSettingsEntity?,
    val accounts: List<AccountEntity>,
    val accountsHaveExplicitBalance: Set<String>,
    val categories: List<CategoryEntity>,
    val subcategories: List<SubcategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val debts: List<DebtEntity>,
    val savingsGoals: List<SavingsGoalEntity>,
    val skippedDuplicatesCount: Int,
    val migratedRecordsCount: Int
)

object BackupMigrationEngine {

    const val CURRENT_SCHEMA_VERSION = 1
    const val MAX_SUPPORTED_SCHEMA_VERSION = 5
    private const val MAX_SUPPORTED_EXPEND_VERSION = 10

    fun parseAndMigrate(
        jsonString: String,
        existingAccountIds: Set<String> = emptySet(),
        onStageUpdate: (RestoreStage, Int, Int, String) -> Unit = { _, _, _, _ -> }
    ): NormalizedBackupPayload {
        onStageUpdate(RestoreStage.READING_FILE, 0, 100, "Reading backup payload...")

        if (jsonString.isBlank()) {
            throw BackupRestoreException(
                BackupDiagnosticError(
                    errorCode = BackupErrorCode.ERR_EMPTY_OR_CORRUPTED,
                    failedStage = RestoreStage.READING_FILE,
                    technicalDetails = "Input JSON string is empty or blank (0 bytes)."
                )
            )
        }

        onStageUpdate(RestoreStage.VALIDATING_STRUCTURE, 10, 100, "Parsing JSON structure...")
        val root = try {
            JSONObject(jsonString.trim())
        } catch (e: JSONException) {
            throw BackupRestoreException(
                BackupDiagnosticError(
                    errorCode = BackupErrorCode.ERR_INVALID_JSON,
                    failedStage = RestoreStage.VALIDATING_STRUCTURE,
                    technicalDetails = "JSON syntax error: ${e.message?.take(160) ?: "Malformed JSON"}"
                ),
                e
            )
        }

        // Verify that the JSON has at least one recognizable wallet collection
        val hasRecognizableKeys = root.has("accounts") ||
            root.has("wallets") ||
            root.has("accountList") ||
            root.has("walletList") ||
            root.has("archivedWallets") ||
            root.has("transactions") ||
            root.has("expenses") ||
            root.has("records") ||
            root.has("transactionList") ||
            root.has("categories") ||
            root.has("expenseCategories") ||
            root.has("budgets") ||
            root.has("debts") ||
            root.has("loans")

        if (!hasRecognizableKeys) {
            throw BackupRestoreException(
                BackupDiagnosticError(
                    errorCode = BackupErrorCode.ERR_MISSING_REQUIRED_FIELDS,
                    failedStage = RestoreStage.VALIDATING_STRUCTURE,
                    technicalDetails = "Root JSON object does not contain any valid wallet collections (accounts, wallets, transactions, expenses, categories, budgets, debts)."
                )
            )
        }

        onStageUpdate(RestoreStage.DETECTING_SCHEMA, 20, 100, "Detecting backup source and schema...")
        val (formatType, detectedVersion) = detectFormatAndVersion(root)

        val payload = when (formatType) {
            BackupFormatType.EXPEND_LEGACY -> {
                onStageUpdate(RestoreStage.MIGRATING_LEGACY, 30, 100, "Migrating Legacy Expend backup...")
                migrateLegacyExpendToCurrent(root, detectedVersion, onStageUpdate)
            }
            BackupFormatType.MY_WALLET_LEGACY -> {
                onStageUpdate(RestoreStage.MIGRATING_LEGACY, 30, 100, "Migrating Legacy My Wallet v0 schema...")
                migrateLegacyMyWalletV0ToCurrent(root, onStageUpdate)
            }
            BackupFormatType.MY_WALLET_CONVERTED_EXPEND,
            BackupFormatType.CSV_EXPORT,
            BackupFormatType.MY_WALLET_CURRENT -> {
                if (formatType == BackupFormatType.MY_WALLET_CONVERTED_EXPEND) {
                    onStageUpdate(RestoreStage.MIGRATING_LEGACY, 30, 100, "Normalizing converted Expend records...")
                }
                parseCurrentMyWalletSchema(root, formatType, detectedVersion, onStageUpdate)
            }
            BackupFormatType.UNKNOWN -> {
                throw BackupRestoreException(
                    BackupDiagnosticError(
                        errorCode = BackupErrorCode.ERR_MISSING_REQUIRED_FIELDS,
                        failedStage = RestoreStage.DETECTING_SCHEMA,
                        technicalDetails = "Unrecognized backup format."
                    )
                )
            }
        }

        onStageUpdate(
            RestoreStage.VALIDATING_REFERENCES,
            50,
            100,
            "Validating accounts and referential integrity..."
        )
        validatePayloadIntegrity(payload, existingAccountIds)

        return payload
    }

    fun detectFormatAndVersion(root: JSONObject): Pair<BackupFormatType, Int> {
        val rawAppName = root.optString("appName", root.optString("sourceApp", "")).trim()
        val hasSchemaVersionKey = root.has("schemaVersion")
        val schemaVersion = root.optInt("schemaVersion", -1)
        val legacyVersion = root.optInt("version", root.optInt("backupVersion", -1))

        val isExpendByKeys = rawAppName.contains("Expend", ignoreCase = true) ||
            root.has("wallets") ||
            root.has("expenses") ||
            root.has("records") ||
            root.has("expenseCategories") ||
            root.has("incomeCategories") ||
            root.has("loans")

        if (isExpendByKeys && !rawAppName.equals("My Wallet", ignoreCase = true) && !rawAppName.equals("Hishab Pati", ignoreCase = true)) {
            val effectiveExpendVer = if (legacyVersion != -1) legacyVersion else if (schemaVersion != -1) schemaVersion else 1
            if (effectiveExpendVer < 0 || effectiveExpendVer > MAX_SUPPORTED_EXPEND_VERSION) {
                throw BackupRestoreException(
                    BackupDiagnosticError(
                        errorCode = BackupErrorCode.ERR_UNSUPPORTED_VERSION,
                        failedStage = RestoreStage.DETECTING_SCHEMA,
                        technicalDetails = "Unsupported Expend schema version: $effectiveExpendVer (supported: 1..$MAX_SUPPORTED_EXPEND_VERSION)"
                    )
                )
            }
            return BackupFormatType.EXPEND_LEGACY to effectiveExpendVer
        }

        if (hasSchemaVersionKey) {
            if (schemaVersion < 0 || schemaVersion > MAX_SUPPORTED_SCHEMA_VERSION) {
                throw BackupRestoreException(
                    BackupDiagnosticError(
                        errorCode = BackupErrorCode.ERR_UNSUPPORTED_VERSION,
                        failedStage = RestoreStage.DETECTING_SCHEMA,
                        technicalDetails = "Unsupported schemaVersion: $schemaVersion (supported: 1..$MAX_SUPPORTED_SCHEMA_VERSION)"
                    )
                )
            }
            if (schemaVersion == 0) {
                return BackupFormatType.MY_WALLET_LEGACY to 0
            }
            // Check if this backup contains converted Expend records
            val txArr = root.optJSONArray("transactions")
            val comment = (root.optString("_comment", "") + " " + root.optString("comment", "") + " " + root.optString("note", "")).lowercase(Locale.US)
            val isExplicitlyConverted = comment.contains("converted") || comment.contains("expend") || rawAppName.contains("expend", ignoreCase = true)
            var hasConvertedMarkers = isExplicitlyConverted
            if (!hasConvertedMarkers && txArr != null) {
                for (i in 0 until minOf(txArr.length(), 100)) {
                    val txObj = txArr.optJSONObject(i) ?: continue
                    val id = txObj.optString("id", "")
                    if (id.startsWith("exp_") || id.startsWith("rec_") || id.startsWith("trf_")) {
                        hasConvertedMarkers = true
                        break
                    }
                    if (txObj.has("walletId") || txObj.has("toWalletId") || txObj.has("destinationWalletId") || txObj.has("sourceWalletId")) {
                        hasConvertedMarkers = true
                        break
                    }
                }
            }
            if (!hasConvertedMarkers) {
                val accArr = root.optJSONArray("accounts")
                if (accArr != null) {
                    for (i in 0 until minOf(accArr.length(), 50)) {
                        val accObj = accArr.optJSONObject(i) ?: continue
                        val id = accObj.optString("id", "")
                        if (id.startsWith("w_") || id.startsWith("exp_")) {
                            hasConvertedMarkers = true
                            break
                        }
                    }
                }
            }
            return if (hasConvertedMarkers) {
                BackupFormatType.MY_WALLET_CONVERTED_EXPEND to schemaVersion
            } else {
                BackupFormatType.MY_WALLET_CURRENT to schemaVersion
            }
        }

        // No explicit schemaVersion key
        if (rawAppName.equals("My Wallet", ignoreCase = true) ||
            rawAppName.equals("Hishab Pati", ignoreCase = true) ||
            (root.has("accounts") && root.has("transactions"))
        ) {
            if (legacyVersion > MAX_SUPPORTED_SCHEMA_VERSION) {
                throw BackupRestoreException(
                    BackupDiagnosticError(
                        errorCode = BackupErrorCode.ERR_UNSUPPORTED_VERSION,
                        failedStage = RestoreStage.DETECTING_SCHEMA,
                        technicalDetails = "Unsupported version: $legacyVersion"
                    )
                )
            }
            return BackupFormatType.MY_WALLET_LEGACY to 0
        }

        return BackupFormatType.UNKNOWN to -1
    }

    data class AccountLookup(
        val accounts: List<AccountEntity>,
        val idMap: Map<String, String>,
        val nameMap: Map<String, String>,
        val normalizedNameMap: Map<String, String>,
        val last4Map: Map<String, String>,
        val institutionMap: Map<String, String>
    )

    private fun buildAccountLookup(accounts: List<AccountEntity>, rawWallets: JSONArray): AccountLookup {
        val idMap = mutableMapOf<String, String>()
        val nameMap = mutableMapOf<String, String>()
        val normalizedNameMap = mutableMapOf<String, String>()
        val last4Counts = mutableMapOf<String, Int>()
        val instCounts = mutableMapOf<String, Int>()

        for (acc in accounts) {
            idMap[acc.id] = acc.id
            idMap[acc.id.lowercase(Locale.US)] = acc.id
            if (acc.id.startsWith("exp_acc_")) {
                idMap[acc.id.removePrefix("exp_acc_")] = acc.id
            }
            if (acc.id.startsWith("acc_")) {
                idMap[acc.id.removePrefix("acc_")] = acc.id
            }
            if (acc.id.startsWith("w_")) {
                idMap[acc.id.removePrefix("w_")] = acc.id
            }

            val lowerName = acc.name.lowercase(Locale.US).trim()
            nameMap[lowerName] = acc.id
            val norm = lowerName.replace(Regex("[^a-z0-9]"), "")
            if (norm.isNotBlank()) normalizedNameMap[norm] = acc.id

            if (acc.accountNumberLast4.length == 4) {
                last4Counts[acc.accountNumberLast4] = (last4Counts[acc.accountNumberLast4] ?: 0) + 1
            }
            if (acc.institution.isNotBlank()) {
                val instKey = acc.institution.lowercase(Locale.US).trim()
                instCounts[instKey] = (instCounts[instKey] ?: 0) + 1
            }
        }

        // Also map raw identifiers from the JSON elements
        for (i in 0 until rawWallets.length()) {
            val w = rawWallets.optJSONObject(i) ?: continue
            val rawId = w.optString("id", w.optString("walletId", w.optString("accountId", ""))).trim()
            val rawName = w.optString("name", w.optString("walletName", w.optString("title", ""))).trim()
            val matchedAcc = accounts.firstOrNull { it.id == rawId || it.name.equals(rawName, ignoreCase = true) }
            if (matchedAcc != null) {
                if (rawId.isNotBlank()) {
                    idMap[rawId] = matchedAcc.id
                    idMap[rawId.lowercase(Locale.US)] = matchedAcc.id
                }
                val otherId = w.optString("walletId", "").trim()
                if (otherId.isNotBlank()) idMap[otherId] = matchedAcc.id
                val otherAccId = w.optString("accountId", "").trim()
                if (otherAccId.isNotBlank()) idMap[otherAccId] = matchedAcc.id
            }
        }

        val last4Map = mutableMapOf<String, String>()
        for (acc in accounts) {
            if (acc.accountNumberLast4.length == 4 && last4Counts[acc.accountNumberLast4] == 1) {
                last4Map[acc.accountNumberLast4] = acc.id
            }
        }

        val institutionMap = mutableMapOf<String, String>()
        for (acc in accounts) {
            val instKey = acc.institution.lowercase(Locale.US).trim()
            if (instKey.isNotBlank() && instCounts[instKey] == 1) {
                institutionMap[instKey] = acc.id
            }
        }

        return AccountLookup(
            accounts = accounts,
            idMap = idMap,
            nameMap = nameMap,
            normalizedNameMap = normalizedNameMap,
            last4Map = last4Map,
            institutionMap = institutionMap
        )
    }

    fun resolveAccount(
        ref: String?,
        titleHint: String? = null,
        notesHint: String? = null,
        payeeHint: String? = null,
        lookup: AccountLookup,
        isDestination: Boolean = false
    ): String? {
        if (!ref.isNullOrBlank()) {
            val trimmed = ref.trim()
            if (!trimmed.equals("null", ignoreCase = true) && !trimmed.equals("undefined", ignoreCase = true)) {
                // 1. Direct ID match
                lookup.idMap[trimmed]?.let { return it }
                lookup.idMap[trimmed.lowercase(Locale.US)]?.let { return it }

                // 2. Direct name match
                lookup.nameMap[trimmed.lowercase(Locale.US)]?.let { return it }

                // 3. Normalized alphanumeric match
                val norm = trimmed.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
                if (norm.isNotBlank()) {
                    lookup.normalizedNameMap[norm]?.let { return it }
                }

                // 4. Substring / Prefix match on account names if unique
                val matchingAccs = lookup.accounts.filter { acc ->
                    val accLower = acc.name.lowercase(Locale.US)
                    val trimLower = trimmed.lowercase(Locale.US)
                    accLower == trimLower || accLower.startsWith(trimLower) || trimLower.startsWith(accLower) ||
                        accLower.contains(trimLower) || trimLower.contains(accLower)
                }
                if (matchingAccs.size == 1) {
                    return matchingAccs.first().id
                }

                // 5. Last 4 digits match
                val digits = trimmed.filter { it.isDigit() }
                if (digits.length >= 4) {
                    val last4 = digits.takeLast(4)
                    lookup.last4Map[last4]?.let { return it }
                }

                // 6. Institution match
                lookup.institutionMap[trimmed.lowercase(Locale.US)]?.let { return it }

                // Return trimmed ID (can be validated or recovered if it was an unlisted wallet)
                return trimmed
            }
        }

        // When reference is missing/null, attempt to extract explicit destination account from text hints
        if (isDestination) {
            val combinedHints = listOfNotNull(titleHint, notesHint, payeeHint).joinToString(" ").trim()
            if (combinedHints.isNotBlank()) {
                // Check if any existing account name is explicitly named in the text (longest name first)
                for (acc in lookup.accounts.sortedByDescending { it.name.length }) {
                    val accName = acc.name.trim()
                    if (accName.length >= 3 && combinedHints.contains(accName, ignoreCase = true)) {
                        return acc.id
                    }
                }
                // Check common Bangladeshi wallet keywords
                val hintLower = combinedHints.lowercase(Locale.US)
                val bkashAcc = lookup.accounts.firstOrNull { it.name.contains("bkash", ignoreCase = true) || it.type == AccountType.MFS }
                if (hintLower.contains("bkash") && bkashAcc != null) {
                    return bkashAcc.id
                }
                val nagadAcc = lookup.accounts.firstOrNull { it.name.contains("nagad", ignoreCase = true) }
                if (hintLower.contains("nagad") && nagadAcc != null) {
                    return nagadAcc.id
                }
                val cashAcc = lookup.accounts.firstOrNull { it.name.contains("cash", ignoreCase = true) || it.type == AccountType.CASH }
                if ((hintLower.contains("cash") || hintLower.contains("pocket") || hintLower.contains("atm")) && cashAcc != null) {
                    return cashAcc.id
                }
                val bankAcc = lookup.accounts.firstOrNull { it.type == AccountType.BANK }
                if ((hintLower.contains("bank") || hintLower.contains("salary")) && bankAcc != null) {
                    return bankAcc.id
                }
                val cardAcc = lookup.accounts.firstOrNull { it.type == AccountType.CREDIT_CARD }
                if ((hintLower.contains("card") || hintLower.contains("visa") || hintLower.contains("mastercard")) && cardAcc != null) {
                    return cardAcc.id
                }
            }
        }

        return null
    }

    private data class ParsedWalletsResult(
        val accounts: List<AccountEntity>,
        val explicitBalanceIds: Set<String>,
        val lookup: AccountLookup,
        val skippedDuplicates: Int,
        val migratedCount: Int
    )

    private fun parseWalletsAndAccounts(root: JSONObject, isExpend: Boolean): ParsedWalletsResult {
        val rawWallets = JSONArray()
        val walletArrays = listOfNotNull(
            root.optJSONArray("wallets"),
            root.optJSONArray("accounts"),
            root.optJSONArray("accountList"),
            root.optJSONArray("walletList"),
            root.optJSONArray("archivedWallets"),
            root.optJSONArray("hiddenWallets"),
            root.optJSONArray("inactiveWallets"),
            root.optJSONArray("deletedWallets")
        )
        for (arr in walletArrays) {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                rawWallets.put(item)
            }
        }

        val accounts = mutableListOf<AccountEntity>()
        val explicitBalanceIds = mutableSetOf<String>()
        val seenAccountIds = mutableSetOf<String>()
        var skippedDuplicates = 0
        var migratedCount = 0

        for (i in 0 until rawWallets.length()) {
            val w = rawWallets.optJSONObject(i) ?: continue
            val rawId = w.optString("id", w.optString("walletId", w.optString("accountId", ""))).trim()
            val name = w.optString("name", w.optString("walletName", w.optString("title", ""))).trim()

            if (rawId.isBlank() && name.isBlank()) {
                if (isExpend) {
                    throw BackupRestoreException(
                        BackupDiagnosticError(
                            errorCode = BackupErrorCode.ERR_MISSING_REQUIRED_FIELDS,
                            failedStage = RestoreStage.MIGRATING_LEGACY,
                            technicalDetails = "Account/Wallet at index $i is missing both id and name."
                        )
                    )
                } else {
                    throw BackupRestoreException(
                        BackupDiagnosticError(
                            errorCode = BackupErrorCode.ERR_MISSING_REQUIRED_FIELDS,
                            failedStage = RestoreStage.VALIDATING_REFERENCES,
                            technicalDetails = "Account at index $i is missing required 'id' or 'name'."
                        )
                    )
                }
            }

            val id = rawId.ifBlank {
                val clean = name.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_')
                if (isExpend) "exp_acc_$clean" else "acc_$clean"
            }

            if (!seenAccountIds.add(id)) {
                skippedDuplicates++
                continue
            }

            val effectiveName = name.ifBlank { id }
            val typeStr = w.optString("type", w.optString("walletType", "OTHER")).uppercase(Locale.US)
            val accountType = mapAccountType(typeStr, effectiveName)

            val hasBalance = w.has("balance") || w.has("currentBalance")
            val rawBalance = parseSafeDouble(w, listOf("balance", "currentBalance"), 0.0, "account[$id].balance")
            val openingBalance = parseSafeDouble(
                w,
                listOf("openingBalance", "initialAmount", "startingBalance", "initialBalance", "startingAmount"),
                if (hasBalance) rawBalance else 0.0,
                "account[$id].openingBalance"
            )
            val outstanding = parseSafeDouble(w, listOf("outstandingBalance", "usedLimit", "currentDebt"), 0.0, "account[$id].outstanding")
            val finalBalance = if (accountType == AccountType.CREDIT_CARD && rawBalance == 0.0 && outstanding > 0.0) {
                -abs(outstanding)
            } else {
                rawBalance
            }

            if (hasBalance || outstanding > 0.0) {
                explicitBalanceIds.add(id)
            }

            val isHidden = w.optBoolean("isHidden", w.optBoolean("archived", w.optBoolean("isArchived", false)))
            val acc = AccountEntity(
                id = id,
                name = effectiveName,
                type = accountType,
                institution = w.optString("institution", w.optString("bankName", w.optString("bank", ""))),
                balance = finalBalance,
                openingBalance = openingBalance,
                accountNumberLast4 = extractLast4(w.optString("accountNumberLast4", w.optString("accountNumber", w.optString("cardNumber", "")))),
                colorHex = normalizeColorHex(w.optString("colorHex", w.optString("color", "#4F46E5")), "#4F46E5"),
                iconName = mapIconName(w.optString("iconName", w.optString("icon", "")), accountType),
                isHidden = isHidden,
                sortOrder = w.optInt("sortOrder", accounts.size),
                creditLimit = parseSafeDouble(w, listOf("creditLimit", "limit", "maxLimit"), 0.0, "account[$id].creditLimit"),
                billingDate = w.optString("billingDate", w.optString("billDate", "")),
                paymentDueDate = w.optString("paymentDueDate", w.optString("dueDate", "")),
                minPayment = parseSafeDouble(w, listOf("minPayment", "minimumPayment"), 0.0, "account[$id].minPayment"),
                notes = w.optString("notes", w.optString("description", w.optString("memo", ""))),
                includeInTotal = w.optBoolean("includeInTotal", true)
            )
            accounts.add(acc)
            migratedCount++
        }

        val lookup = buildAccountLookup(accounts, rawWallets)
        return ParsedWalletsResult(accounts, explicitBalanceIds, lookup, skippedDuplicates, migratedCount)
    }

    private fun migrateLegacyExpendToCurrent(
        root: JSONObject,
        version: Int,
        onStageUpdate: (RestoreStage, Int, Int, String) -> Unit
    ): NormalizedBackupPayload {
        val backupDate = root.optString(
            "backupDate",
            root.optString("exportDate", root.optString("createdAt", ""))
        )
        var migratedCount = 0
        var skippedDuplicates = 0

        val settings = parseSettings(root.optJSONObject("settings"), root.optString("currency", "BDT"))

        // 1. Wallets / Accounts
        val walletsResult = parseWalletsAndAccounts(root, isExpend = true)
        val mutableAccounts = walletsResult.accounts.toMutableList()
        val explicitBalanceIds = walletsResult.explicitBalanceIds.toMutableSet()
        var lookup = walletsResult.lookup
        skippedDuplicates += walletsResult.skippedDuplicates
        migratedCount += walletsResult.migratedCount

        // 2. Categories & Subcategories
        onStageUpdate(RestoreStage.PROCESSING_CATEGORIES, 40, 100, "Migrating categories and subcategories...")
        val (categories, subcategories, catIdToName, subIdToName, catDupes) = parseAndMigrateCategories(root)
        skippedDuplicates += catDupes
        migratedCount += categories.size + subcategories.size

        // 3. Transactions / Expenses / Records
        onStageUpdate(RestoreStage.PROCESSING_TRANSACTIONS, 45, 100, "Migrating transactions...")
        val rawTxs = root.optJSONArray("transactions")
            ?: root.optJSONArray("expenses")
            ?: root.optJSONArray("records")
            ?: root.optJSONArray("transactionList")
            ?: JSONArray()

        val (transactions, txDupes, recoveredAccounts) = parseAndMigrateTransactions(
            rawTxs = rawTxs,
            lookup = lookup,
            catIdToName = catIdToName,
            subIdToName = subIdToName,
            categories = categories,
            subcategories = subcategories,
            isLegacyOrConverted = true
        )
        skippedDuplicates += txDupes
        migratedCount += transactions.size

        if (recoveredAccounts.isNotEmpty()) {
            mutableAccounts.addAll(recoveredAccounts)
            lookup = buildAccountLookup(mutableAccounts, JSONArray())
        }

        // Templates / recurring transactions in Expend
        val (templateTxs, templateDupes) = parseTemplates(
            rawTemplates = root.optJSONArray("templates") ?: root.optJSONArray("recurringTemplates") ?: root.optJSONArray("quickTemplates"),
            lookup = lookup,
            catIdToName = catIdToName
        )
        skippedDuplicates += templateDupes
        val allTransactions = transactions + templateTxs

        // Compute balances for accounts without explicit balance in backup
        val finalAccounts = mutableAccounts.map { acc ->
            if (explicitBalanceIds.contains(acc.id)) {
                acc
            } else {
                val delta = allTransactions.sumOf { FinancialIntegrityValidator.computeSingleTxBalanceDelta(it, acc.id) }
                acc.copy(balance = acc.openingBalance + delta)
            }
        }

        // 4. Budgets & Debts
        onStageUpdate(RestoreStage.PROCESSING_BUDGETS_DEBTS, 48, 100, "Migrating budgets and debts...")
        val (budgets, bgtDupes) = parseAndMigrateBudgets(
            rawBudgets = root.optJSONArray("budgets") ?: root.optJSONArray("budgetList"),
            catIdToName = catIdToName
        )
        skippedDuplicates += bgtDupes
        migratedCount += budgets.size

        val (debts, debtDupes) = parseAndMigrateDebts(
            rawDebts = root.optJSONArray("debts") ?: root.optJSONArray("loans") ?: root.optJSONArray("debtList"),
            lookup = lookup
        )
        skippedDuplicates += debtDupes
        migratedCount += debts.size

        val (goals, goalDupes) = parseSavingsGoals(root.optJSONArray("savingsGoals") ?: root.optJSONArray("goals"))
        skippedDuplicates += goalDupes

        return NormalizedBackupPayload(
            appName = root.optString("appName", "Expend"),
            schemaVersion = version,
            formatType = BackupFormatType.EXPEND_LEGACY,
            backupDate = backupDate,
            settings = settings,
            accounts = finalAccounts,
            accountsHaveExplicitBalance = explicitBalanceIds,
            categories = categories,
            subcategories = subcategories,
            transactions = allTransactions,
            budgets = budgets,
            debts = debts,
            savingsGoals = goals,
            skippedDuplicatesCount = skippedDuplicates,
            migratedRecordsCount = migratedCount
        )
    }

    private fun migrateLegacyMyWalletV0ToCurrent(
        root: JSONObject,
        onStageUpdate: (RestoreStage, Int, Int, String) -> Unit
    ): NormalizedBackupPayload {
        val payload = parseCurrentMyWalletSchema(root, BackupFormatType.MY_WALLET_LEGACY, 0, onStageUpdate)
        val totalMigrated = payload.accounts.size + payload.transactions.size +
            payload.categories.size + payload.subcategories.size + payload.budgets.size + payload.debts.size
        return payload.copy(migratedRecordsCount = totalMigrated)
    }

    private fun parseCurrentMyWalletSchema(
        root: JSONObject,
        formatType: BackupFormatType,
        schemaVersion: Int,
        onStageUpdate: (RestoreStage, Int, Int, String) -> Unit
    ): NormalizedBackupPayload {
        val appName = root.optString("appName", "Hishab Pati")
        val backupDate = root.optString("backupDate", "")
        var skippedDuplicates = 0
        var migratedCount = 0

        val settings = parseSettings(root.optJSONObject("settings"), root.optString("currency", "BDT"))

        // 1. Accounts
        val walletsResult = parseWalletsAndAccounts(root, isExpend = false)
        val mutableAccounts = walletsResult.accounts.toMutableList()
        val explicitBalanceIds = walletsResult.explicitBalanceIds.toMutableSet()
        var lookup = walletsResult.lookup
        skippedDuplicates += walletsResult.skippedDuplicates

        // 2. Categories & Subcategories
        onStageUpdate(RestoreStage.PROCESSING_CATEGORIES, 40, 100, "Processing categories and subcategories...")
        val (categories, subcategories, catIdToName, subIdToName, catDupes) = parseAndMigrateCategories(root)
        skippedDuplicates += catDupes

        // 3. Transactions
        onStageUpdate(RestoreStage.PROCESSING_TRANSACTIONS, 45, 100, "Processing transactions...")
        val txArr = root.optJSONArray("transactions") ?: JSONArray()
        val isLegacyOrConverted = (formatType == BackupFormatType.MY_WALLET_CONVERTED_EXPEND || formatType == BackupFormatType.MY_WALLET_LEGACY)
        val (transactions, txDupes, recoveredAccounts) = parseAndMigrateTransactions(
            rawTxs = txArr,
            lookup = lookup,
            catIdToName = catIdToName,
            subIdToName = subIdToName,
            categories = categories,
            subcategories = subcategories,
            isLegacyOrConverted = isLegacyOrConverted
        )
        skippedDuplicates += txDupes

        if (recoveredAccounts.isNotEmpty()) {
            mutableAccounts.addAll(recoveredAccounts)
            lookup = buildAccountLookup(mutableAccounts, JSONArray())
        }

        if (formatType == BackupFormatType.MY_WALLET_CONVERTED_EXPEND) {
            migratedCount += transactions.count { it.id.startsWith("exp_") || it.id.startsWith("rec_") }
        }

        // Templates
        val (templateTxs, templateDupes) = parseTemplates(
            rawTemplates = root.optJSONArray("templates"),
            lookup = lookup,
            catIdToName = catIdToName
        )
        skippedDuplicates += templateDupes
        val allTransactions = transactions + templateTxs

        // 4. Budgets
        onStageUpdate(RestoreStage.PROCESSING_BUDGETS_DEBTS, 48, 100, "Processing budgets and debts...")
        val (budgets, bgtDupes) = parseAndMigrateBudgets(
            rawBudgets = root.optJSONArray("budgets"),
            catIdToName = catIdToName
        )
        skippedDuplicates += bgtDupes

        // 5. Debts
        val (debts, debtDupes) = parseAndMigrateDebts(
            rawDebts = root.optJSONArray("debts") ?: root.optJSONArray("loans"),
            lookup = lookup
        )
        skippedDuplicates += debtDupes

        // 6. Savings Goals
        val (goals, goalDupes) = parseSavingsGoals(root.optJSONArray("savingsGoals"))
        skippedDuplicates += goalDupes

        val finalAccounts = mutableAccounts.map { acc ->
            if (explicitBalanceIds.contains(acc.id)) {
                acc
            } else {
                val delta = allTransactions.sumOf { FinancialIntegrityValidator.computeSingleTxBalanceDelta(it, acc.id) }
                acc.copy(balance = acc.openingBalance + delta)
            }
        }

        return NormalizedBackupPayload(
            appName = appName,
            schemaVersion = if (schemaVersion <= 0) CURRENT_SCHEMA_VERSION else schemaVersion,
            formatType = formatType,
            backupDate = backupDate,
            settings = settings,
            accounts = finalAccounts,
            accountsHaveExplicitBalance = explicitBalanceIds,
            categories = categories,
            subcategories = subcategories,
            transactions = allTransactions,
            budgets = budgets,
            debts = debts,
            savingsGoals = goals,
            skippedDuplicatesCount = skippedDuplicates,
            migratedRecordsCount = migratedCount
        )
    }

    private data class ParsedCategoriesResult(
        val categories: MutableList<CategoryEntity>,
        val subcategories: MutableList<SubcategoryEntity>,
        val catIdToName: MutableMap<String, String>,
        val subIdToName: MutableMap<String, String>,
        val skippedDuplicates: Int
    )

    private fun parseAndMigrateCategories(root: JSONObject): ParsedCategoriesResult {
        val categories = mutableListOf<CategoryEntity>()
        val subcategories = mutableListOf<SubcategoryEntity>()
        val catIdToName = mutableMapOf<String, String>()
        val catNameToId = mutableMapOf<String, String>()
        val subIdToName = mutableMapOf<String, String>()
        val seenCatIds = mutableSetOf<String>()
        val seenCatNames = mutableSetOf<String>()
        val seenSubIds = mutableSetOf<String>()
        val seenSubKeys = mutableSetOf<String>()
        var skippedDuplicates = 0

        // Seed lookup with default categories
        val (defaultCats, defaultSubs) = InitialData.getDefaultCategories()
        defaultCats.forEach { dc ->
            catIdToName[dc.id] = dc.name
            catNameToId[dc.name.lowercase(Locale.US)] = dc.id
        }
        defaultSubs.forEach { ds ->
            subIdToName[ds.id] = ds.name
        }

        val rawCatArrays = listOfNotNull(
            root.optJSONArray("categories"),
            root.optJSONArray("expenseCategories"),
            root.optJSONArray("incomeCategories"),
            root.optJSONArray("categoryList")
        )

        for (arr in rawCatArrays) {
            val isIncomeArray = arr === root.optJSONArray("incomeCategories")
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                val rawName = c.optString("name", c.optString("title", c.optString("categoryName", ""))).trim()
                val rawId = c.optString("id", c.optString("categoryId", "")).trim()
                if (rawName.isBlank()) continue

                val id = rawId.ifBlank { "cat_${rawName.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_")}" }
                catIdToName[id] = rawName
                if (rawId.isNotBlank()) catIdToName[rawId] = rawName

                val normNameKey = rawName.lowercase(Locale.US)
                if (!seenCatIds.add(id) || !seenCatNames.add(normNameKey)) {
                    val existingId = catNameToId[normNameKey] ?: id
                    catIdToName[id] = rawName
                    extractNestedSubcategories(c, existingId, subcategories, subIdToName, seenSubIds, seenSubKeys)
                    skippedDuplicates++
                    continue
                }

                catNameToId[normNameKey] = id
                val isIncome = when {
                    c.has("isIncome") -> c.optBoolean("isIncome", false)
                    c.has("type") -> c.optString("type", "").equals("INCOME", ignoreCase = true)
                    else -> isIncomeArray
                }
                val isTransfer = c.optBoolean(
                    "isTransfer",
                    rawName.equals("Internal Transfer", ignoreCase = true) || rawName.equals("Transfer", ignoreCase = true)
                )

                categories.add(
                    CategoryEntity(
                        id = id,
                        name = rawName,
                        nameBn = c.optString("nameBn", ""),
                        isIncome = isIncome,
                        isTransfer = isTransfer,
                        isActive = c.optBoolean("isActive", true),
                        iconName = c.optString("iconName", c.optString("icon", "category")).ifBlank { "category" },
                        colorHex = normalizeColorHex(c.optString("colorHex", c.optString("color", "#4F46E5")), "#4F46E5"),
                        sortOrder = c.optInt("sortOrder", categories.size)
                    )
                )

                extractNestedSubcategories(c, id, subcategories, subIdToName, seenSubIds, seenSubKeys)
            }
        }

        val subArr = root.optJSONArray("subcategories") ?: root.optJSONArray("subcategoryList")
        if (subArr != null) {
            for (i in 0 until subArr.length()) {
                val s = subArr.optJSONObject(i) ?: continue
                val rawName = s.optString("name", s.optString("subcategoryName", "")).trim()
                val rawId = s.optString("id", s.optString("subcategoryId", "")).trim()
                val rawCatId = s.optString("categoryId", s.optString("parentId", "")).trim()
                if (rawName.isBlank()) continue

                val resolvedCatName = catIdToName[rawCatId] ?: rawCatId
                val resolvedCatId = catNameToId[resolvedCatName.lowercase(Locale.US)]
                    ?: rawCatId.ifBlank { "cat_misc" }

                val id = rawId.ifBlank { "${resolvedCatId}_sub_${subcategories.size}" }
                subIdToName[id] = rawName
                if (rawId.isNotBlank()) subIdToName[rawId] = rawName

                val subKey = "${resolvedCatId.lowercase(Locale.US)}::${rawName.lowercase(Locale.US)}"
                if (!seenSubIds.add(id) || !seenSubKeys.add(subKey)) {
                    skippedDuplicates++
                    continue
                }

                subcategories.add(
                    SubcategoryEntity(
                        id = id,
                        categoryId = resolvedCatId,
                        name = rawName,
                        nameBn = s.optString("nameBn", ""),
                        sortOrder = s.optInt("sortOrder", i)
                    )
                )
            }
        }

        return ParsedCategoriesResult(categories, subcategories, catIdToName, subIdToName, skippedDuplicates)
    }

    private fun extractNestedSubcategories(
        catObj: JSONObject,
        parentCategoryId: String,
        subcategories: MutableList<SubcategoryEntity>,
        subIdToName: MutableMap<String, String>,
        seenSubIds: MutableSet<String>,
        seenSubKeys: MutableSet<String>
    ) {
        val nested = catObj.optJSONArray("subcategories") ?: catObj.optJSONArray("children") ?: return
        for (j in 0 until nested.length()) {
            val item = nested.opt(j)
            when (item) {
                is JSONObject -> {
                    val sName = item.optString("name", item.optString("title", "")).trim()
                    if (sName.isBlank()) continue
                    val sId = item.optString("id", "${parentCategoryId}_sub_$j").trim()
                    subIdToName[sId] = sName
                    val subKey = "${parentCategoryId.lowercase(Locale.US)}::${sName.lowercase(Locale.US)}"
                    if (seenSubIds.add(sId) && seenSubKeys.add(subKey)) {
                        subcategories.add(
                            SubcategoryEntity(
                                id = sId,
                                categoryId = parentCategoryId,
                                name = sName,
                                nameBn = item.optString("nameBn", ""),
                                sortOrder = item.optInt("sortOrder", j)
                            )
                        )
                    }
                }
                is String -> {
                    val sName = item.trim()
                    if (sName.isBlank()) continue
                    val sId = "${parentCategoryId}_sub_$j"
                    subIdToName[sId] = sName
                    val subKey = "${parentCategoryId.lowercase(Locale.US)}::${sName.lowercase(Locale.US)}"
                    if (seenSubIds.add(sId) && seenSubKeys.add(subKey)) {
                        subcategories.add(
                            SubcategoryEntity(
                                id = sId,
                                categoryId = parentCategoryId,
                                name = sName,
                                sortOrder = j
                            )
                        )
                    }
                }
            }
        }
    }

    private data class ParsedTransactionsResult(
        val transactions: List<TransactionEntity>,
        val skippedDuplicates: Int,
        val recoveredAccounts: List<AccountEntity>
    )

    private fun parseAndMigrateTransactions(
        rawTxs: JSONArray,
        lookup: AccountLookup,
        catIdToName: Map<String, String>,
        subIdToName: Map<String, String>,
        categories: MutableList<CategoryEntity>,
        subcategories: MutableList<SubcategoryEntity>,
        isLegacyOrConverted: Boolean = false
    ): ParsedTransactionsResult {
        val parsedList = mutableListOf<TransactionEntity>()
        val seenIds = mutableSetOf<String>()
        val recoveredAccounts = mutableListOf<AccountEntity>()
        val knownAccountIds = lookup.accounts.map { it.id }.toMutableSet()
        var skippedDuplicates = 0

        val catNameSet = categories.map { it.name.lowercase(Locale.US) }.toMutableSet()
        val catNameToId = categories.associate { it.name.lowercase(Locale.US) to it.id }.toMutableMap()
        val subKeySet = subcategories.map { "${it.categoryId.lowercase(Locale.US)}::${it.name.lowercase(Locale.US)}" }.toMutableSet()

        // Pass 1: Raw transaction pre-parsing for paired transfer detection
        data class RawTxInfo(
            val index: Int,
            val json: JSONObject,
            val id: String,
            val amount: Double,
            val typeStr: String,
            val rawSrc: String?,
            val rawDst: String?,
            val timestamp: Long,
            val title: String,
            val notes: String,
            val category: String,
            val subcategory: String,
            val payee: String,
            val tags: String,
            val linkId: String?
        )

        val preParsed = mutableListOf<RawTxInfo>()
        for (i in 0 until rawTxs.length()) {
            val t = rawTxs.optJSONObject(i) ?: continue
            val rawId = t.optString("id", t.optString("transactionId", t.optString("expenseId", t.optString("recordId", "")))).trim()

            if (rawId.isNotBlank() && !seenIds.add(rawId)) {
                skippedDuplicates++
                continue
            }

            if (!t.has("amount") && !t.has("value") && !t.has("total") && !t.has("cost")) {
                throw BackupRestoreException(
                    BackupDiagnosticError(
                        errorCode = BackupErrorCode.ERR_MISSING_REQUIRED_FIELDS,
                        failedStage = RestoreStage.PROCESSING_TRANSACTIONS,
                        technicalDetails = "Transaction at index $i (id='$rawId') is missing required 'amount' field."
                    )
                )
            }
            val rawAmount = parseSafeDouble(t, listOf("amount", "value", "total", "cost"), Double.NaN, "transaction[$rawId].amount")
            if (rawAmount.isNaN() || rawAmount.isInfinite() || rawAmount < 0.0) {
                throw BackupRestoreException(
                    BackupDiagnosticError(
                        errorCode = BackupErrorCode.ERR_INVALID_FINANCIAL_DATA,
                        failedStage = RestoreStage.PROCESSING_TRANSACTIONS,
                        technicalDetails = "Transaction '$rawId' has invalid amount: $rawAmount"
                    )
                )
            }

            val rawSrc = extractAccountReference(
                t,
                "accountId", "walletId", "fromAccountId", "fromWalletId", "sourceAccountId",
                "sourceWalletId", "fromAccount", "fromWallet", "sourceAccount", "sourceWallet",
                "senderWalletId", "senderAccountId", "srcWalletId", "srcAccountId", "wallet", "account"
            )

            val rawDst = extractAccountReference(
                t,
                "toAccountId", "toWalletId", "destinationAccountId", "destinationWalletId",
                "targetAccountId", "targetWalletId", "toAccount", "toWallet", "destinationAccount",
                "destinationWallet", "targetAccount", "targetWallet", "transferToAccountId",
                "transferToWalletId", "transferTo", "receiverAccountId", "receiverWalletId",
                "recipientAccountId", "recipientWalletId", "destAccountId", "destWalletId",
                "destination", "target", "to"
            )

            val rawCat = extractCategoryString(t, "category", "categoryName", "categoryId")
            if (rawCat.equals("INVALID_CATEGORY", ignoreCase = true)) {
                throw BackupRestoreException(
                    BackupDiagnosticError(
                        errorCode = BackupErrorCode.ERR_REFERENTIAL_INTEGRITY,
                        failedStage = RestoreStage.VALIDATING_REFERENCES,
                        technicalDetails = "Transaction '$rawId' references an invalid category '$rawCat'."
                    )
                )
            }
            val resolvedCategory = (catIdToName[rawCat] ?: rawCat).ifBlank { "Miscellaneous" }

            val rawSub = extractCategoryString(t, "subcategory", "subcategoryName", "subcategoryId", "subCategoryId")
            val resolvedSubcategory = (subIdToName[rawSub] ?: rawSub)

            val rawTypeStr = t.optString("type", t.optString("transactionType", t.optString("recordType", "EXPENSE"))).trim()
            val title = t.optString(
                "title",
                t.optString("name", t.optString("description", t.optString("note", resolvedCategory)))
            ).trim().ifBlank { resolvedCategory }

            val notes = t.optString("notes", t.optString("memo", t.optString("comment", t.optString("remark", "")))).trim()
            val payee = t.optString("payeeOrPayer", t.optString("person", t.optString("payee", t.optString("payer", t.optString("merchant", ""))))).trim()
            val tags = extractTagsString(t)
            val timestamp = parseTimestampMillis(t, i)

            val linkId = t.optString("transferId", t.optString("pairId", t.optString("linkId", t.optString("transferPairId", "")))).trim().takeIf { it.isNotBlank() }

            preParsed.add(
                RawTxInfo(
                    index = i,
                    json = t,
                    id = rawId,
                    amount = rawAmount,
                    typeStr = rawTypeStr,
                    rawSrc = rawSrc,
                    rawDst = rawDst,
                    timestamp = timestamp,
                    title = title,
                    notes = notes,
                    category = resolvedCategory,
                    subcategory = resolvedSubcategory,
                    payee = payee,
                    tags = tags,
                    linkId = linkId
                )
            )
        }

        // Pass 2: Paired transfer matching for legacy two-legged transfer records
        val matchedInboundIds = mutableSetOf<String>()
        val pairedDestinations = mutableMapOf<Int, String>()

        for (item in preParsed) {
            val isTransferLike = item.typeStr.equals("TRANSFER", ignoreCase = true) ||
                item.category.equals("Internal Transfer", ignoreCase = true) ||
                item.category.equals("Transfer", ignoreCase = true)

            if (isTransferLike && item.rawDst.isNullOrBlank()) {
                // Find matching counterpart in preParsed
                val counterpart = preParsed.firstOrNull { other ->
                    if (other.index == item.index || other.id in matchedInboundIds) return@firstOrNull false
                    val sameLink = item.linkId != null && item.linkId == other.linkId
                    val sameAmount = abs(item.amount - other.amount) < 0.01
                    val closeTime = abs(item.timestamp - other.timestamp) <= 600000L // within 10 mins
                    val diffAccount = item.rawSrc != other.rawSrc
                    val isInbound = other.typeStr.equals("INCOME", ignoreCase = true) ||
                        (other.typeStr.equals("TRANSFER", ignoreCase = true) && other.rawDst.isNullOrBlank())

                    sameLink || (sameAmount && closeTime && diffAccount && isInbound)
                }

                if (counterpart != null && !counterpart.rawSrc.isNullOrBlank()) {
                    pairedDestinations[item.index] = counterpart.rawSrc
                    matchedInboundIds.add(counterpart.id)
                }
            }
        }

        // Pass 3: Resolve accounts, synthesize missing referenced accounts, and convert transactions
        for (item in preParsed) {
            // Skip the second leg of paired transfers
            if (item.id in matchedInboundIds) {
                skippedDuplicates++
                continue
            }

            val t = item.json
            val rawId = item.id

            // Resolve source account
            if (item.rawSrc.isNullOrBlank()) {
                throw BackupRestoreException(
                    BackupDiagnosticError(
                        errorCode = BackupErrorCode.ERR_REFERENTIAL_INTEGRITY,
                        failedStage = RestoreStage.VALIDATING_REFERENCES,
                        technicalDetails = "Transaction '$rawId' is missing 'accountId'."
                    )
                )
            }

            var resolvedAccountId = resolveAccount(
                ref = item.rawSrc,
                titleHint = item.title,
                notesHint = item.notes,
                payeeHint = item.payee,
                lookup = lookup,
                isDestination = false
            ) ?: item.rawSrc

            // If source account was not found in known accounts:
            if (!knownAccountIds.contains(resolvedAccountId)) {
                if (!isLegacyOrConverted) {
                    throw BackupRestoreException(
                        BackupDiagnosticError(
                            errorCode = BackupErrorCode.ERR_REFERENTIAL_INTEGRITY,
                            failedStage = RestoreStage.VALIDATING_REFERENCES,
                            technicalDetails = "Transaction '$rawId' references non-existent accountId '$resolvedAccountId'."
                        )
                    )
                }
                // In legacy or converted format: recover unlisted referenced account
                val cleanName = resolvedAccountId.replace(Regex("[^a-zA-Z0-9]+"), " ").trim()
                val recovered = AccountEntity(
                    id = resolvedAccountId,
                    name = cleanName.ifBlank { "Account ${resolvedAccountId.takeLast(6)}" },
                    type = mapAccountType("OTHER", cleanName),
                    balance = 0.0,
                    openingBalance = 0.0,
                    isHidden = true,
                    notes = "Recovered from transaction reference history."
                )
                recoveredAccounts.add(recovered)
                knownAccountIds.add(resolvedAccountId)
            }

            // Resolve destination account
            val rawDstCandidate = item.rawDst ?: pairedDestinations[item.index]
            var resolvedToAccountId = resolveAccount(
                ref = rawDstCandidate,
                titleHint = item.title,
                notesHint = item.notes,
                payeeHint = item.payee,
                lookup = lookup,
                isDestination = true
            )

            // If destination account is explicitly specified by ID but missing from accounts list
            if (!resolvedToAccountId.isNullOrBlank() && !knownAccountIds.contains(resolvedToAccountId)) {
                if (isLegacyOrConverted) {
                    val cleanName = resolvedToAccountId.replace(Regex("[^a-zA-Z0-9]+"), " ").trim()
                    val recovered = AccountEntity(
                        id = resolvedToAccountId,
                        name = cleanName.ifBlank { "Account ${resolvedToAccountId.takeLast(6)}" },
                        type = mapAccountType("OTHER", cleanName),
                        balance = 0.0,
                        openingBalance = 0.0,
                        isHidden = true,
                        notes = "Recovered from transfer destination reference history."
                    )
                    recoveredAccounts.add(recovered)
                    knownAccountIds.add(resolvedToAccountId)
                }
            }

            // Type resolution
            var txType = mapTransactionType(item.typeStr, item.category, item.subcategory, resolvedToAccountId)

            // CRITICAL FIX: If transaction was labeled TRANSFER (or Internal Transfer), but has NO destination account:
            // For legacy or converted backup: safely convert to EXPENSE so it does NOT invent fake accounts,
            // does NOT assign arbitrary wallets, deducts the exact amount from accountId, and satisfies referential integrity!
            var finalCategory = item.category
            var finalNotes = item.notes
            if ((txType == TransactionType.TRANSFER || txType == TransactionType.CC_PAYMENT) && resolvedToAccountId.isNullOrBlank()) {
                if (isLegacyOrConverted) {
                    txType = TransactionType.EXPENSE
                    if (finalCategory.equals("Internal Transfer", ignoreCase = true) || finalCategory.equals("Transfer", ignoreCase = true)) {
                        finalCategory = "Transfer"
                    }
                    if (!finalNotes.contains("[External Transfer]", ignoreCase = true)) {
                        finalNotes = if (finalNotes.isNotBlank()) "$finalNotes [External Transfer]" else "[External Transfer]"
                    }
                }
                // If !isLegacyOrConverted, keep txType as TRANSFER and resolvedToAccountId as null so validatePayloadIntegrity will fail with ERR_REFERENTIAL_INTEGRITY!
            }

            if (item.tags.isNotBlank() && !finalNotes.contains(item.tags)) {
                finalNotes = if (finalNotes.isNotBlank()) "$finalNotes [Tags: ${item.tags}]" else "[Tags: ${item.tags}]"
            }

            val isRecurring = t.optBoolean("isRecurring", t.optBoolean("recurring", false))
            val debtId = t.optString("debtId", t.optString("loanId", "")).trim().takeIf { it.isNotBlank() }

            // Ensure category and subcategory exist in normalized entities
            val normCatKey = finalCategory.lowercase(Locale.US)
            if (finalCategory.isNotBlank() && !catNameSet.contains(normCatKey)) {
                val newCatId = "cat_${normCatKey.replace(Regex("[^a-z0-9]+"), "_")}"
                catNameSet.add(normCatKey)
                catNameToId[normCatKey] = newCatId
                categories.add(
                    CategoryEntity(
                        id = newCatId,
                        name = finalCategory,
                        isIncome = txType == TransactionType.INCOME,
                        isTransfer = txType == TransactionType.TRANSFER || txType == TransactionType.CC_PAYMENT,
                        iconName = "category",
                        colorHex = if (txType == TransactionType.INCOME) "#10B981" else "#64748B",
                        sortOrder = categories.size
                    )
                )
            }

            if (item.subcategory.isNotBlank()) {
                val parentCatId = catNameToId[normCatKey] ?: "cat_misc"
                val subKey = "${parentCatId.lowercase(Locale.US)}::${item.subcategory.lowercase(Locale.US)}"
                if (!subKeySet.contains(subKey)) {
                    subKeySet.add(subKey)
                    subcategories.add(
                        SubcategoryEntity(
                            id = "${parentCatId}_sub_${subcategories.size}",
                            categoryId = parentCatId,
                            name = item.subcategory,
                            sortOrder = subcategories.size
                        )
                    )
                }
            }

            // Split items handling
            val splitsArr = t.optJSONArray("splits") ?: t.optJSONArray("splitItems")
            if (splitsArr != null && splitsArr.length() > 0) {
                for (sIdx in 0 until splitsArr.length()) {
                    val splitObj = splitsArr.optJSONObject(sIdx) ?: continue
                    val splitId = splitObj.optString("id", "${rawId.ifBlank { "tx_${item.index}" }}_split_$sIdx")
                    if (!seenIds.add(splitId)) {
                        skippedDuplicates++
                        continue
                    }
                    val splitAmt = parseSafeDouble(splitObj, listOf("amount", "value"), 0.0, "split[$splitId].amount")
                    val splitCatRaw = extractCategoryString(splitObj, "category", "categoryName", "categoryId")
                    val splitCat = (catIdToName[splitCatRaw] ?: splitCatRaw).ifBlank { finalCategory }
                    val splitSubRaw = extractCategoryString(splitObj, "subcategory", "subcategoryName", "subcategoryId", "subCategoryId")
                    val splitSub = (subIdToName[splitSubRaw] ?: splitSubRaw).ifBlank { item.subcategory }
                    val splitTitle = splitObj.optString("title", splitObj.optString("note", item.title)).ifBlank { item.title }
                    val splitNotes = buildString {
                        if (finalNotes.isNotBlank()) append(finalNotes)
                        val sn = splitObj.optString("notes", "")
                        if (sn.isNotBlank()) {
                            if (isNotEmpty()) append(" | ")
                            append(sn)
                        }
                        if (isNotEmpty()) append(" ")
                        append("[Split ${sIdx + 1}/${splitsArr.length()}]")
                    }
                    parsedList.add(
                        TransactionEntity(
                            id = splitId,
                            type = txType,
                            amount = splitAmt,
                            title = splitTitle,
                            category = splitCat,
                            subcategory = splitSub,
                            accountId = resolvedAccountId,
                            toAccountId = resolvedToAccountId,
                            timestamp = item.timestamp + sIdx,
                            notes = splitNotes,
                            isRecurring = isRecurring,
                            payeeOrPayer = item.payee,
                            debtId = debtId
                        )
                    )
                }
            } else {
                val finalId = rawId.ifBlank { "tx_migrated_${item.timestamp}_${item.index}" }
                parsedList.add(
                    TransactionEntity(
                        id = finalId,
                        type = txType,
                        amount = item.amount,
                        title = item.title,
                        category = finalCategory,
                        subcategory = item.subcategory,
                        accountId = resolvedAccountId,
                        toAccountId = resolvedToAccountId,
                        timestamp = item.timestamp,
                        notes = finalNotes,
                        isRecurring = isRecurring,
                        payeeOrPayer = item.payee,
                        debtId = debtId
                    )
                )
            }
        }

        return ParsedTransactionsResult(parsedList, skippedDuplicates, recoveredAccounts)
    }

    private fun parseTemplates(
        rawTemplates: JSONArray?,
        lookup: AccountLookup,
        catIdToName: Map<String, String>
    ): Pair<List<TransactionEntity>, Int> {
        if (rawTemplates == null) return emptyList<TransactionEntity>() to 0
        val list = mutableListOf<TransactionEntity>()
        var skipped = 0
        for (i in 0 until rawTemplates.length()) {
            val t = rawTemplates.optJSONObject(i) ?: continue
            val title = t.optString("title", t.optString("name", "")).trim()
            val amount = parseSafeDouble(t, listOf("amount", "value"), 0.0, "template[$i].amount")
            if (amount <= 0.0 || title.isBlank()) {
                skipped++
                continue
            }
            val rawAcc = extractAccountReference(t, "accountId", "walletId")
            val accId = resolveAccount(rawAcc, lookup = lookup) ?: lookup.accounts.firstOrNull()?.id
            if (accId.isNullOrBlank()) {
                skipped++
                continue
            }
            val rawCat = extractCategoryString(t, "category", "categoryId")
            val cat = (catIdToName[rawCat] ?: rawCat).ifBlank { "Miscellaneous" }
            val rawType = t.optString("type", "EXPENSE")
            val type = try { mapTransactionType(rawType, cat, "", null) } catch (_: Exception) { TransactionType.EXPENSE }

            list.add(
                TransactionEntity(
                    id = "tpl_${System.currentTimeMillis()}_$i",
                    type = type,
                    amount = amount,
                    title = title,
                    category = cat,
                    accountId = accId,
                    timestamp = System.currentTimeMillis(),
                    isRecurring = true,
                    notes = "[Template]"
                )
            )
        }
        return list to skipped
    }

    private fun parseAndMigrateBudgets(
        rawBudgets: JSONArray?,
        catIdToName: Map<String, String>
    ): Pair<List<BudgetEntity>, Int> {
        if (rawBudgets == null) return emptyList<BudgetEntity>() to 0
        val list = mutableListOf<BudgetEntity>()
        val seenIds = mutableSetOf<String>()
        val seenCatMonth = mutableSetOf<String>()
        var skippedDuplicates = 0

        for (i in 0 until rawBudgets.length()) {
            val b = rawBudgets.optJSONObject(i) ?: continue
            val rawId = b.optString("id", b.optString("budgetId", "")).trim()
            val rawCat = b.optString("categoryName", b.optString("category", b.optString("categoryId", ""))).trim()
            val resolvedCatName = (catIdToName[rawCat] ?: rawCat).ifBlank {
                if (b.optBoolean("isOverall", false)) "Overall" else ""
            }
            if (resolvedCatName.isBlank()) continue

            val limit = parseSafeDouble(b, listOf("monthlyLimit", "limit", "amount"), -1.0, "budget[$rawId].monthlyLimit")
            if (limit < 0.0 || limit.isNaN() || limit.isInfinite()) {
                throw BackupRestoreException(
                    BackupDiagnosticError(
                        errorCode = BackupErrorCode.ERR_INVALID_FINANCIAL_DATA,
                        failedStage = RestoreStage.PROCESSING_BUDGETS_DEBTS,
                        technicalDetails = "Budget '$rawId' has invalid monthlyLimit: $limit"
                    )
                )
            }

            val monthYear = b.optString("monthYear", b.optString("period", "ALL")).ifBlank { "ALL" }
            val isOverall = b.optBoolean("isOverall", resolvedCatName.equals("Overall", ignoreCase = true))
            val subcategoryName = b.optString("subcategoryName", "")
            val name = b.optString("name", "")
            val iconName = b.optString("iconName", "")
            val colorHex = b.optString("colorHex", "")
            val alertPercentage = b.optInt("alertPercentage", 80)
            val id = rawId.ifBlank { "bgt_${resolvedCatName.lowercase(Locale.US)}_${if (subcategoryName.isNotBlank()) "${subcategoryName.lowercase(Locale.US)}_" else ""}$monthYear" }
            val catMonthKey = "${resolvedCatName.lowercase(Locale.US)}::$monthYear::$isOverall::${subcategoryName.lowercase(Locale.US)}"

            if (!seenIds.add(id) || !seenCatMonth.add(catMonthKey)) {
                skippedDuplicates++
                continue
            }

            list.add(
                BudgetEntity(
                    id = id,
                    categoryName = resolvedCatName,
                    monthlyLimit = limit,
                    monthYear = monthYear,
                    isOverall = isOverall,
                    rolloverEnabled = b.optBoolean("rolloverEnabled", false),
                    name = name,
                    subcategoryName = subcategoryName,
                    iconName = iconName,
                    colorHex = colorHex,
                    alertPercentage = alertPercentage
                )
            )
        }
        return list to skippedDuplicates
    }

    private fun parseAndMigrateDebts(
        rawDebts: JSONArray?,
        lookup: AccountLookup
    ): Pair<List<DebtEntity>, Int> {
        if (rawDebts == null) return emptyList<DebtEntity>() to 0
        val tempDebts = mutableListOf<DebtEntity>()
        val seenIds = mutableSetOf<String>()
        var skippedDuplicates = 0

        for (i in 0 until rawDebts.length()) {
            val d = rawDebts.optJSONObject(i) ?: continue
            val rawId = d.optString("id", d.optString("debtId", d.optString("loanId", ""))).trim()
            val personName = d.optString("personName", d.optString("person", d.optString("name", ""))).trim()
            if (personName.isBlank()) continue

            val id = rawId.ifBlank { "debt_${personName.lowercase(Locale.US)}_$i" }
            if (!seenIds.add(id)) {
                skippedDuplicates++
                continue
            }

            val totalAmount = parseSafeDouble(d, listOf("totalAmount", "amount", "principal"), 0.0, "debt[$id].totalAmount")
            val paidAmount = parseSafeDouble(d, listOf("paidAmount", "paid"), 0.0, "debt[$id].paidAmount")
            val amount = if (d.has("amount")) {
                parseSafeDouble(d, listOf("amount"), 0.0, "debt[$id].amount")
            } else {
                (totalAmount - paidAmount).coerceAtLeast(0.0)
            }

            if (amount < 0.0 || amount.isNaN() || totalAmount < 0.0 || totalAmount.isNaN()) {
                throw BackupRestoreException(
                    BackupDiagnosticError(
                        errorCode = BackupErrorCode.ERR_INVALID_FINANCIAL_DATA,
                        failedStage = RestoreStage.PROCESSING_BUDGETS_DEBTS,
                        technicalDetails = "Debt '$id' has invalid negative or NaN amount."
                    )
                )
            }

            val rawType = d.optString("type", d.optString("debtType", "LENT")).uppercase(Locale.US)
            val debtType = when (rawType) {
                "BORROWED", "BORROW", "PAYABLE", "LOAN_TAKEN", "LIABILITY" -> DebtType.BORROWED
                else -> DebtType.LENT
            }

            val rawAcc = extractAccountReference(d, "accountId", "walletId")
            val resolvedAcc = resolveAccount(rawAcc, lookup = lookup) ?: ""

            val isSettled = d.optBoolean("isSettled", d.optBoolean("settled", amount <= 0.0 && totalAmount > 0.0))
            val tags = extractTagsString(d)

            tempDebts.add(
                DebtEntity(
                    id = id,
                    personName = personName,
                    amount = amount,
                    totalAmount = totalAmount,
                    paidAmount = paidAmount,
                    type = debtType,
                    dueDate = d.optString("dueDate", ""),
                    notes = d.optString("notes", d.optString("description", "")),
                    isSettled = isSettled,
                    accountId = resolvedAcc,
                    reason = d.optString("reason", d.optString("notes", "")),
                    colorHex = normalizeColorHex(d.optString("colorHex", "#3B82F6"), "#3B82F6"),
                    tentativeCloseDateStr = d.optString("tentativeCloseDateStr", ""),
                    tags = tags
                )
            )
        }

        val deduplicated = mutableListOf<DebtEntity>()
        val seenActiveSignatures = mutableSetOf<String>()

        for (debt in tempDebts) {
            if (!debt.isSettled) {
                val sig = "${debt.personName.lowercase(Locale.US)}::${debt.type}::${debt.amount}::${debt.notes.lowercase(Locale.US)}"
                if (!seenActiveSignatures.add(sig)) {
                    skippedDuplicates++
                    continue
                }
            }
            deduplicated.add(debt)
        }

        return deduplicated to skippedDuplicates
    }

    private fun parseSavingsGoals(rawGoals: JSONArray?): Pair<List<SavingsGoalEntity>, Int> {
        if (rawGoals == null) return emptyList<SavingsGoalEntity>() to 0
        val list = mutableListOf<SavingsGoalEntity>()
        val seenIds = mutableSetOf<String>()
        var skipped = 0
        for (i in 0 until rawGoals.length()) {
            val g = rawGoals.optJSONObject(i) ?: continue
            val name = g.optString("name", "").trim()
            if (name.isBlank()) continue
            val id = g.optString("id", "goal_$i").trim()
            if (!seenIds.add(id)) {
                skipped++
                continue
            }
            val targetAmount = parseSafeDouble(g, listOf("targetAmount", "target"), 0.0, "goal[$id].targetAmount")
            val savedAmount = parseSafeDouble(g, listOf("savedAmount", "currentAmount", "saved"), 0.0, "goal[$id].savedAmount")
            list.add(
                SavingsGoalEntity(
                    id = id,
                    name = name,
                    targetAmount = targetAmount,
                    savedAmount = savedAmount,
                    themeColorHex = normalizeColorHex(g.optString("themeColorHex", "#F59E0B"), "#F59E0B"),
                    iconName = g.optString("iconName", "savings"),
                    targetDeadline = if (g.has("targetDeadline") && !g.isNull("targetDeadline")) g.optLong("targetDeadline") else null,
                    targetDeadlineStr = g.optString("targetDeadlineStr", ""),
                    createdAt = g.optLong("createdAt", System.currentTimeMillis()),
                    isCompleted = g.optBoolean("isCompleted", savedAmount >= targetAmount && targetAmount > 0)
                )
            )
        }
        return list to skipped
    }

    private fun validatePayloadIntegrity(
        payload: NormalizedBackupPayload,
        existingAccountIds: Set<String>
    ) {
        val validAccountIds = payload.accounts.map { it.id }.toSet() + existingAccountIds

        if (payload.transactions.isNotEmpty() && validAccountIds.isEmpty()) {
            throw BackupRestoreException(
                BackupDiagnosticError(
                    errorCode = BackupErrorCode.ERR_REFERENTIAL_INTEGRITY,
                    failedStage = RestoreStage.VALIDATING_REFERENCES,
                    technicalDetails = "Backup contains ${payload.transactions.size} transactions but 0 valid accounts."
                )
            )
        }

        for (tx in payload.transactions) {
            if (tx.accountId.isBlank() || !validAccountIds.contains(tx.accountId)) {
                throw BackupRestoreException(
                    BackupDiagnosticError(
                        errorCode = BackupErrorCode.ERR_REFERENTIAL_INTEGRITY,
                        failedStage = RestoreStage.VALIDATING_REFERENCES,
                        technicalDetails = "Transaction '${tx.id}' references non-existent accountId '${tx.accountId}'."
                    )
                )
            }
            if (tx.type == TransactionType.TRANSFER || tx.type == TransactionType.CC_PAYMENT) {
                val toId = tx.toAccountId
                if (toId.isNullOrBlank() || !validAccountIds.contains(toId)) {
                    throw BackupRestoreException(
                        BackupDiagnosticError(
                            errorCode = BackupErrorCode.ERR_REFERENTIAL_INTEGRITY,
                            failedStage = RestoreStage.VALIDATING_REFERENCES,
                            technicalDetails = "Transfer transaction '${tx.id}' references missing or invalid destination account '${toId ?: "null"}'."
                        )
                    )
                }
            }
        }
    }

    private fun parseSettings(s: JSONObject?, fallbackCurrency: String): AppSettingsEntity? {
        if (s == null) return null
        val defaults = AppSettingsEntity()
        return defaults.copy(
            userName = s.optString("userName", defaults.userName),
            country = s.optString("country", defaults.country),
            currencyCode = s.optString("currencyCode", fallbackCurrency.ifBlank { defaults.currencyCode }),
            currencySymbol = s.optString("currencySymbol", defaults.currencySymbol),
            language = try { AppLanguage.valueOf(s.optString("language")) } catch (_: Exception) { defaults.language },
            themeMode = try { ThemeMode.valueOf(s.optString("themeMode")) } catch (_: Exception) { defaults.themeMode },
            themeColor = try { ThemeColorOption.valueOf(s.optString("themeColor")) } catch (_: Exception) { defaults.themeColor },
            backgroundTexture = try { BackgroundTextureOption.valueOf(s.optString("backgroundTexture")) } catch (_: Exception) { defaults.backgroundTexture },
            appLockEnabled = s.optBoolean("appLockEnabled", defaults.appLockEnabled),
            screenshotProtectionEnabled = s.optBoolean("screenshotProtectionEnabled", defaults.screenshotProtectionEnabled),
            notificationsEnabled = s.optBoolean("notificationsEnabled", defaults.notificationsEnabled)
        )
    }

    private fun mapAccountType(rawType: String, accountName: String): AccountType {
        return when (rawType.uppercase(Locale.US)) {
            "BANK", "SAVINGS", "REGULAR", "CURRENT", "FDR", "DPS", "DEBIT_CARD", "CHECKING" -> AccountType.BANK
            "CREDIT_CARD", "CREDIT", "CARD", "MASTERCARD", "VISA", "AMEX" -> AccountType.CREDIT_CARD
            "CASH", "HAND_CASH", "PHYSICAL_CASH" -> AccountType.CASH
            "MFS", "MOBILE_WALLET", "E_WALLET", "WALLET", "BKASH", "NAGAD", "ROCKET", "UPAY" -> AccountType.MFS
            "OTHER", "OTHERS" -> AccountType.OTHER
            else -> {
                val lowerName = accountName.lowercase(Locale.US)
                when {
                    lowerName.contains("mastercard") || lowerName.contains("visa") || lowerName.contains("credit") -> AccountType.CREDIT_CARD
                    lowerName.contains("bkash") || lowerName.contains("nagad") || lowerName.contains("wallet") -> AccountType.MFS
                    lowerName.contains("cash") -> AccountType.CASH
                    lowerName.contains("bank") || lowerName.contains("ebl") || lowerName.contains("scb") || lowerName.contains("fdr") -> AccountType.BANK
                    else -> AccountType.OTHER
                }
            }
        }
    }

    private fun mapTransactionType(
        rawType: String,
        category: String,
        subcategory: String,
        toAccountId: String?
    ): TransactionType {
        val norm = rawType.uppercase(Locale.US).trim()
        val mapped = when (norm) {
            "EXPENSE", "DEBIT", "OUTFLOW", "SPEND", "0", "-1" -> TransactionType.EXPENSE
            "INCOME", "CREDIT", "INFLOW", "EARNING", "1" -> TransactionType.INCOME
            "TRANSFER", "INTERNAL_TRANSFER", "ACCOUNT_TRANSFER", "2" -> TransactionType.TRANSFER
            "LEND", "LENT", "LOAN_GIVEN", "RECEIVABLE" -> TransactionType.LEND
            "BORROW", "BORROWED", "LOAN_TAKEN", "PAYABLE", "LOAN_RECEIVED" -> TransactionType.BORROW
            "CC_PAYMENT", "CREDIT_CARD_PAYMENT", "CARD_BILL", "BILL_PAYMENT" -> TransactionType.CC_PAYMENT
            else -> TransactionType.EXPENSE
        }

        // Upgrade legacy transfers that were stored as EXPENSE with toAccountId + Internal Transfer category
        if (mapped == TransactionType.EXPENSE && !toAccountId.isNullOrBlank() &&
            (category.equals("Internal Transfer", ignoreCase = true) || category.equals("Transfer", ignoreCase = true))
        ) {
            return TransactionType.TRANSFER
        }
        if (mapped == TransactionType.EXPENSE && !toAccountId.isNullOrBlank() &&
            (category.equals("Credit Card Payment", ignoreCase = true) || subcategory.equals("Credit Card Payment", ignoreCase = true))
        ) {
            return TransactionType.CC_PAYMENT
        }

        return mapped
    }

    private fun extractCategoryString(obj: JSONObject, vararg keys: String): String {
        for (k in keys) {
            if (!obj.has(k) || obj.isNull(k)) continue
            val v = obj.opt(k)
            when (v) {
                is JSONObject -> {
                    val name = v.optString("name", v.optString("title", v.optString("id", ""))).trim()
                    if (name.isNotBlank()) return name
                }
                is String -> {
                    if (v.isNotBlank()) return v.trim()
                }
            }
        }
        return ""
    }

    private fun extractAccountReference(obj: JSONObject, vararg keys: String): String? {
        for (k in keys) {
            if (!obj.has(k) || obj.isNull(k)) continue
            val v = obj.opt(k)
            when (v) {
                is JSONObject -> {
                    val id = v.optString("id", v.optString("walletId", v.optString("accountId", v.optString("name", "")))).trim()
                    if (id.isNotBlank() && !id.equals("null", ignoreCase = true)) return id
                }
                is String -> {
                    val trimmed = v.trim()
                    if (trimmed.isNotBlank() && !trimmed.equals("null", ignoreCase = true) && !trimmed.equals("undefined", ignoreCase = true)) {
                        return trimmed
                    }
                }
            }
        }
        return null
    }

    private fun extractTagsString(obj: JSONObject): String {
        val keys = listOf("tags", "tagList", "labels")
        for (k in keys) {
            if (!obj.has(k) || obj.isNull(k)) continue
            val v = obj.opt(k)
            when (v) {
                is JSONArray -> {
                    val items = mutableListOf<String>()
                    for (i in 0 until v.length()) {
                        val item = v.opt(i)
                        when (item) {
                            is String -> if (item.isNotBlank()) items.add(item.trim())
                            is JSONObject -> {
                                val n = item.optString("name", item.optString("title", "")).trim()
                                if (n.isNotBlank()) items.add(n)
                            }
                        }
                    }
                    if (items.isNotEmpty()) return items.joinToString(", ")
                }
                is String -> {
                    val trimmed = v.trim()
                    if (trimmed.isNotBlank()) return trimmed
                }
            }
        }
        return ""
    }

    private fun parseSafeDouble(
        obj: JSONObject,
        keys: List<String>,
        default: Double,
        fieldContext: String
    ): Double {
        for (k in keys) {
            if (!obj.has(k) || obj.isNull(k)) continue
            val raw = obj.opt(k)
            when (raw) {
                is Number -> {
                    val d = raw.toDouble()
                    if (d.isNaN() || d.isInfinite()) {
                        throw BackupRestoreException(
                            BackupDiagnosticError(
                                errorCode = BackupErrorCode.ERR_INVALID_FINANCIAL_DATA,
                                failedStage = RestoreStage.PROCESSING_TRANSACTIONS,
                                technicalDetails = "Non-finite number in $fieldContext"
                            )
                        )
                    }
                    return d
                }
                is String -> {
                    val cleaned = raw.trim().replace(",", "").replace("৳", "").replace("$", "").trim()
                    if (cleaned.isEmpty()) return default
                    val parsed = cleaned.toDoubleOrNull()
                        ?: throw BackupRestoreException(
                            BackupDiagnosticError(
                                errorCode = BackupErrorCode.ERR_INVALID_FINANCIAL_DATA,
                                failedStage = RestoreStage.PROCESSING_TRANSACTIONS,
                                technicalDetails = "Invalid numeric value '$raw' in $fieldContext"
                            )
                        )
                    return parsed
                }
            }
        }
        return default
    }

    private fun parseTimestampMillis(obj: JSONObject, fallbackIndex: Int): Long {
        val keys = listOf("timestamp", "date", "dateTime", "createdAt", "time")
        for (k in keys) {
            if (!obj.has(k) || obj.isNull(k)) continue
            val v = obj.opt(k)
            when (v) {
                is Number -> {
                    val num = v.toLong()
                    if (num <= 0L) continue
                    return if (num in 100_000_000L..99_999_999_999L) num * 1000L else num
                }
                is String -> {
                    val str = v.trim()
                    if (str.isEmpty()) continue
                    str.toLongOrNull()?.let { num ->
                        if (num > 0L) {
                            return if (num in 100_000_000L..99_999_999_999L) num * 1000L else num
                        }
                    }
                    val formats = listOf(
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                        "yyyy-MM-dd'T'HH:mm:ss'Z'",
                        "yyyy-MM-dd'T'HH:mm:ssXXX",
                        "yyyy-MM-dd'T'HH:mm:ss",
                        "yyyy-MM-dd HH:mm:ss",
                        "yyyy-MM-dd"
                    )
                    for (pattern in formats) {
                        try {
                            val sdf = SimpleDateFormat(pattern, Locale.US)
                            if (pattern.endsWith("'Z'")) {
                                sdf.timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val parsedDate = sdf.parse(str)
                            if (parsedDate != null) return parsedDate.time
                        } catch (_: Exception) {
                        }
                    }
                    throw BackupRestoreException(
                        BackupDiagnosticError(
                            errorCode = BackupErrorCode.ERR_INVALID_FINANCIAL_DATA,
                            failedStage = RestoreStage.PROCESSING_TRANSACTIONS,
                            technicalDetails = "Unparseable date/timestamp string '$str'"
                        )
                    )
                }
            }
        }
        return System.currentTimeMillis() - fallbackIndex * 1000L
    }

    private fun extractLast4(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length >= 4) digits.takeLast(4) else digits
    }

    private fun normalizeColorHex(raw: String, fallback: String): String {
        val trimmed = raw.trim()
        if (trimmed.matches(Regex("^#[0-9A-Fa-f]{6,8}$"))) return trimmed
        if (trimmed.matches(Regex("^[0-9A-Fa-f]{6}$"))) return "#$trimmed"
        return fallback
    }

    private fun mapIconName(raw: String, accountType: AccountType): String {
        if (raw.isNotBlank()) return raw
        return when (accountType) {
            AccountType.BANK -> "account_balance"
            AccountType.CREDIT_CARD -> "credit_card"
            AccountType.CASH -> "payments"
            AccountType.MFS -> "account_balance_wallet"
            AccountType.OTHER -> "account_balance_wallet"
        }
    }
}
