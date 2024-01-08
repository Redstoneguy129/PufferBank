package me.cameronwhyte.pufferfish.util;

import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;

import java.util.NoSuchElementException;

public class SlashUtil {

    public static ApplicationCommandInteractionOptionValue getOptionValue(ApplicationCommandInteractionOption optionInteraction, String option) throws NoSuchElementException {
        return optionInteraction.getOption(option).orElseThrow().getValue().orElseThrow();
    }
}
