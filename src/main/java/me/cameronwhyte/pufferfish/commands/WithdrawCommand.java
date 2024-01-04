package me.cameronwhyte.pufferfish.commands;

import org.springframework.stereotype.Component;

@Component
public class WithdrawCommand extends ATMCommand implements SlashCommand {

    @Override
    public String getName() {
        return "withdraw";
    }
}
