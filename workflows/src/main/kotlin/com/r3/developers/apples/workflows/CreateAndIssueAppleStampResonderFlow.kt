package com.r3.developers.apples.workflows

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.utxo.UtxoLedgerService

@InitiatedBy(protocol = "create-end-issue-apple-stamp")
class CreateAndIssueAppleStampResonderFlow: ResponderFlow {

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService
    @Suspendable
    override fun call(session: FlowSession) {
        val receivedTransaction = utxoLedgerService.receiveFinality(session) { transaction -> }
    }

}