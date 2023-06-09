package com.r3.developers.apples.workflows

import com.r3.developers.apples.contracts.AppleCommands
import com.r3.developers.apples.states.AppleStamp
import com.r3.developers.apples.states.BasketOfApples
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.security.PublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@InitiatingFlow(protocol = "redeem-apple")
class RedeemApplesFlow: ClientStartableFlow {
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

    private data class RedeemAppleRequest(
        val buyer: MemberX500Name,
        val stampId: UUID
    )
    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(jsonMarshallingService, RedeemAppleRequest::class.java)
        val buyerName = request.buyer
        val stampId = request.stampId

        val notaryInfo = notaryLookup.notaryServices.single()

        // where are these coming from? what do these do?
        val myKey = memberLookup.myInfo().ledgerKeys.first()
        val buyer = memberLookup.lookup(buyerName)
            ?.let { it.ledgerKeys.first() }
            ?: throw IllegalArgumentException("The buyer $buyerName does not exist within the network")

        val appleStampStateRef = utxoLedgerService.findUnconsumedStatesByType(AppleStamp::class.java)
            .firstOrNull { stateAndRef -> stateAndRef.state.contractState.id == stampId }
            ?: throw IllegalArgumentException("No apple stamp matching the stamp id $stampId")
        val basketOfApplesStateRef = utxoLedgerService.findUnconsumedStatesByType(BasketOfApples::class.java)
            .firstOrNull { stateAndRef -> stateAndRef.state.contractState.owner == appleStampStateRef.state.contractState.issuer }
            ?: throw IllegalArgumentException("There are no eligible baskets of apples")

        val originalBasketOfApples = basketOfApplesStateRef.state.contractState
        val updatedBasket = originalBasketOfApples.changeOwner(buyer)

        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notaryInfo.name)
            .addInputStates(appleStampStateRef.ref, basketOfApplesStateRef.ref)
            .addOutputState(updatedBasket)
            .addCommand(AppleCommands.Redeem())
            .addSignatories(listOf(myKey, buyer))
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .toSignedTransaction()

        val session = flowMessaging.initiateFlow(buyerName)

         try {
            utxoLedgerService.finalize(transaction, listOf(session))
        } catch (e: Exception) {
            "Flow failed, message: ${e.message}"
        }

        // search for latest unconsumed basket state, look for the owner of the state, search owner in the network by owner's public key, return owners name
        val ownershipChanged = utxoLedgerService.findUnconsumedStatesByType(BasketOfApples::class.java)
            .firstOrNull { stateAndRef -> stateAndRef.state.contractState.owner == buyer }
            ?: throw IllegalArgumentException("There are no eligible baskets of apples")
        val newOwner = memberLookup.lookup(ownershipChanged.state.contractState.owner)
        return newOwner?.name.toString()
    }
}