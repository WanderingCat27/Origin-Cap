package notstable.origincap;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.networking.ModPackets;
import io.github.apace100.origins.origin.Impact;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginRegistry;
import io.github.apace100.origins.registry.ModItems;
import io.github.apace100.origins.screen.OriginDisplayScreen;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class CappedChooseOriginScreen extends OriginDisplayScreen {

    private final ArrayList<OriginLayer> layerList;
    private final List<Origin> originSelection;
    private int currentLayerIndex = 0;
    private int currentOrigin = 0;
    private int maxSelection = 0;

    private Origin randomOrigin;

    private LiteralText chooseText;
    private LiteralText fullText;


    public CappedChooseOriginScreen(ArrayList<OriginLayer> layerList, int currentLayerIndex, boolean showDirtBackground) {
        super(new TranslatableText(Origins.MODID + ".screen.choose_origin"), showDirtBackground);
        this.layerList = layerList;
        this.currentLayerIndex = currentLayerIndex;
        this.originSelection = new ArrayList<>(10);
        PlayerEntity player = MinecraftClient.getInstance().player;
        OriginLayer currentLayer = layerList.get(currentLayerIndex);
        // gets list of all origins?
        List<Identifier> originIdentifiers = currentLayer.getOrigins(player);
        originIdentifiers.forEach(originId -> {
            Origin origin = OriginRegistry.get(originId);
            // make list without checking choosable and check choosable on fly
            // or
            // stop making origins unchoosable thru origins and just send a packet to check if choosable each time
            // might need to add a loading screen then while packet is sending
            if (origin.isChoosable()) {
                // replaces player heads with the head of the client player
                ItemStack displayItem = origin.getDisplayItem();
                if (displayItem.getItem() == Items.PLAYER_HEAD) {
                    if (!displayItem.hasNbt() || !displayItem.getNbt().contains("SkullOwner")) {
                        displayItem.getOrCreateNbt().putString("SkullOwner", player.getDisplayName().getString());
                    }
                }

                // adds origins to the selector
                this.originSelection.add(origin);
            }
        });
        // sorts origin list
        originSelection.sort((a, b) -> {
            int impDelta = a.getImpact().getImpactValue() - b.getImpact().getImpactValue();
            return impDelta == 0 ? a.getOrder() - b.getOrder() : impDelta;
        });
        // maxselection is just the amount of origins
        maxSelection = originSelection.size();
        if (currentLayer.isRandomAllowed() && currentLayer.getRandomOrigins(player).size() > 0) {
            maxSelection += 1;
        }
        if (maxSelection == 0) {
            openNextLayerScreen();
        }

        Origin newOrigin = getCurrentOriginInternal();
        // shows the origin screen
        // maybe need to make custom origin screen to include cap
        // or
        // just display on button
        showOrigin(newOrigin, layerList.get(currentLayerIndex), newOrigin == randomOrigin);
    }

    private void openNextLayerScreen() {
        // waitscreen for origins is protected so I just made a screen that all it does is extend it
        MinecraftClient.getInstance().setScreen(new OriginWaitScreen(layerList, currentLayerIndex, this.showDirtBackground));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        chooseText = new LiteralText("Select");
        fullText = new LiteralText("FULL");
        if (maxSelection > 1) {
            addDrawableChild(new ButtonWidget(guiLeft - 40, this.height / 2 - 10, 20, 20, new LiteralText("<"), b -> {
                currentOrigin = (currentOrigin - 1 + maxSelection) % maxSelection;
                Origin newOrigin = getCurrentOriginInternal();
                showOrigin(newOrigin, layerList.get(currentLayerIndex), newOrigin == randomOrigin);
            }));
            addDrawableChild(new ButtonWidget(guiLeft + windowWidth + 20, this.height / 2 - 10, 20, 20, new LiteralText(">"), b -> {
                currentOrigin = (currentOrigin + 1) % maxSelection;
                Origin newOrigin = getCurrentOriginInternal();
                showOrigin(newOrigin, layerList.get(currentLayerIndex), newOrigin == randomOrigin);
            }));
        }
        addDrawableChild(new ButtonWidget(guiLeft + windowWidth / 2 - 50, guiTop + windowHeight + 5, 100, 20, chooseText, b -> {
            System.out.println("Button");
            // check if origin full
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            if (currentOrigin == originSelection.size()) {
                buf.writeString(layerList.get(currentLayerIndex).getIdentifier().toString());
                ClientPlayNetworking.send(ModPackets.CHOOSE_RANDOM_ORIGIN, buf);
            } else {
                buf.writeString(getCurrentOrigin().getIdentifier().toString());
                buf.writeString(layerList.get(currentLayerIndex).getIdentifier().toString());
                ClientPlayNetworking.send(ModPackets.CHOOSE_ORIGIN, buf);
            }
            // dont open layer screen from here?
            // add event?
            openNextLayerScreen();
        }));
    }

    @Override
    protected Text getTitleText() {
        if (getCurrentLayer().shouldOverrideChooseOriginTitle()) {
            return new TranslatableText(getCurrentLayer().getTitleChooseOriginTranslationKey());
        }
        return new TranslatableText(Origins.MODID + ".gui.choose_origin.title", new TranslatableText(getCurrentLayer().getTranslationKey()));
    }

    private Origin getCurrentOriginInternal() {
        if (currentOrigin == originSelection.size()) {
            if (randomOrigin == null) {
                initRandomOrigin();
            }
            return randomOrigin;
        }
        return originSelection.get(currentOrigin);
    }

    private void initRandomOrigin() {
        this.randomOrigin = new Origin(Origins.identifier("random"), new ItemStack(ModItems.ORB_OF_ORIGIN), Impact.NONE, -1, Integer.MAX_VALUE);
        MutableText randomOriginText = new LiteralText("");
        List<Identifier> randoms = layerList.get(currentLayerIndex).getRandomOrigins(MinecraftClient.getInstance().player);
        randoms.sort((ia, ib) -> {
            Origin a = OriginRegistry.get(ia);
            Origin b = OriginRegistry.get(ib);
            int impDelta = a.getImpact().getImpactValue() - b.getImpact().getImpactValue();
            return impDelta == 0 ? a.getOrder() - b.getOrder() : impDelta;
        });
        for (Identifier id : randoms) {
            randomOriginText.append(OriginRegistry.get(id).getName());
            randomOriginText.append(new LiteralText("\n"));
        }
        setRandomOriginText(randomOriginText);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (maxSelection == 0) {
            openNextLayerScreen();
            return;
        }
        super.render(matrices, mouseX, mouseY, delta);
    }
}
