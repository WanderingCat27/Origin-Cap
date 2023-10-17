package org.notdev.origincap.mixin;

import java.util.ArrayList;
import java.util.List;

import org.notdev.origincap.global.CapHandler;
import org.notdev.origincap.server.C2S_IsOriginFullCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.networking.packet.c2s.ChooseOriginC2SPacket;
import io.github.apace100.origins.networking.packet.c2s.ChooseRandomOriginC2SPacket;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.screen.ChooseOriginScreen;
import io.github.apace100.origins.screen.OriginDisplayScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Mixin(value = ChooseOriginScreen.class)
@Environment(EnvType.CLIENT)
public abstract class ChooseOriginScreenMixin extends OriginDisplayScreen {

  
  // shadow variables
  @Shadow
  private ArrayList<OriginLayer> layerList;
  @Shadow
  private List<Origin> originSelection;

  @Shadow
  private int currentLayerIndex;
  
  
  @Shadow
  private int currentOriginIndex;
  
  public ChooseOriginScreenMixin(Text title, boolean showDirtBackground) {
    super(title, showDirtBackground);
    //TODO Auto-generated constructor stub
  }
  // my variables
  private ButtonWidget selectButton;

  // enums not allowed annoying since this rlly shouldn't be its own class since
  // it's a mixin and I am kinda using mixins in a undesirable way
  // 0 = inactive 1 = awaiting 2 = active
  private int buttonState;


  // redirect the adding of select button from choose origin class init
  @Redirect(method = "init", at = @At(value = "INVOKE", ordinal = 0, target = "Lio/github/apace100/origins/screen/ChooseOriginScreen;addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;"))
  protected Element init(ChooseOriginScreen instance, Element element) {
    selectButton = ButtonWidget.builder(Text.translatable(Origins.MODID + ".gui.select"), b -> {
      // if state is not active (2) then button shouldn't be clicked
      if (buttonState != 2)
        return;

      Identifier originId = super.getCurrentOrigin().getIdentifier();
      Identifier layerId = getCurrentLayer().getIdentifier();

      if (currentOriginIndex == originSelection.size()) {
        ClientPlayNetworking.send(new ChooseRandomOriginC2SPacket(layerId));
      } else {
        ClientPlayNetworking.send(new ChooseOriginC2SPacket(layerId, originId));
      }

      openNextLayerScreen();
    }
		).dimensions(guiLeft + WINDOW_WIDTH / 2 - 50, guiTop + WINDOW_HEIGHT + 5, 100, 20).build();

    addDrawableChild(selectButton);
    // showOrigin which I override below which also changes the state of the button
    // is run before
    // this.selectButton is assigned so I need to make sure to have it update the
    // look of the button
    // after assigning
    updateButtonState();

    // link CapHandler function for when the origin full check is finished
    CapHandler.onGetResult = (packet) -> {
      // origin and layer recieved from packet is not equal to the current origin on
      // screen
      // maybe switched screens too fast and packet didnt come back in time
      // just ignore the packet because new origin selected has also sent its own
      // request to the server
      if (!equalsCurrOrigin(packet.layerId(), packet.originId()))
        return null;
      if (packet.isFull())
        setButtonState(0); // 0 = inactive
      else
        setButtonState(2); // 2 = active

      return null;
    };
    return selectButton;
  }

  private boolean equalsCurrOrigin(String layerId, String originId) {
    return layerList.get(currentLayerIndex).getIdentifier().toString().equals(layerId)
        && getCurrentOrigin().getIdentifier().toString().equals(originId);
  }

  private void updateButtonState() {
    switch (this.buttonState) {
      case 0:
        selectButton.setMessage(Text.of("FULL"));
        selectButton.setAlpha(.35f);
        break;
      case 2:
        selectButton.setMessage(Text.translatable(Origins.MODID + ".gui.select"));
        selectButton.setAlpha(1);
        break;
      default:
        selectButton.setMessage(Text.of("AWAITING SERVER RESPONSE"));
        selectButton.setAlpha(.5f);
    }
  }

  @Shadow
  abstract void openNextLayerScreen();

  @Override
  public void showOrigin(Origin origin, OriginLayer layer, boolean isRandom) {
    // button state set to awaiting
    setButtonState(1);
    super.showOrigin(origin, layer, isRandom);

    // see if origin is choose-able
    // send packet to server ask if is full
    // recieve event is defined in the init function mixin
    ClientPlayNetworking
        .send(new C2S_IsOriginFullCheck(layer.getIdentifier().toString(), origin.getIdentifier().toString()));
    System.out.println("Sent packet to server");
  }

  /**
   * sets the button state and interact-ability and changes the button appearance
   * to reflect its state
   *
   * @param state 0 = inactive <br>
   *              1 = awaiting response from server <br>
   *              2 = full
   **/
  private void setButtonState(int state) {
    System.out.println(state);
    this.buttonState = state;
    if (this.selectButton != null)
      updateButtonState();
  }

}
