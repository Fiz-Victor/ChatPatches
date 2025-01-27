package obro1961.chatpatches.config;

import com.google.common.collect.Lists;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import obro1961.chatpatches.ChatPatches;
import obro1961.chatpatches.util.Flags;
import obro1961.chatpatches.util.SharedVariables;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * The YetAnotherConfigLib config class.
 * @see Config
 * @apiNote This is the 2nd edition of a config menu using external libraries.
 */
public class YACLConfig extends Config {

    @Override
    public Screen getConfigScreen(Screen parent) {
        List<Option<?>> timeOpts = Lists.newArrayList();
        List<Option<?>> hoverOpts = Lists.newArrayList();
        List<Option<?>> counterOpts = Lists.newArrayList();
        List<Option<?>> compactChatOpts = Lists.newArrayList();
        List<Option<?>> boundaryOpts = Lists.newArrayList();
        List<Option<?>> chatHudOpts = Lists.newArrayList();
        List<Option<?>> chatScreenOpts = Lists.newArrayList();
        List<Option<?>> copyMenuOpts = Lists.newArrayList();

        Config.getOptions().forEach(opt -> {
            String key = opt.key; // to fix "local variable opt.key must be final or effectively final"
            String cat = key.split("[A-Z]")[0];
            if( key.contains("counterCompact") )
                cat = "compact";
            else if( !I18n.hasTranslation("text.chatpatches.category." + cat) )
                cat = "screen";

            if(key.contains("Color"))
                opt = new ConfigOption<>(new Color( (int)opt.get() ), new Color( (int)opt.def ), key) {
                    @Override
                    public Color get() {
                        return new Color( (int)Config.getOption(key).get() );
                    }

                    @Override
                    public void set(Object value) {
                        super.set( ((Color)value).getRGB() - 0xff000000);
                    }
                };

            Option<?> yaclOpt =
                Option.createBuilder()
                    .name( Text.translatable("text.chatpatches." + key) )
                    .description( desc(opt) )
                    .controller( me -> getController(me, key) )
                    .binding( getBinding(opt) )
                    .flag(
                        key.matches(".*[Cc]hat.*") // contains "chat" or "Chat" somewhere
                            ? new OptionFlag[] { client -> client.inGameHud.getChatHud().reset() }
                            : new OptionFlag[0]
                    )
                    .build();


            switch(cat) {
                case "time" -> timeOpts.add(yaclOpt);
                case "hover" -> hoverOpts.add(yaclOpt);
                case "counter" -> counterOpts.add(yaclOpt);
                case "compact" -> compactChatOpts.add(yaclOpt);
                case "boundary" -> boundaryOpts.add(yaclOpt);
                case "chat" -> chatHudOpts.add(yaclOpt);
                case "screen" -> chatScreenOpts.add(yaclOpt);
                case "copy" -> copyMenuOpts.add(yaclOpt);
            }
        });


        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
            .title(Text.translatable("text.chatpatches.title"))
                .category( category("time", timeOpts) )
                .category( category("hover", hoverOpts) )
                .category( category("counter", counterOpts, group(
                    "counter.compact", compactChatOpts, Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/mod/compact-chat"))
                )) )
                .category( category("boundary", boundaryOpts) )
                .category( category("chat", List.of(), group("chat.hud", chatHudOpts, null), group("chat.screen", chatScreenOpts, null)) )
                .category( category("copy", copyMenuOpts) )

                .category(
                    category(
                    "help",
                        List.of(
                            label( Text.translatable("text.chatpatches.help.dateFormat"), "https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html" ),
                            label( Text.translatable("text.chatpatches.help.formatCodes"), "https://minecraft.gamepedia.com/Formatting_codes" ),
                            label( Text.translatable("text.chatpatches.help.faq"), "https://github.com/mrbuilder1961/ChatPatches#faq" ),
                            label( Text.translatable("text.chatpatches.help.regex"), "https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html"),
                            label( Text.translatable("text.chatpatches.help.regexTester"), "https://regex101.com/" )
                        )
                    )
                )
                .save(() -> {
                    write();
                    ChatPatches.LOGGER.info("[YACLConfig.save] Updated the config file at '{}'!", CONFIG_PATH);
                });

        // debug options
        if(SharedVariables.FABRIC_LOADER.isDevelopmentEnvironment()) {
            builder.category(
                category(
                    "debug",
                    List.of(
                        Option.<Integer>createBuilder()
                            .name( Text.literal("Edit Bit Flags (%d^10, %s^2)".formatted(Flags.flags, Integer.toBinaryString(Flags.flags))) )
                            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 0b1111).step(1))
                            .binding( Flags.flags, () -> Flags.flags, inc -> Flags.flags = inc )
                            .build(),

                        ButtonOption.createBuilder()
                            .name( Text.literal("Print GitHub Option table") )
                            .action((yaclScreen, buttonOption) -> {
                                StringBuilder str = new StringBuilder();

                                Config.getOptions().forEach(opt ->
                                    str.append("\n| %s | %s | %s | `text.chatpatches.%s` |".formatted(
                                        I18n.translate("text.chatpatches." + opt.key),

                                        ( opt.getType().equals(Integer.class) && opt.key.contains("Color") )
                                            ? "`0x%06x`".formatted(opt.def)
                                            : (opt.getType().equals(String.class))
                                                ? "`\"" + opt.def + "\"`"
                                                : "`" + opt.def + "`",

                                        I18n.translate("text.chatpatches.desc." + opt.key),
                                        opt.key
                                    ))
                                );

                                ChatPatches.LOGGER.warn("[YACLConfig.printGithubTables]" + str);
                            })
                            .build()
                    )
                )
            );
        }

        ChatPatches.LOGGER.warn("[YACLConfig.desc] Image preview temporarily disabled due to a YACL bug (https://github.com/isXander/YetAnotherConfigLib/issues/87). If you want updates join the discord: https://discord.gg/3MqBvNEyMz!");
        return builder.build().generateScreen(parent);
    }


    @SuppressWarnings("unchecked")
    private static <T> ControllerBuilder<T> getController(Option<T> opt, String key) {
        if( key.matches("^.*(?:Str|Date|Format)$") ) // endsWith "Str" "Date" or "Format"
            return (ControllerBuilder<T>) StringControllerBuilder.create( (Option<String>)opt );

        else if( key.contains("Color") )
            return (ControllerBuilder<T>) ColorControllerBuilder.create( (Option<Color>)opt );

        else if( Config.getOption(key).get() instanceof Integer ) // key is int but not color
            return (ControllerBuilder<T>) IntegerSliderControllerBuilder.create( (Option<Integer>)opt )
                .range( getMinOrMax(key, true), getMinOrMax(key, false) )
                .step( getInterval(key) );

        else
            return (ControllerBuilder<T>) BooleanControllerBuilder.create( (Option<Boolean>)opt ).coloured(true);
    }

    @SuppressWarnings("unchecked")
    private static <T> Binding<T> getBinding(ConfigOption<?> option) {
        ConfigOption<T> o = (ConfigOption<T>) option;

        if( o.key.contains("Date") )
            // must be able to successfully create a SimpleDateFormat
            return Binding.generic(o.def, o::get, inc -> {
                try {
                    new SimpleDateFormat( inc.toString() );
                    o.set( inc );
                } catch (IllegalArgumentException e) {
                    ChatPatches.LOGGER.error("[YACLConfig.getBinding] Invalid date format '{}' provided for '{}'", inc, o.key);
                }
            });

        else if( o.key.contains("Format") )
            // must contain '$'
            return Binding.generic( o.def, o::get, inc -> o.set(inc, inc.toString().contains("$")) );

        else
            // every other setting either has no requirements or is already constrained with its controller
            // this applies to all options containing 'Str' and all boolean, int, and color options.
            // color options have type transformers to int overridden in the screen builder
            return Binding.generic(o.def, o::get, o::set);
    }

    /**
     * Returns the appropriate minimum or maximum value for the given key.
     * Used for upholding the disorganized yet clean look to this class.
     */
    private static int getMinOrMax(String key, boolean min) {
        if(min) {
            return switch(key) {
                case "counterCompactDistance" -> -1;
                default -> 0;
            };
        } else {
            return switch(key) {
                case "counterCompactDistance" -> 1024;
                case "chatWidth" -> 630;
                case "chatMaxMessages" -> Short.MAX_VALUE;
                case "shiftChat" -> 100;
                default -> 100; // fallback as required by the compiler
            };
        }
    }

    /** Returns the appropriate interval for the given key. */
    private static int getInterval(String key) {
        return switch(key) {
            case "chatMaxMessages" -> 16;
            default -> 1;
        };
    }


    /** Note: puts groups before ungrouped options */
    private static ConfigCategory category(String key, List<Option<?>> options, OptionGroup... groups) {
        ConfigCategory.Builder builder = ConfigCategory.createBuilder()
            .name( Text.translatable("text.chatpatches.category." + key) );

        if( I18n.hasTranslation("text.chatpatches.category.desc." + key) )
            builder.tooltip( Text.translatable("text.chatpatches.category.desc." + key) );
        if( groups.length > 0 )
            builder.groups( List.of(groups) );
        if( !options.isEmpty() )
            builder.options( options );

        return builder.build();
    }

    private static OptionGroup group(String key, List<Option<?>> options, Style descriptionStyle) {
        return OptionGroup.createBuilder()
            .name( Text.translatable("text.chatpatches.category." + key) )
            .description(OptionDescription.of( Text.translatable("text.chatpatches.category.desc." + key).fillStyle(descriptionStyle != null ? descriptionStyle : Style.EMPTY) ))
            .options( options )
            .build();
    }

    private static OptionDescription desc(ConfigOption<?> opt) {
        OptionDescription.Builder builder = OptionDescription.createBuilder().text( Text.translatable("text.chatpatches.desc." + opt.key) );

        /*// still crashes in prod :(
        String ext = "webp";
        String image = "textures/preview/" + opt.key.replaceAll("([A-Z])", "_$1").toLowerCase() + "." + ext;
        Path imagePath = Path.of("");
        Identifier id = Identifier.of(ChatPatches.MOD_ID, image);

        try {
            String path = "assets/" + ChatPatches.MOD_ID + "/" + image;
            imagePath = Path.of( YACLConfig.class.getClassLoader().getResource(path).toURI() );
        } catch(URISyntaxException e) {
            ChatPatches.LOGGER.error("[YACLConfig.desc] Error accessing image path for '{}':", opt.key.replaceAll("([A-Z])", "_$1").toLowerCase(), e);
            id = null;
        } catch(NullPointerException npe) {
            ChatPatches.LOGGER.info("[YACLConfig.desc] No .{} image found for '{}'", ext, opt.key.replaceAll("([A-Z])", "_$1").toLowerCase());
            id = null;
        }

        if(id != null) {
            try {
                if(image.endsWith(".webp"))
                    builder.webpImage(imagePath, id);
                else
                    builder.image(imagePath, id);
            } catch(Throwable err) {
                ChatPatches.LOGGER.error("[YACLConfig.desc] Either the Path provided or the Identifier created was invalid: '{}' => Identifier[{}:{}]",
                    imagePath,
                    ChatPatches.MOD_ID, image,
                    err
                );
            }
        }*/

        return builder.build();
    }

    private static Option<Text> label(MutableText labelText, String urlTooltip) {
        return LabelOption.create(
            labelText.fillStyle( Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, urlTooltip)) )
        );
    }
}
