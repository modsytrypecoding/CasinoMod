package org.Modstrype.CasinoMod;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.Modstrype.core.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public class ChatMessageMixin {

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void onAddMessage(Text message, CallbackInfo ci) {
        String messageContent = message.getString();

        // Filter für Nachrichten im Format "CityBuild » Du hast $<Betrag> von <Spieler> erhalten"
        Pattern pattern = Pattern.compile("CityBuild.* Du hast \\$(\\d{1,3}(?:,\\d{3})*) von ([^ ]+) erhalten.*");
        Matcher matcher = pattern.matcher(messageContent);

        if (matcher.find()) {
            String amountString = matcher.group(1); // Betrag
            String sender = matcher.group(2); // Spielername

            try {

                String cleanedAmountString = amountString.replace(",", "");
                int amount = Integer.parseInt(cleanedAmountString);


                Main.getInstance().processMessage(sender, amount);
            } catch (NumberFormatException e) {
                System.err.println("[CasinoMod Mixin] Fehler beim Parsen des Betrags: " + amountString);
            }
        }
        }
    }

