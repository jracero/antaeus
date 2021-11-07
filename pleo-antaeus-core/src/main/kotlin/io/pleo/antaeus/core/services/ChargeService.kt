package io.pleo.antaeus.core.services

import io.pleo.antaeus.data.AntaeusChargeDal
import io.pleo.antaeus.models.Charge

class ChargeService(private val dal: AntaeusChargeDal) {

    fun createChargeTransaction(charge: Charge): Charge = dal.createChargeTransaction(charge)

    fun fetchAll(): List<Charge> = dal.fetchChargeTransaction()

}