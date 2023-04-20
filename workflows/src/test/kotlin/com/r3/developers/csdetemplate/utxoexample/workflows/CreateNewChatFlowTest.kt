package com.r3.developers.csdetemplate.utxoexample.workflows


import net.corda.simulator.RequestData
import net.corda.simulator.Simulator
import net.corda.simulator.crypto.HsmCategory
import net.corda.simulator.runtime.messaging.SimFiber
import net.corda.v5.application.crypto.SigningService
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.persistence.PersistenceService
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.common.Party
import net.corda.v5.membership.NotaryInfo
//import org.junit.Before
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
//import com.nhaarman.mockitokotlin2.mock
import org.mockito.kotlin.whenever
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

class CreateNewChatFlowTest {

    // Names picked to match the corda network in config/dev-net.json
    private val aliceX500 = MemberX500Name.parse("CN=Alice, OU=Test Dept, O=R3, L=London, C=GB")
    private val bobX500 = MemberX500Name.parse("CN=Bob, OU=Test Dept, O=R3, L=London, C=GB")
    private val notaryServiceNode = MemberX500Name.parse("CN=NotaryService, OU=Test Dept, O=R3, L=London, C=GB")
    val mockNotaryLookup = mock<NotaryLookup>()

//    private lateinit var services: MockServices
    private lateinit var notaryLookup: NotaryLookup

    val myServiceMock = Mockito.mock(NotaryLookup::class.java)

    @CordaInject
    lateinit var notaryLookupInject: NotaryLookup


//    @Before
//    fun setup() {
//        services = MockServices(listOf("com.example.contract"))
//        notaryLookup = services.networkMapCache.notaryIdentities.first()
//    }

//    @Before
    fun setup1() {
        notaryLookup = mock()

        val notaryX500Name = MemberX500Name.parse("O=ExampleNotaryService, L=London, C=GB")
        val notaryService = mock<NotaryInfo>().apply {
            whenever(this.name).thenReturn(notaryX500Name)
        }
/*
        whenever(mockNotaryLookup.notaryServices).thenReturn(listOf(notaryService))

        val notaryX500Name = MemberX500Name.parse("O=ExampleNotaryService, L=London, C=GB")
        val publicKeyExample: PublicKey = KeyPairGenerator.getInstance("EC")
            .apply { initialize(ECGenParameterSpec("secp256r1")) }
            .generateKeyPair().public
        val utxoNotaryExample = Party(notaryX500Name, publicKeyExample)

        whenever{ notaryLookup.notaryServices.single() }.thenReturn { utxoNotaryExample }

        val mockNotaryLookup = mock<NotaryLookup>()

        val notaryService = mock<NotaryInfo>().apply {
            whenever(this.name).thenReturn(utxoNotaryExample.name)
        }

        whenever(mockNotaryLookup.notaryServices).thenReturn(listOf(notaryService))
        */
    }

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `test that CreateNewChatFlow returns correct message`() {

//        val notaryX500Name = MemberX500Name.parse("O=ExampleNotaryService, L=London, C=GB")
//        val publicKeyExample: PublicKey = KeyPairGenerator.getInstance("EC")
//            .apply { initialize(ECGenParameterSpec("secp256r1")) }
//            .generateKeyPair().public
//        val utxoNotaryExample = Party(notaryX500Name, publicKeyExample)
//
//        val mockNotaryLookup = mock<NotaryLookup>()



        notaryLookup = mock<NotaryLookup>()
        val notaryX500Name = MemberX500Name.parse("O=ExampleNotaryService, L=London, C=GB")
        val notaryService = mock<NotaryInfo>().apply {
            whenever(this.name).thenReturn(notaryX500Name)
        }
        whenever(notaryLookup.notaryServices).thenReturn(listOf(notaryService))

        // Instantiate an instance of the Simulator
        val simulator = Simulator()

        val aliceVN = simulator.createVirtualNode(aliceX500, CreateNewChatFlow::class.java)
        val bobVN =simulator.createVirtualNode(bobX500, CreateNewChatFlow::class.java)


        aliceVN.generateKey("alice-key", HsmCategory.LEDGER, "any-scheme")
        bobVN.generateKey("bob-key", HsmCategory.LEDGER, "any-scheme")

//        val notaryServ = simulator.createInstanceNode(notaryService,null,null)

        val createNewFlowStartArgs = CreateNewFlowStartArgs("Chat with Bob","CN=Bob, OU=Test Dept, O=R3, L=London, C=GB","Hello Bob")
        // Create a requestData object
        val requestData = RequestData.create(
            "request no 1", // A unique reference for the instance of the flow request
            CreateNewChatFlow::class.java, // The name of the flow class which is to be started
            createNewFlowStartArgs // The object which contains the start arguments of the flow
        )

        val flowResponse = aliceVN.callFlow(requestData)

        // Check that the flow has returned the expected string
        assert(flowResponse == "Hello Alice, best wishes from Bob")

    }
}

class CreateNewFlowStartArgs(val chatName: String , val otherMember: String, val message: String)