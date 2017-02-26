package org.researchstack.backbone.step;

import org.researchstack.backbone.result.TaskResult;
import org.researchstack.backbone.task.NavigableOrderedTask;
import org.researchstack.backbone.ui.step.layout.InstructionStepLayout;

import java.util.List;

/**
 * Created by TheMDP on 1/12/17.
 */

public class CustomInstructionStep extends CustomStep implements NavigableOrderedTask.NavigationRule, InstructionStepInterface {
    /*
     * Additional detailed text to display
     */
    String moreDetailText;

    /**
     Additional text to display for the step in a localized string at the bottom of the view.

     The footnote is displayed in a smaller font below the continue button. It is intended to be used
     in order to include disclaimer, copyright, etc. that is important to display in the step but
     should not distract from the main purpose of the step.
     */
    String footnote;

    /**
     An image that provides visual context for the instruction.

     The image is displayed with aspect fit. Depending on the device, the screen area
     available for this image can vary. For exact
     metrics, see `ORKScreenMetricIllustrationHeight`.
     */
    String image;

    /**
     * True if this drawable should be loaded using AnimatedVectorDrawableCompat
     * false, if this drawable should be loaded like any other image
     */
    boolean isImageAnimated;

    /**
     * The duration in between animation repeats in milliseconds
     */
    long animationRepeatDuration;

    /**
     Optional icon image to show above the title and text.
     */
    String iconImage;

    /**
     * Pointer to the next step to show after this one. If nil, then the next step
     * is determined by the navigation rules setup by NavigableOrderedTask.
     */
    String nextStepIdentifier;

    /* Default constructor needed for serilization/deserialization of object */
    CustomInstructionStep() {
        super();
    }

    public CustomInstructionStep(String identifier, String title, String text, String customTypeIdentifier)
    {
        super(identifier, title, customTypeIdentifier);
        setText(text);
        setOptional(false);
    }

    @Override
    public Class getStepLayoutClass()
    {
        return InstructionStepLayout.class;
    }

    public void setMoreDetailText(String detailText) {
        moreDetailText = detailText;
    }
    public String getMoreDetailText() {
        return moreDetailText;
    }

    public void setFootnote(String newFootnote) {
        footnote = newFootnote;
    }
    public String getFootnote() {
        return footnote;
    }

    public void setImage(String newImage) {
        image = newImage;
    }
    public String getImage() {
        return image;
    }

    public void setIconImage(String image) {
        iconImage = image;
    }
    public String getIconImage() {
        return iconImage;
    }

    public void setNextStepIdentifier(String identifier) {
        nextStepIdentifier = identifier;
    }
    public String getNextStepIdentifier() {
        return nextStepIdentifier;
    }

    @Override
    public void setIsImageAnimated(boolean isImageAnimated) {
        this.isImageAnimated = isImageAnimated;
    }

    @Override
    public boolean getIsImageAnimated() {
        return isImageAnimated;
    }

    public void setAnimationRepeatDuration(long animationRepeatDuration) {
        this.animationRepeatDuration = animationRepeatDuration;
    }
    public long getAnimationRepeatDuration() {
        return animationRepeatDuration;
    }

    @Override
    public String nextStepIdentifier(TaskResult result, List<TaskResult> additionalTaskResults) {
        return nextStepIdentifier;
    }
}
