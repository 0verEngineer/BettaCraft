package org.overengineer.bettacraft.client.model;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.overengineer.bettacraft.client.render.GuppyLook;
import org.overengineer.bettacraft.client.render.MobState;
import org.overengineer.bettacraft.client.render.TankmateRenderer;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;

/**
 * M21.6: the guppy and endler model of the livebearer genetics ({@link GuppyModelDefinitions}): the tankmate animation,
 * plus one tail part per tail shape of which only the expressed one shows; females and the young get a smaller tail and
 * the female fins. It picks them in {@link #setupAnim} from the fish.
 */
public class GuppyModel extends TankmateModel {
    private final Map<String, ModelPart> shapes = new HashMap<>();
    private final ModelPart tailGroup, male, female;
    private String shape = "delta";
    private float tailScale = 1F;
    private boolean femaleLook;

    public GuppyModel(ModelPart root) {
        super(root);
        ModelPart fish = root.getChild("fish");
        this.tailGroup = fish.getChild("tail");
        this.male = fish.getChild("male");
        this.female = fish.getChild("female");
        for (String s : GuppyModelDefinitions.SHAPES) shapes.put(s, tailGroup.getChild("t_" + s));
    }

    /** The tail shape that shows, its scale and whether the fish has the female (or young) fins. */
    public void prepare(String shape, float tailScale, boolean femaleLook) {
        this.shape = shapes.containsKey(shape) ? shape : "round";
        this.tailScale = tailScale;
        this.femaleLook = femaleLook;
    }

    @Override
    public void setupAnim(MobState<TankmateEntity> state) {
        // M52b: the model is posed when the submit queue draws the fish (not when the renderer submits it), so the tail
        // shape of the fish is chosen here
        var lb = state.entity.livebearerPhenotype();
        if (lb != null) {
            boolean adult = TankmateRenderer.adultLook(state.entity);
            prepare(lb.tail(), GuppyLook.tailScale(lb, adult), !adult);
        }
        super.setupAnim(state);
        for (var e : shapes.entrySet()) e.getValue().visible = e.getKey().equals(shape);
        tailGroup.xScale = 1F;
        tailGroup.yScale = tailScale;
        tailGroup.zScale = tailScale;
        male.visible = !femaleLook;
        female.visible = femaleLook;
    }
}
