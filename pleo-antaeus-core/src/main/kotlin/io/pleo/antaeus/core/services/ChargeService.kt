package io.pleo.antaeus.core.services

import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Charge

class ChargeService(private val dal: AntaeusDal) {

    fun createChargeTransaction(charge: Charge): Charge? {
        return dal.createChargeTransaction(charge)
    }

    fun fetchAll(): List<Charge> {
        return dal.fetchChargeTransaction()
    }
}