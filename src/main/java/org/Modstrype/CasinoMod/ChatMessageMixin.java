package org.Modstrype.CasinoMod;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.Modstrype.core.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public class ChatMessageMixin {

    private static final Logger LOGGER = Logger.getLogger("CasinoMod");

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void onAddMessage(Text message, CallbackInfo ci) {
        String messageContent = message.getString();

        // Filter für Nachrichten im Format "CityBuild » Du hast $<Betrag> von <Spieler> erhalten"
        Pattern pattern = Pattern.compile("CityBuild.* Du hast \\$(\\d{1,3}(?:,\\d{3})*) von ([^ ]+) erhalten.*");
        Matcher matcher = pattern.matcher(messageContent);

        if (matcher.find()) {
            String amountString = matcher.group(1); // Betrag
            String sender = matcher.group(2); // Spielername

            if (Main.getInstance().isPlayerInCasino(sender)) {
                LOGGER.info("[ChatMessageMixin] Nachricht erkannt: Betrag = " + amountString + ", Sender = " + sender);

                try {
                    String cleanedAmountString = amountString.replace(",", "");
                    int amount = Integer.parseInt(cleanedAmountString);

                    LOGGER.info("[ChatMessageMixin] Betrag erfolgreich geparst: " + amount);
                    Main.getInstance().processMessage(sender, amount);
                } catch (NumberFormatException e) {
                    LOGGER.severe("[ChatMessageMixin] Fehler beim Parsen des Betrags: " + amountString);
                    e.printStackTrace();
                }
            } else {
                LOGGER.info("[ChatMessageMixin] Spieler " + sender + " ist nicht in der Casino-Hashmap. Nachricht ignoriert.");
            }
        }
    }
}
