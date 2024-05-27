package io.github.tt432.pickcommand;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(Pickcommand.MOD_ID)
public class Pickcommand {
    public static final String MOD_ID = "pickcommand";

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CommandDataComponent>> COMMAND_COMPONENT =
            DATA_COMPONENTS.register("command",
                    () -> DataComponentType.<CommandDataComponent>builder()
                            .persistent(Codec.STRING.xmap(CommandDataComponent::new, CommandDataComponent::command))
                            .build());

    public Pickcommand(IEventBus bus) {
        DATA_COMPONENTS.register(bus);

        NeoForge.EVENT_BUS.addListener(Pickcommand::onEvent);
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void onEvent(ItemEntityPickupEvent.Pre event) {
        ItemEntity itemEntity = event.getItemEntity();

        if (itemEntity.hasPickUpDelay() || itemEntity.level().isClientSide) return;

        CommandDataComponent commandDataComponent = itemEntity.getItem().getComponents().get(COMMAND_COMPONENT.get());

        if (commandDataComponent != null) {
            CommandSourceStack commandSourceStack = event.getPlayer().createCommandSourceStack();

            try {
                commandSourceStack.dispatcher().execute(commandDataComponent.command(),
                        commandSourceStack.withPermission(4));
                itemEntity.discard();
                event.setCanPickup(TriState.FALSE);
            } catch (CommandSyntaxException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }
}
