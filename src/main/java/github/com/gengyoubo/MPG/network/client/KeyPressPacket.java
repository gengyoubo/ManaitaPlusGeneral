package github.com.gengyoubo.MPG.network.client;

import github.com.gengyoubo.MPG.item.data.IMPGKey;
import github.com.gengyoubo.MPG.item.ring.MPGCuriosHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class KeyPressPacket {
    private final byte keyCode;


    public KeyPressPacket(FriendlyByteBuf buffer) {
        keyCode = buffer.readByte();
    }


    public KeyPressPacket(byte key) {
        this.keyCode = key;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeByte(keyCode);
    }

    public void handler(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isClient()) {
                return;
            }

            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                return;
            }

            switch (keyCode) {
                case 0 -> handleMainKey(sender);
                case 1 -> handleArmorKey(sender);
            }
        });

        ctx.setPacketHandled(true);
    }

    private void handleMainKey(ServerPlayer player) {
        Optional<ItemStack> curiosKey = MPGCuriosHelper.findFirstMatching(
                player,
                stack -> stack.getItem() instanceof IMPGKey
        );

        if (curiosKey.isPresent()) {
            ItemStack stack = curiosKey.get();
            ((IMPGKey) stack.getItem()).onManaitaKeyPress(stack, player);
            return;
        }

        ItemStack mainHandStack = player.getMainHandItem();

        if (mainHandStack.getItem() instanceof IMPGKey keyItem) {
            keyItem.onManaitaKeyPress(mainHandStack, player);
        }
    }

    private void handleArmorKey(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.getItem() instanceof IMPGKey keyItem) {
                keyItem.onManaitaKeyPress(stack);
            }
        }
    }
}
