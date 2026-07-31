package com.msg.quickteleports.util;

import java.util.LinkedHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;

public class TextFormatting {

    public static Style gold(){
        return applyStyle(ChatFormatting.GOLD);
    }

    public static Style green(){
        return applyStyle(ChatFormatting.GREEN);
    }

    public static Style red(){
        return applyStyle(ChatFormatting.RED);
    }

    public static Style yellow(){
        return applyStyle(ChatFormatting.YELLOW);
    }

    public static Style white(){
        return applyStyle(ChatFormatting.WHITE);
    }

    public static Style button(String command){
        return applyStyle(Style.EMPTY.withClickEvent(new RunCommand(command)).withBold(true), ChatFormatting.RED);
    }

    public static Style applyStyle(ChatFormatting... formattings){
        return Style.EMPTY.applyFormats(formattings).withClickEvent(null);
    }

    public static Style applyStyle(Style style, ChatFormatting... formattings){
        return Style.EMPTY.applyTo(style).applyFormats(formattings);
    }

    public static MutableComponent shortText(String string, Style style){
        return Component.literal(string).withStyle(style);
    }

    public static MutableComponent longText(LinkedHashMap<String, Style> textMap){
        MutableComponent textCompoent = MutableComponent.create(PlainTextContents.create(""));
        for (String string : textMap.keySet()) {
            textCompoent.append(Component.literal(string).withStyle(textMap.get(string)));
        }
        return textCompoent;
    }
}
