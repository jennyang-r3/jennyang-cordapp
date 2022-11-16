package com.r3.developers.csdetemplate

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger

// A class to hold the arguments required to start the flow
class MyFirstFlowStartArgs(val otherMember: MemberX500Name)


// Where a class contains a message, mark it with @CordaSerializable to enable Corda to send it from one virtual node to another.
@CordaSerializable
class Message(val sender: MemberX500Name, val message: String)


// MyFirstFlow is an initiating flow, it's corresponding responder flow is called MyFirstFlowResponder (defined below)
// to link the two sides of the flow together they need to have the same protocol.
@InitiatingFlow(protocol = "my-first-flow")
// MyFirstFlow should inherit from RPCStartableFlow, which tells Corda it can be started via an RPC call
class MyFirstFlow: RPCStartableFlow {

    // Log messages from the flows for debugging.
    private companion object {
        val log = contextLogger()
    }

    // Corda has a set of injectable services which are injected into the flow at runtime.
    // Flows declare them with @CordaInjectable, then the flows have access to their services.

    // JsonMarshallingService provides a service for manipulating json
    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    // FlowMessaging establishes flow sessions between Virtual Nodes which
    // sends and receives payloads between them.
    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    // MemberLookup looks for information about members of the Virtual Network which
    // this CorDapp is operates in.
    @CordaInject
    lateinit var memberLookup: MemberLookup



    // When a flow is invoked it's call() method is called.
    // call() methods must be marked as @Suspendable, this allows Corda to pause mid-execution to wait
    // for a response from the other flows and services
    @Suspendable
    override fun call(requestBody: RPCRequestData): String {

        // Follow what happens in the console or logs
        log.info("MFF: MyFirstFlow.call() called")

        // Show the requestBody in the logs - establishes the format for starting a flow on corda
        log.info("MFF: requestBody: ${requestBody.getRequestBody()}")

        // Deserialize the Json requestBody into the MyfirstFlowStartArgs class using the JsonSerialisation Service
        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, MyFirstFlowStartArgs::class.java)

        // Obtain the MemberX500Name of counterparty
        val otherMember = flowArgs.otherMember

        // Get our identity from the MemberLookup service.
        val ourIdentity = memberLookup.myInfo().name

        // Create the message payload using the MessageClass we defined.
        val message = Message(otherMember, "Hello from $ourIdentity.")

        // Log the message to be sent.
        log.info("MFF: message.message: ${message.message}")

        // Start a flow session with the otherMember using the FlowMessaging service
        // The otherMember's Virtual Node will run the corresponding MyFirstFlowResponder responder flow
        val session = flowMessaging.initiateFlow(otherMember)

        // Send the Payload using the send method on the session to the MyFirstFlowResponder Responder flow
        session.send(message)

        // Receive a response from the Responder flow
        val response = session.receive(Message::class.java)

        // The return value of a RPCStartableFlow must always be a string. This string will pass
        // back as the REST RPC response when the status of the flow is queried on Corda, or as the return
        // value from the flow when testing using the Simulator
        return response.message
    }
}

// MyFirstFlowResponder is a responder flow, it's corresponding initiating flow is called MyFirstFlow (defined above)
// to link the two sides of the flow together they need to have the same protocol.
@InitiatedBy(protocol = "my-first-flow")
// Responder flows must inherit from ResponderFlow
class MyFirstFlowResponder: ResponderFlow {

    // It is useful to be able to log messages from the flows for debugging.
    private companion object {
        val log = contextLogger()
    }

    // MemberLookup provides a service for looking up information about members of the Virtual Network which
    // this CorDapp is operating in.
    @CordaInject
    lateinit var memberLookup: MemberLookup


    // Responder flows are invoked when an initiating flow makes a call via a session set up with the Virtual
    // node hosting the Responder flow. When a responder flow is invoked it's call() method is called.
    // call() methods must be marked as @Suspendable, this allows Corda to pause mid-execution to wait
    // for a response from the other flows and services/
    // The Call method has the flow session passed in as a parameter by Corda so the session is available to
    // responder flow code, you don't need to inject the FlowMessaging service.
    @Suspendable
    override fun call(session: FlowSession) {

        // Useful logging to follow what's happening in the console or logs
        log.info("MFF: MyFirstResponderFlow.call() called")

        // Receive the payload and deserialize it into a Message class
        val receivedMessage = session.receive(Message::class.java)

        // Log the message as a proxy for performing some useful operation on it.
        log.info("MFF: Message received from ${receivedMessage.sender}: ${receivedMessage.message} ")

        // Get our identity from the MemberLookup service.
        val ourIdentity = memberLookup.myInfo().name

        // Create a response to greet the sender
        val response = Message(ourIdentity,
            "Hello ${session.counterparty.commonName}, best wishes from ${ourIdentity.commonName}")

        // Log the response to be sent.
        log.info("MFF: response.message: ${response.message}")

        // Send the response via the send method on the flow session
        session.send(response)
    }
}
/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "r1",
    "flowClassName": "com.r3.developers.csdetemplate.MyFirstFlow",
    "requestData": {
        "otherMember":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
        }
}
 */