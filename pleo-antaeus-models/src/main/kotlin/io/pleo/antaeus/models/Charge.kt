package io.pleo.antaeus.models

import java.util.*

data class Charge(
    val id: Int,
    val chargeCreated: Date,
    val invoices: Int,
    val charged: Int,
    val noFunds: Int,
    val unknownCustomer: Int,
    val currencyMismatch: Int
)
