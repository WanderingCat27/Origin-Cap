package notstable.origincap.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.networking.ModPackets;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.screen.ChooseOriginScreen;
import io.github.apace100.origins.screen.OriginDisplayScreen;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import notstable.origincap.OriginCap.ButtonStatus;
import notstable.origincap.OriginCapPackets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ChooseOriginScreen.class)
public abstract class ChooseOriginScreenMixin extends OriginDisplayScreen {
    @Shadow
    public abstract void render(MatrixStack matrices, int mouseX, int mouseY, float delta);

    @Shadow
    private final ArrayList<OriginLayer> layerList;
    @Shadow
    private final List<Origin> originSelection;
    public ButtonStatus buttonStatus;
    @Shadow
    private int currentLayerIndex;
    @Shadow
    private int currentOrigin;
    @Shadow
    private int maxSelection;
    @Shadow
    private Origin randomOrigin;
    private ButtonWidget chooseOriginButton;
    private Text chooseText;
    private Text fullText;
    private Text loadingText;
    private Text fetchingText;
    private int randomIndex = -1;
    private boolean randomNotLoaded = false;

    public ChooseOriginScreenMixin(Text title, boolean showDirtBackground, ArrayList<OriginLayer> layerList, List<Origin> originSelection) {
        super(title, showDirtBackground);
        this.layerList = layerList;
        this.originSelection = originSelection;
    }

    @Shadow
    protected abstract Origin getCurrentOriginInternal();

    /**
     * @author NotStable
     */
    @Overwrite
    protected void init() {
        // client world - The integrated server is only present when a local single player world is open.
        if (MinecraftClient.getInstance().getServer() != null) {
            originalOriginInit(); // if client world ignore custom screen
            return;
        }


        customInit();
    }


    private void customInit() {
        PacketByteBuf removeBuf = new PacketByteBuf(Unpooled.buffer());
        removeBuf.writeString(layerList.get(currentLayerIndex).getIdentifier().toString());
        ClientPlayNetworking.send(OriginCapPackets.REMOVE_PLAYER_FROM_CAP, removeBuf);

        // random origin
        // when select on random origin changes behavior to just select a random origin from the list so it is more predictable for me
        new Thread(() -> {
            createRandom(1, 0);
        }).start();


        super.init();

        chooseText = Text.literal("Select");
        fullText = Text.literal("FULL");
        loadingText = Text.literal("LOADING");
        fetchingText = Text.literal("FETCHING ORIGIN");

        chooseOriginButton = new ButtonWidget(guiLeft + windowWidth / 2 - 50, guiTop + windowHeight + 5, 100, 20, loadingText, b -> {
            System.out.println(buttonStatus);

            if (buttonStatus == ButtonStatus.CHOOSABLE) {
                AtomicBoolean openNextScreen = new AtomicBoolean(false);

                // the button has been clicked while choosable
                // will check one more time that the origin has not changed since found choosable
                // so now is fetching origin from the server then if good will proceed
                System.out.println("settings up to fetch");
                setButtonFetching();
                // set event to know when server returns
                OriginCapPackets.setCapReceivedListener((layerKey, originKey, isChoosable) -> {
                    if (buttonStatus != ButtonStatus.FETCHING || !isCurrOrigin(layerKey, originKey))
                        return;
                    // if choosable proceed

                    if (!isChoosable) { // button fetched and found smth changed and now is full so abort
                        setButtonFull();
                        return;
                    }
                    // else is choosable below

                    // tell server to update origin and origin cap
                    // update cap
                    ClientPlayNetworking.send(OriginCapPackets.UPDATE_ORIGIN_CAP, getOriginPacketBuffer());
                    // update origin
                    ClientPlayNetworking.send(ModPackets.CHOOSE_ORIGIN, getOriginPacketBuffer());
                    openNextScreen.set(true);
                });
                // ping server again to make sure origin is still choosable
                // ping result gets sent to the event above
                ClientPlayNetworking.send(OriginCapPackets.SERVER_RESPOND_CHECK_CAP, getCapPacketBuffer());


                // ****** ERROR CALLED FROM WRONG THREAD ******* -- curr fix
                while (buttonStatus == ButtonStatus.FETCHING && !openNextScreen.get()) ;
                if (openNextScreen.get()) // will this re-invoke or will the button be choosable still b4 checking
                    openNextLayerScreen(); // do I need to add setButtonLoading()
            }
        });

        // loading button
        setButtonLoading();

        // left right buttons
        if (maxSelection > 1) {
            addDrawableChild(new ButtonWidget(guiLeft - 40, this.height / 2 - 10, 20, 20, Text.of("<"), b -> {
                currentOrigin = (currentOrigin - 1 + maxSelection) % maxSelection;
                Origin newOrigin = getCurrentOriginInternal();
                showOrigin(newOrigin, layerList.get(currentLayerIndex), newOrigin == randomOrigin);


                // set selection button loading
                setButtonLoading();
            }));
            addDrawableChild(new ButtonWidget(guiLeft + windowWidth + 20, this.height / 2 - 10, 20, 20, Text.of(">"), b -> {
                currentOrigin = (currentOrigin + 1) % maxSelection;
                Origin newOrigin = getCurrentOriginInternal();
                showOrigin(newOrigin, layerList.get(currentLayerIndex), newOrigin == randomOrigin);

                // set selection button loading
                setButtonLoading();
            }));
        }
        addDrawableChild(chooseOriginButton);
    }


    private void createRandom(int randSelect, int i) {
        if (i >= originSelection.size()) {
            randomIndex = -2; // -2 == full
            if (randomNotLoaded) {
                randomNotLoaded = false;
                loadCurrOrigin();
            }
            return; // stop searching if iterates thru everything
        }
        if (randSelect == 0) randSelect++; // 0 = human, skip

        // register even for packet that will be set below
        int finalRandSelect = randSelect;
        OriginCapPackets.setRandomCapReceivedListener((layerKey, originKey, isChoosable) -> {
            System.out.println("received check for " + originKey);

            if (isChoosable) {
                if (randomNotLoaded) {
                    randomNotLoaded = false;
                    loadCurrOrigin();
                }

                System.out.println("selected origin");

                randomIndex = finalRandSelect;
            } else {
                // random failed, check next index
                createRandom((finalRandSelect + 1) % originSelection.size(), i + 1);
            }
        });

        // send packet to check the curr rand index

        // send request to compare origin to cap and return to random origin
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        // request type
        buf.writeEnumConstant(OriginCapPackets.CapRequestType.RANDOM_CHECK);
        // random origin id check
        buf.writeString(originSelection.get(randSelect).getIdentifier().toString());
        // layer id
        buf.writeString(layerList.get(currentLayerIndex).getIdentifier().toString());

        System.out.println("sending random check packet");
        ClientPlayNetworking.send(OriginCapPackets.SERVER_RESPOND_CHECK_CAP, buf);
    }

    private boolean isOnRandomOrigin() {
        return currentOrigin == originSelection.size();
    }

    private void setButtonFull() {
        chooseOriginButton.setAlpha(.5f);
        chooseOriginButton.setMessage(fullText);
        buttonStatus = ButtonStatus.FULL;
    }

    private void setButtonLoading() {
        if(randomIndex == -2)
            setButtonFull();
        chooseOriginButton.setAlpha(.5f);
        chooseOriginButton.setMessage(loadingText);
        buttonStatus = ButtonStatus.LOADING;
        if (getCurrOriginIncludeRandom() == null) // random not found yet
            return;


        loadCurrOrigin();
    }

    private void setButtonChoosable() {
        chooseOriginButton.setAlpha(1f);
        chooseOriginButton.setMessage(chooseText);
        buttonStatus = ButtonStatus.CHOOSABLE;

        System.out.println("button choosable");
    }

    private void setButtonFetching() {
        chooseOriginButton.setAlpha(.75f);
        chooseOriginButton.setMessage(fetchingText);
        buttonStatus = ButtonStatus.FETCHING;
    }

    private void loadCurrOrigin() {
        // if random has not found anything set full
        // already dealt with if on random screen and still loading random
        if (getCurrOriginIncludeRandom() == null) {
            setButtonFull();
            return;
        }

        // set load event
        OriginCapPackets.setCapReceivedListener((layerKey, originKey, isChoosable) -> {
            System.out.println("load event (isChoosable): " + isChoosable);
            if (!isCurrOrigin(layerKey, originKey) || buttonStatus != ButtonStatus.LOADING) // make sure still on same page, if not, abort
                return;
            if (isChoosable)
                setButtonChoosable();
            else
                setButtonFull();
        });

        // ping server to load button - standard origin check
        ClientPlayNetworking.send(OriginCapPackets.SERVER_RESPOND_CHECK_CAP, getCapPacketBuffer());

    }

    private boolean isCurrOrigin(String layerID, String originID) {
        // should never check if curr origin is -1 by the way of implementation otherwise will error
        return getCurrOriginIncludeRandom().getIdentifier().toString().equalsIgnoreCase(originID) && layerList.get(currentLayerIndex).getIdentifier().toString().equalsIgnoreCase(layerID);
    }


    // standard origin packet, the one from original origin code
    private PacketByteBuf getOriginPacketBuffer() {

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        // origin id
        buf.writeString(getCurrOriginIncludeRandom().getIdentifier().toString());
        // layer id
        buf.writeString(layerList.get(currentLayerIndex).getIdentifier().toString());

        return buf;
    }

    // packet for requesting if origin is capped or not
    private PacketByteBuf getCapPacketBuffer() {

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        // request type
        buf.writeEnumConstant(OriginCapPackets.CapRequestType.LOAD_CHECK);
        // origin id
        buf.writeString(getCurrOriginIncludeRandom().getIdentifier().toString());
        // layer id
        buf.writeString(layerList.get(currentLayerIndex).getIdentifier().toString());

        return buf;
    }

    private Origin getCurrOriginIncludeRandom() {
        if (isOnRandomOrigin()) {
            if (randomIndex < 0) // random not found yet or at all
                return null;
            else
                return originSelection.get(randomIndex); // get random origin
        }
        return originSelection.get(currentOrigin); // get standard origin selection
    }

    @Shadow
    private void openNextLayerScreen() {

    }


    protected void originalOriginInit() {
        super.init();
        if (maxSelection > 1) {
            addDrawableChild(new ButtonWidget(guiLeft - 40, this.height / 2 - 10, 20, 20, Text.of("<"), b -> {
                currentOrigin = (currentOrigin - 1 + maxSelection) % maxSelection;
                Origin newOrigin = getCurrentOriginInternal();
                showOrigin(newOrigin, layerList.get(currentLayerIndex), newOrigin == randomOrigin);
            }));
            addDrawableChild(new ButtonWidget(guiLeft + windowWidth + 20, this.height / 2 - 10, 20, 20, Text.of(">"), b -> {
                currentOrigin = (currentOrigin + 1) % maxSelection;
                Origin newOrigin = getCurrentOriginInternal();
                showOrigin(newOrigin, layerList.get(currentLayerIndex), newOrigin == randomOrigin);
            }));
        }
        addDrawableChild(new ButtonWidget(guiLeft + windowWidth / 2 - 50, guiTop + windowHeight + 5, 100, 20, Text.translatable(Origins.MODID + ".gui.select"), b -> {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            if (currentOrigin == originSelection.size()) {
                buf.writeString(layerList.get(currentLayerIndex).getIdentifier().toString());
                ClientPlayNetworking.send(ModPackets.CHOOSE_RANDOM_ORIGIN, buf);
            } else {
                buf.writeString(getCurrentOrigin().getIdentifier().toString());
                buf.writeString(layerList.get(currentLayerIndex).getIdentifier().toString());
                ClientPlayNetworking.send(ModPackets.CHOOSE_ORIGIN, buf);
            }
            openNextLayerScreen();
        }));
    }
}

