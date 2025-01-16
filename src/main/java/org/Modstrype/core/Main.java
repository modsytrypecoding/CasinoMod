package org.Modstrype.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class Main implements ClientModInitializer {

    private static Main instance;

    private final Map<String, Boolean> casinoPlayers = new HashMap<>();

    private int numberRange = 5;
    private int winningNumber = 2;
    private double winMultiplier = 2.0;
    private int minBet = 100;
    private int maxBet = 100000;
    private  String CommandPräfix = "?";

    private List<String> winMessages = new ArrayList<>();
    private List<String> loseMessages = new ArrayList<>();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public Main() {
        instance = this;
    }

    private final File configDirectory = new File(MinecraftClient.getInstance().runDirectory, "config/casino_mod");
    private final File configFile = new File(configDirectory, "casino_config.json");



    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        System.out.println("[CasinoMod] client-side mod wird Initialisiert...");
        loadConfig();

        // Register handler
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (message.startsWith(CommandPräfix)) {
                handleCustomCommand(message.substring(1).trim());
                return false;
            }
            return true;
        });

        // Register join event
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            casinoPlayers.clear();

            assert client.player != null;
            client.player.sendMessage(
                    Text.literal("[CasinoMod] Der Mod ist aktiv!").formatted(Formatting.GREEN),
                    false
            );
        });

        System.out.println("[CasinoMod] Client-side Mod erfolgreich initialisiert!");
    }

    public void processMessage(String sender, int amount) {
        assert MinecraftClient.getInstance().player != null;
        String playerName = MinecraftClient.getInstance().player.getGameProfile().getName();

        if (!casinoPlayers.containsKey(playerName) || !casinoPlayers.get(playerName)) {
            return; // Ignore if user is not in Hash
        }

        if (amount < minBet) {
            sendChatMessage(sender, "[BOT] Der Betrag ist zu gering, um zu spielen (min: " + minBet + ").");
            refundAmount(sender, amount);
            return;
        }

        if (amount > maxBet) {
            sendChatMessage(sender, "[BOT] Der Betrag überschreitet das Maximum (max: " + maxBet + ").");
            refundAmount(sender, amount);
            return;
        }

        Random random = new Random();
        int randomNumber = random.nextInt(numberRange) + 1;

        if (randomNumber == winningNumber) {
            int payout = (int) (amount * winMultiplier);
            payPlayer(sender, payout);
            sendChatMessage(sender, getRandomMessage(winMessages));
        } else {
            sendChatMessage(sender, getRandomMessage(loseMessages));
        }
    }

    private void handleCustomCommand(String command) {
        assert MinecraftClient.getInstance().player != null;
        String playerName = MinecraftClient.getInstance().player.getGameProfile().getName();

        if (command.equalsIgnoreCase("casino")) {
            if (casinoPlayers.containsKey(playerName) && casinoPlayers.get(playerName)) {
                sendPlayerMessage("[CasinoMod] Du hast den CasinoMod bereits aktiviert!", Formatting.YELLOW);
            } else {
                casinoPlayers.put(playerName, true);
                sendPlayerMessage("[CasinoMod] Der CasinoMod wurde erfolgreich aktiviert!", Formatting.GREEN);
            }
        } else if (command.equalsIgnoreCase("casino off")) {
            if (casinoPlayers.containsKey(playerName) && casinoPlayers.get(playerName)) {
                casinoPlayers.remove(playerName);
                sendPlayerMessage(" [CasinoMod] Du hast den CasinoMod deaktiviert.", Formatting.RED);
            } else {
                sendPlayerMessage("[CasinoMod] Der CasinoMod ist nicht aktiviert.", Formatting.YELLOW);
            }
        } else if (command.startsWith("casino config")) {
            handleConfigCommand(command);
        } else if (command.equalsIgnoreCase("casino help")) {
            displayHelp();
        } else {
            sendPlayerMessage("[CasinoMod] Diesen Command gibt es nicht: " + command, Formatting.DARK_RED);
        }
    }

    private void handleConfigCommand(String command) {
        String[] args = command.split(" ");
        if (args.length == 7) {
            try {
                numberRange = Integer.parseInt(args[2]);
                winningNumber = Integer.parseInt(args[3]);
                winMultiplier = Double.parseDouble(args[4]);
                minBet = Integer.parseInt(args[5]);
                maxBet = Integer.parseInt(args[6]);

                saveConfig();

                sendPlayerMessage(
                        "Config geupdated: Range = " + numberRange +
                                ", Winning Number = " + winningNumber +
                                ", Multiplier = " + winMultiplier +
                                ", Min Bet = " + minBet +
                                ", Max Bet = " + maxBet,
                        Formatting.GREEN
                );
            } catch (NumberFormatException e) {
                sendPlayerMessage("[CasinoMod] Bei deinen Eingaben stimmt etwas nicht!", Formatting.RED);
            }
        } else {
            sendPlayerMessage("Command: " + CommandPräfix +"casino config <range> <winning number> <multiplier> <min bet> <max bet>", Formatting.YELLOW);
        }
    }

    private void displayHelp() {
        sendPlayerMessage("[CasinoMod Help]", Formatting.GOLD);
        sendPlayerMessage(CommandPräfix +"casino - Aktiviert den CasinoMod", Formatting.GREEN);
        sendPlayerMessage(CommandPräfix +"casino off - Deaktiviert den CasinoMod", Formatting.GREEN);
        sendPlayerMessage(CommandPräfix +"casino config <range> <winning number> <multiplier> <min bet> <max bet> - Konfigurationseinstellungen", Formatting.GREEN);
        sendPlayerMessage(CommandPräfix +"casino help - Zeigt diese hilfreiche Nachricht ;)", Formatting.GREEN);
    }

    private void sendPlayerMessage(String message, Formatting color) {
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendMessage(Text.literal(message).formatted(color), false);
    }

    private void sendChatMessage(String recipient, String message) {
        assert MinecraftClient.getInstance().player != null;
        String command = "msg " + recipient + " " + message;
        MinecraftClient.getInstance().player.networkHandler.sendChatCommand(command);
    }

    private void refundAmount(String sender, int amount) {
        assert MinecraftClient.getInstance().player != null;
        String command = "pay " + sender + " " + amount;

        MinecraftClient.getInstance().player.networkHandler.sendChatCommand(command);

        if (amount >= 5000) {
            String confirmCommand = command + " confirm";
            MinecraftClient.getInstance().player.networkHandler.sendChatCommand(confirmCommand);
        }
    }

    private void payPlayer(String recipient, int amount) {
        assert MinecraftClient.getInstance().player != null;
        String command = "pay " + recipient + " " + amount;

        MinecraftClient.getInstance().player.networkHandler.sendChatCommand(command);

        if (amount >= 5000) {
            String confirmCommand = command + " confirm";
            System.out.println("[CasinoMod] Chat confirm command sent: " + confirmCommand); // Log the confirm command
            MinecraftClient.getInstance().player.networkHandler.sendChatCommand(confirmCommand);
        }
    }

    private String getRandomMessage(List<String> messages) {
        Random random = new Random();
        assert MinecraftClient.getInstance().player != null;
        return messages.get(random.nextInt(messages.size())).replace("{player}", MinecraftClient.getInstance().player.getGameProfile().getName());
    }

    private void saveConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("numberRange", numberRange);
        data.put("winningNumber", winningNumber);
        data.put("winMultiplier", winMultiplier);
        data.put("minBet", minBet);
        data.put("maxBet", maxBet);
        data.put("winMessages", winMessages);
        data.put("loseMessages", loseMessages);

        try (Writer writer = new FileWriter(configFile)) {
            gson.toJson(data, writer);
            System.out.println("[CasinoMod] Konfiguration gespeichert.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        try {
            if (!configFile.exists()) {
                configDirectory.mkdirs();
                saveDefaultConfig(); // Save default config if it doesn't exist
            }

            try (Reader reader = new FileReader(configFile)) {
                Type type = new TypeToken<Map<String, Object>>() {}.getType();
                Map<String, Object> data = gson.fromJson(reader, type);

                numberRange = ((Number) data.getOrDefault("numberRange", 5)).intValue();
                winningNumber = ((Number) data.getOrDefault("winningNumber", 2)).intValue();
                winMultiplier = ((Number) data.getOrDefault("winMultiplier", 2.0)).doubleValue();
                minBet = ((Number) data.getOrDefault("minBet", 100)).intValue();
                maxBet = ((Number) data.getOrDefault("maxBet", 100000)).intValue();

                if (data.containsKey("winMessages")) {
                    Object winMessageData = data.get("winMessages");
                    if (winMessageData instanceof List<?>) {
                        winMessages = new ArrayList<>();
                        for (Object message : (List<?>) winMessageData) {
                            if (message instanceof String) {
                                winMessages.add((String) message);
                            }
                        }
                    }
                }

                if (data.containsKey("loseMessages")) {
                    Object loseMessageData = data.get("loseMessages");
                    if (loseMessageData instanceof List<?>) {
                        loseMessages = new ArrayList<>();
                        for (Object message : (List<?>) loseMessageData) {
                            if (message instanceof String) {
                                loseMessages.add((String) message);
                            }
                        }
                    }
                }

                System.out.println("[CasinoMod] Konfiguration erfolgreich geladen.");
            }
        } catch (Exception e) {
            System.err.println("[CasinoMod] Beim laden der Konfigurationsdatei hat etwas nicht geklappt:");
            e.printStackTrace();
        }
    }

    private void saveDefaultConfig() {
        Map<String, Object> defaultData = new LinkedHashMap<>();
        defaultData.put("numberRange", 5); // Standardwerte für die Range
        defaultData.put("winningNumber", 2); // Standardgewinnzahl
        defaultData.put("winMultiplier", 2.0); // Standardmultiplikator
        defaultData.put("minBet", 100); // Mindestwetteinsatz
        defaultData.put("maxBet", 100000); // Höchstwetteinsatz
        defaultData.put("CommandPrefix", "?");
        defaultData.put("winMessages", Arrays.asList(
                "[BOT] Glückwunsch, du hast gewonnen!",
                "[BOT] Das war dein Glückstag!",
                "[BOT] Jackpot! Du bist der Gewinner!",
                "[BOT] Du hast den Hauptpreis abgestaubt!",
                "[BOT] Nice! Dein Einsatz hat sich gelohnt!",
                "[BOT] Gewonnen! So geht das!",
                "[BOT] Bingo! Preis gehört dir!",
                "[BOT] Du bist der Champ!",
                "[BOT] Mega Glück, du hast abgesahnt!",
                "[BOT] Gewinn! Herzlichen Glückwunsch!",
                "[BOT] Der Sieg gehört dir!",
                "[BOT] Kasse klingelt: Du hast gewonnen!",
                "[BOT] Glückspilz! Der Preis ist dein!",
                "[BOT] Alles richtig gemacht, Glückwunsch!",
                "[BOT] Einsatz verdoppelt! Gewonnen!",
                "[BOT] Das war stark! Sieg für dich!",
                "[BOT] Du hast es geschafft! Preis für dich!",
                "[BOT] Du hast abgeräumt, Glückwunsch!",
                "[BOT] Gewinner! Dein Name steht oben!",
                "[BOT] Dein Glücksmoment ist da: Sieg!"
        ));
        defaultData.put("loseMessages", Arrays.asList(
                "[BOT] Leider verloren, versuch's nochmal!",
                "[BOT] Das war knapp, nächstes Mal!",
                "[BOT] Pech gehabt, Kopf hoch!",
                "[BOT] Heute nicht dein Tag, weiter so!",
                "[BOT] Leider nichts, vielleicht beim nächsten Mal!",
                "[BOT] Knapp daneben ist auch vorbei!",
                "[BOT] Das Glück war nicht auf deiner Seite.",
                "[BOT] Keine Sorge, du hast es fast geschafft!",
                "[BOT] Leider verloren, gib nicht auf!",
                "[BOT] Es hat nicht gereicht, probier’s nochmal!",
                "[BOT] Schade, nächster Versuch!",
                "[BOT] Heute kein Glück, bleib dran!",
                "[BOT] Leider kein Gewinn, weiter geht's!",
                "[BOT] Pech gehabt, das war’s für heute!",
                "[BOT] Knapp verloren, bleib dran!",
                "[BOT] Das war nix, nächstes Mal klappt’s!",
                "[BOT] Kein Glück diesmal, Versuch zählt!",
                "[BOT] Leider nicht dein Tag, probier’s wieder!",
                "[BOT] Du warst nah dran, gib nicht auf!",
                "[BOT] Verloren, aber das nächste Mal gehörst du dazu!"
        ));

        try (Writer writer = new FileWriter(configFile)) {
            gson.toJson(defaultData, writer);
            System.out.println("[CasinoMod] Standardkonfiguration erstellt.");
        } catch (Exception e) {
            System.err.println("[CasinoMod] Fehler beim Speichern der Standardkonfiguration:");
            e.printStackTrace();
        }
    }

}
