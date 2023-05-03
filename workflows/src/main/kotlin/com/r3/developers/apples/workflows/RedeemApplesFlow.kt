package com.r3.developers.apples.workflows

import com.r3.developers.apples.states.AppleStamp
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.security.PublicKey

@InitiatingFlow(protocol = "create-end-redeem-apple")
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
        val buyer: PublicKey,
        val stampId: AppleStamp
    )
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(jsonMarshallingService, RedeemAppleRequest::class.java)
        val buyerName = requestBody.
    }

}