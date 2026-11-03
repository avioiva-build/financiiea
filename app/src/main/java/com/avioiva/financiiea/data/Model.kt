package com.avioiva.financiiea.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The kind of money movement. CREDIT = money received from someone (borrowed). */
enum class TxType { EXPENSE, PAYMENT, CREDIT }

/** Where the money lives: cash wallet or bank account. */
enum class Account { CASH, BANK }

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String = TxType.EXPENSE.name,   // TxType.name
    val amount: Double = 0.0,
    val currency: String = "BDT",
    val account: String = Account.CASH.name,  // Account.name
    val category: String = "other",           // key from Categories catalog
    val fromWhom: String = "",                // only for CREDIT
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val receiptPath: String = ""              // optional photo of the receipt
) {
    val txType: TxType get() = runCatching { TxType.valueOf(type) }.getOrDefault(TxType.EXPENSE)
    val txAccount: Account get() = runCatching { Account.valueOf(account) }.getOrDefault(Account.CASH)

    /** Money coming in (payments and credits) is positive, expenses are negative. */
    val signedAmount: Double
        get() = when (txType) { TxType.EXPENSE -> -amount else -> amount }
}
