package com.r3.developers.apples.contracts

import com.r3.developers.apples.states.AppleStamp
import com.r3.developers.apples.states.BasketOfApples
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class BasketOfApplesContract: Contract {
    override fun verify(transaction: UtxoLedgerTransaction) {
        when (val command = transaction.commands.first()) {
            is AppleCommands.PackBasket -> {
                val output = transaction.getOutputStates(BasketOfApples::class.java).first()
                require(transaction.outputContractStates.size == 1) {
                    "This transaction should only have one BasketOfApples state as output"
                }
                require(output.description.isNotBlank()) {
                    "The output BasketOfApples state should have clear description of the type of redeemable goods"
                }
                require(output.weight > 0) {
                    "The output BasketOfApples state should have non zero weight"
                }
            }
            is AppleCommands.Redeem -> {

                val appleStampInputs = transaction.getInputStates(AppleStamp::class.java)
                val basketOfApplesInputs = transaction.getInputStates(BasketOfApples::class.java)

                val output = transaction.getOutputStates(AppleStamp::class.java).first()
                require(transaction.outputContractStates.size == 2) {
                    "This transaction should consumes two states"
                }
                require(appleStampInputs.isNotEmpty() && basketOfApplesInputs.isNotEmpty()) {
                    "This transaction should have exactly one AppleStamp and one BasketOfApples input state"
                }
                require(appleStampInputs.single().issuer == basketOfApplesInputs.single().farm) {
                    "The issuer of the AppleStamp should be the producing farm of BasketOfApples"
                }
                require(basketOfApplesInputs.single().weight > 0) {
                    "The weight of the basket of apples must be greater than zero"
                }
            }
            else -> {
                throw IllegalArgumentException("Incorrect type of BasketOfApples commands: ${command::class.java.name}")
            }
        }
    }

}