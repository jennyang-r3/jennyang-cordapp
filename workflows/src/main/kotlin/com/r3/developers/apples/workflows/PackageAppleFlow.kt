package com.r3.developers.apples.workflows

import com.r3.developers.apples.contracts.AppleCommands
import com.r3.developers.apples.states.BasketOfApples
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.time.Instant
import java.time.temporal.ChronoUnit

class PackageAppleFlow: ClientStartableFlow {

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    private data class PackAppleRequest(
        val appleDescription: String,
        val weight: Int
    )

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(jsonMarshallingService, PackAppleRequest::class.java)
        val appleDescription = request.appleDescription
        val weight = request.weight

        val notary = notaryLookup.notaryServices.single()
        val myKey = memberLookup.myInfo().ledgerKeys.first()

        // Building the output BasketOfApples state
        val basket = BasketOfApples(
            description = appleDescription,
            farm = myKey,
            owner = myKey,
            weight = weight,
            participants = listOf(myKey)
        )

        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notary.name)
            .addOutputState(basket)
            .addCommand(AppleCommands.PackBasket())
            .addSignatories(listOf(myKey))
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .toSignedTransaction()

        return try {
            // Record the transaction,
            // no sessions are passed in as the transaction is only being recorded locally
            utxoLedgerService.finalize(transaction, emptyList()).toString()
        } catch (e: Exception) {
            "Flow failed, message: ${e.message}"
        }
    }
}