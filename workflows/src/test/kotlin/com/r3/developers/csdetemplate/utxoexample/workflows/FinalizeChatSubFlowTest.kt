package com.r3.developers.csdetemplate.utxoexample.workflows

import net.corda.simulator.RequestData
import net.corda.simulator.Simulator
import net.corda.simulator.crypto.HsmCategory
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
//import net.corda.v5.application.flows.RPCRequestData
//import net.corda.v5.application.flows.RPCStartableFlow
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.crypto.SecureHash
import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import net.corda.v5.membership.NotaryInfo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec

class FinalizeChatSubFlowTest {

    // Names picked to match the corda network in config/dev-net.json
    private val aliceX500 = MemberX500Name.parse("CN=Alice, OU=Test Dept, O=R3, L=London, C=GB")
    private val bobX500 = MemberX500Name.parse("CN=Bob, OU=Test Dept, O=R3, L=London, C=GB")
    private val notaryService = MemberX500Name.parse("CN=NotaryService, OU=Test Dept, O=R3, L=London, C=GB")

    @Test
    fun `test that CreateNewChatFlow returns correct message`() {

        val notaryX500Name = MemberX500Name.parse("O=ExampleNotaryService, L=London, C=GB")
        val publicKeyExample: PublicKey = KeyPairGenerator.getInstance("EC")
            .apply { initialize(ECGenParameterSpec("secp256r1")) }
            .generateKeyPair().public
        val utxoNotaryExample = Party(notaryX500Name, publicKeyExample)

//val initiatingFlow = object : RPCStartableFlow{
//    @CordaInject
//    private lateinit var  flowEngine: FlowEngine
//
//    override fun call(requestBody: RPCRequestData): String {
////        return flowEngine.subFlow(FinalizeChatSubFlow(null, bobX500))
//        return ""
//    }
//}
        // Instantiate an instance of the Simulator
        val simulator = Simulator()

        val aliceVN = simulator.createVirtualNode(aliceX500, CreateNewChatFlow::class.java)
        val bobVN =simulator.createVirtualNode(bobX500, CreateNewChatFlow::class.java)

        aliceVN.generateKey("alice-key", HsmCategory.LEDGER, "any-scheme")
        bobVN.generateKey("bob-key", HsmCategory.LEDGER, "any-scheme")

//        val notaryServ = simulator.createInstanceNode(notaryService,null,null)

        val finalizeChatSubFlowStartArgs = FinalizeChatSubFlowStartArgs("674276c9-f311-43a6-90b8-73439bc7e28b","CN=Bob, OU=Test Dept, O=R3, L=London, C=GB")
        // Create a requestData object
        val requestData = RequestData.create(
            "request no 1", // A unique reference for the instance of the flow request
            FinalizeChatSubFlow::class.java, // The name of the flow class which is to be started
            finalizeChatSubFlowStartArgs // The object which contains the start arguments of the flow
        )

        val flowResponse = aliceVN.callFlow(requestData)

        // Check that the flow has returned the expected string
        assert(flowResponse == "Hello Alice, best wishes from Bob")

    }
}

class FinalizeChatSubFlowStartArgs(private val signedTransaction: String, private val otherMember: String)