package com.stripe.android;

import android.app.Activity;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.stripe.android.stripe3ds2.init.ui.ButtonCustomization;
import com.stripe.android.stripe3ds2.init.ui.LabelCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeButtonCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeLabelCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeTextBoxCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization;
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization;
import com.stripe.android.stripe3ds2.init.ui.TextBoxCustomization;
import com.stripe.android.stripe3ds2.init.ui.ToolbarCustomization;
import com.stripe.android.stripe3ds2.init.ui.UiCustomization;
import com.stripe.android.utils.ObjectUtils;

import java.util.Objects;

/**
 * Configuration for authentication mechanisms via {@link PaymentController}
 */
public final class PaymentAuthConfig {

    @Nullable
    private static PaymentAuthConfig sInstance;

    @NonNull
    private static final PaymentAuthConfig DEFAULT = new PaymentAuthConfig.Builder()
            .set3ds2Config(new Stripe3ds2Config.Builder().build())
            .build();

    public static void init(@NonNull PaymentAuthConfig config) {
        sInstance = config;
    }

    @NonNull
    public static PaymentAuthConfig get() {
        return sInstance != null ? sInstance : DEFAULT;
    }

    @VisibleForTesting
    static void reset() {
        sInstance = null;
    }

    @NonNull final Stripe3ds2Config stripe3ds2Config;

    private PaymentAuthConfig(@NonNull Builder builder) {
        stripe3ds2Config = builder.mStripe3ds2Config;
    }

    public static final class Builder implements ObjectBuilder<PaymentAuthConfig> {
        private Stripe3ds2Config mStripe3ds2Config;

        @NonNull
        public Builder set3ds2Config(@NonNull Stripe3ds2Config stripe3ds2Config) {
            this.mStripe3ds2Config = stripe3ds2Config;
            return this;
        }

        @NonNull
        public PaymentAuthConfig build() {
            return new PaymentAuthConfig(this);
        }
    }

    public static final class Stripe3ds2Config {
        static final int DEFAULT_TIMEOUT = 5;

        final int timeout;
        @NonNull final Stripe3ds2UiCustomization uiCustomization;

        private Stripe3ds2Config(@NonNull Builder builder) {
            timeout = checkValidTimeout(builder.mTimeout);
            uiCustomization = Objects.requireNonNull(builder.mUiCustomization);
        }

        private int checkValidTimeout(int timeout) {
            if (timeout < 5 || timeout > 99) {
                throw new IllegalArgumentException(
                        "Timeout value must be between 5 and 99, inclusive");
            }
            return timeout;
        }

        public static final class Builder implements ObjectBuilder<Stripe3ds2Config> {
            private int mTimeout = DEFAULT_TIMEOUT;
            private Stripe3ds2UiCustomization mUiCustomization =
                    new Stripe3ds2UiCustomization.Builder().build();

            @NonNull
            public Builder setTimeout(@IntRange(from = 5, to = 99) int timeout) {
                this.mTimeout = timeout;
                return this;
            }

            @NonNull
            public Builder setUiCustomization(@NonNull Stripe3ds2UiCustomization uiCustomization) {
                this.mUiCustomization = uiCustomization;
                return this;
            }

            @NonNull
            public Stripe3ds2Config build() {
                return new Stripe3ds2Config(this);
            }
        }
    }

    /**
     * Customization for 3DS2 buttons
     */
    public static final class Stripe3ds2ButtonCustomization {

        @NonNull final ButtonCustomization mButtonCustomization;

        Stripe3ds2ButtonCustomization(@NonNull ButtonCustomization buttonCustomization) {
            mButtonCustomization = buttonCustomization;
        }

        public static final class Builder implements ObjectBuilder<Stripe3ds2ButtonCustomization> {
            @NonNull final ButtonCustomization mButtonCustomization;

            public Builder() {
                mButtonCustomization = new StripeButtonCustomization();
            }

            /**
             * Set the button's background color
             *
             * @param hexColor The button's background color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @NonNull
            public Builder setBackgroundColor(@NonNull String hexColor)
                    throws RuntimeException {
                mButtonCustomization.setBackgroundColor(hexColor);
                return this;
            }

            /**
             * Set the corner radius of the button
             *
             * @param cornerRadius The radius of the button in pixels
             * @throws RuntimeException If the corner radius is less than 0
             */
            @NonNull
            public Builder setCornerRadius(int cornerRadius) throws RuntimeException {
                mButtonCustomization.setCornerRadius(cornerRadius);
                return this;
            }

            /**
             * Set the button's text font
             *
             * @param fontName The name of the font. If not found, default system font used
             * @throws RuntimeException If font name is null or empty
             */
            @NonNull
            public Builder setTextFontName(@NonNull String fontName) throws RuntimeException {
                mButtonCustomization.setTextFontName(fontName);
                return this;
            }

            /**
             * Set the button's text color
             *
             * @param hexColor The button's text color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @NonNull
            public Builder setTextColor(@NonNull String hexColor) throws RuntimeException {
                mButtonCustomization.setTextColor(hexColor);
                return this;
            }

            /**
             * Set the button's text size
             *
             * @param fontSize The size of the font in scaled-pixels (sp)
             * @throws RuntimeException If the font size is 0 or less
             */
            @NonNull
            public Builder setTextFontSize(int fontSize) throws RuntimeException {
                mButtonCustomization.setTextFontSize(fontSize);
                return this;
            }

            /**
             * Build the button customization
             *
             * @return The built button customization
             */
            @NonNull
            public Stripe3ds2ButtonCustomization build() {
                return new Stripe3ds2ButtonCustomization(mButtonCustomization);
            }
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || (obj instanceof Stripe3ds2ButtonCustomization &&
                    typedEquals((Stripe3ds2ButtonCustomization) obj));
        }

        private boolean typedEquals(@NonNull Stripe3ds2ButtonCustomization uiCustomization) {
            return Objects.equals(mButtonCustomization, uiCustomization.mButtonCustomization);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mButtonCustomization);
        }
    }

    /**
     * Customization for 3DS2 labels
     */
    public static final class Stripe3ds2LabelCustomization {

        @NonNull final LabelCustomization mLabelCustomization;

        Stripe3ds2LabelCustomization(@NonNull LabelCustomization labelCustomization) {
            mLabelCustomization = labelCustomization;
        }

        public static final class Builder implements ObjectBuilder<Stripe3ds2LabelCustomization> {

            @NonNull final LabelCustomization mLabelCustomization;

            public Builder() {
                mLabelCustomization = new StripeLabelCustomization();
            }

            /**
             * Set the text color for heading labels
             *
             * @param hexColor The heading labels's text color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @NonNull
            public Builder setHeadingTextColor(@NonNull String hexColor)
                    throws RuntimeException {
                mLabelCustomization.setHeadingTextColor(hexColor);
                return this;
            }

            /**
             * Set the heading label's font
             *
             * @param fontName The name of the font. Defaults to system font if not found
             * @throws RuntimeException If the font name is null or empty
             */
            @NonNull
            public Builder setHeadingTextFontName(@NonNull String fontName)
                    throws RuntimeException {
                mLabelCustomization.setHeadingTextFontName(fontName);
                return this;
            }

            /**
             * Set the heading label's text size
             *
             * @param fontSize The size of the heading label in scaled-pixels (sp).
             * @throws RuntimeException If the font size is 0 or less
             */
            @NonNull
            public Builder setHeadingTextFontSize(int fontSize) throws RuntimeException {
                mLabelCustomization.setHeadingTextFontSize(fontSize);
                return this;
            }

            /**
             * Set the label's font
             *
             * @param fontName The name of the font. Defaults to system font if not found
             * @throws RuntimeException If the font name is null or empty
             */
            @NonNull
            public Builder setTextFontName(@NonNull String fontName) throws RuntimeException {
                mLabelCustomization.setTextFontName(fontName);
                return this;
            }

            /**
             * Set the label's text color
             *
             * @param hexColor The labels's text color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @NonNull
            public Builder setTextColor(@NonNull String hexColor) throws RuntimeException {
                mLabelCustomization.setTextColor(hexColor);
                return this;
            }

            /**
             * Set the label's text size
             *
             * @param fontSize The label's font size in scaled-pixels (sp)
             * @throws RuntimeException If the font size is 0 or less
             */
            @NonNull
            public Builder setTextFontSize(int fontSize) throws RuntimeException {
                mLabelCustomization.setTextFontSize(fontSize);
                return this;
            }

            /**
             * Build the configured label customization
             *
             * @return The built label customization
             */
            @NonNull
            public Stripe3ds2LabelCustomization build() {
                return new Stripe3ds2LabelCustomization(mLabelCustomization);
            }
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || (obj instanceof Stripe3ds2LabelCustomization &&
                    typedEquals((Stripe3ds2LabelCustomization) obj));
        }

        private boolean typedEquals(@NonNull Stripe3ds2LabelCustomization uiCustomization) {
            return Objects.equals(mLabelCustomization, uiCustomization.mLabelCustomization);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mLabelCustomization);
        }
    }

    /**
     * Customization for 3DS2 text entry
     */
    public static final class Stripe3ds2TextBoxCustomization {

        @NonNull final TextBoxCustomization mTextBoxCustomization;

        Stripe3ds2TextBoxCustomization(@NonNull TextBoxCustomization textBoxCustomization) {
            mTextBoxCustomization = textBoxCustomization;
        }

        public static final class Builder implements ObjectBuilder<Stripe3ds2TextBoxCustomization> {
            @NonNull final TextBoxCustomization mTextBoxCustomization;

            public Builder() {
                mTextBoxCustomization = new StripeTextBoxCustomization();
            }

            /**
             * Set the width of the border around the text entry box
             *
             * @param borderWidth Width of the border in pixels
             * @throws RuntimeException If the border width is less than 0
             */
            @NonNull
            public Builder setBorderWidth(int borderWidth) throws RuntimeException {
                mTextBoxCustomization.setBorderWidth(borderWidth);
                return this;
            }

            /**
             * Set the color of the border around the text entry box
             *
             * @param hexColor The border's color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @NonNull
            public Builder setBorderColor(@NonNull String hexColor) throws RuntimeException {
                mTextBoxCustomization.setBorderColor(hexColor);
                return this;
            }

            /**
             * Set the corner radius of the text entry box
             *
             * @param cornerRadius The corner radius in pixels
             * @throws RuntimeException If the corner radius is less than 0
             */
            @NonNull
            public Builder setCornerRadius(int cornerRadius) throws RuntimeException {
                mTextBoxCustomization.setCornerRadius(cornerRadius);
                return this;
            }

            /**
             * Set the font for text entry
             *
             * @param fontName The name of the font. The system default is used if not found.
             * @throws RuntimeException If the font name is null or empty.
             */
            @NonNull
            public Builder setTextFontName(@NonNull String fontName) throws RuntimeException {
                mTextBoxCustomization.setTextFontName(fontName);
                return this;
            }

            /**
             * Set the text color for text entry
             *
             * @param hexColor The text color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @NonNull
            public Builder setTextColor(@NonNull String hexColor) throws RuntimeException {
                mTextBoxCustomization.setTextColor(hexColor);
                return this;
            }

            /**
             * Set the text entry font size
             *
             * @param fontSize The font size in scaled-pixels (sp)
             * @throws RuntimeException If the font size is 0 or less
             */
            @NonNull
            public Builder setTextFontSize(int fontSize) throws RuntimeException {
                mTextBoxCustomization.setTextFontSize(fontSize);
                return this;
            }

            /**
             * Build the text box customization
             *
             * @return The text box customization
             */
            @NonNull
            public Stripe3ds2TextBoxCustomization build() {
                return new Stripe3ds2TextBoxCustomization(mTextBoxCustomization);
            }
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || (obj instanceof Stripe3ds2TextBoxCustomization &&
                    typedEquals((Stripe3ds2TextBoxCustomization) obj));
        }

        private boolean typedEquals(@NonNull Stripe3ds2TextBoxCustomization uiCustomization) {
            return Objects.equals(mTextBoxCustomization, uiCustomization.mTextBoxCustomization);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mTextBoxCustomization);
        }
    }

    /**
     * Customization for the 3DS2 toolbar
     */
    public static final class Stripe3ds2ToolbarCustomization {

        @NonNull final ToolbarCustomization mToolbarCustomization;

        Stripe3ds2ToolbarCustomization(@NonNull ToolbarCustomization toolbarCustomization) {
            mToolbarCustomization = toolbarCustomization;
        }

        public static final class Builder implements ObjectBuilder<Stripe3ds2ToolbarCustomization> {
            @NonNull final ToolbarCustomization mToolbarCustomization;

            public Builder() {
                mToolbarCustomization = new StripeToolbarCustomization();
            }

            /**
             * Set the toolbar's background color
             *
             * @param hexColor The background color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @NonNull
            public Builder setBackgroundColor(@NonNull String hexColor)
                    throws RuntimeException {
                mToolbarCustomization.setBackgroundColor(hexColor);
                return this;
            }

            /**
             * Set the status bar color, if not provided a darkened version of the background
             * color will be used.
             *
             * @param hexColor The status bar color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @NonNull
            public Builder setStatusBarColor(@NonNull String hexColor)
                    throws RuntimeException {
                mToolbarCustomization.setStatusBarColor(hexColor);
                return this;
            }

            /**
             * Set the toolbar's title
             *
             * @param headerText The toolbar's title text
             * @throws RuntimeException if the title is null or empty
             */
            @NonNull
            public Builder setHeaderText(@NonNull String headerText) throws RuntimeException {
                mToolbarCustomization.setHeaderText(headerText);
                return this;
            }

            /**
             * Set the toolbar's cancel button text
             *
             * @param buttonText The cancel button's text
             * @throws RuntimeException If the button text is null or empty
             */
            @NonNull
            public Builder setButtonText(@NonNull String buttonText) throws RuntimeException {
                mToolbarCustomization.setButtonText(buttonText);
                return this;
            }

            /**
             * Set the font for the title text
             *
             * @param fontName The name of the font. System default is used if not found
             * @throws RuntimeException If the font name is null or empty
             */
            @NonNull
            public Builder setTextFontName(@NonNull String fontName) throws RuntimeException {
                mToolbarCustomization.setTextFontName(fontName);
                return this;
            }

            /**
             * Set the color of the title text
             *
             * @param hexColor The title's text color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @NonNull
            public Builder setTextColor(@NonNull String hexColor) throws RuntimeException {
                mToolbarCustomization.setTextColor(hexColor);
                return this;
            }

            /**
             * Set the title text's font size
             *
             * @param fontSize The size of the title text in scaled-pixels (sp)
             * @throws RuntimeException If the font size is 0 or less
             */
            @NonNull
            public Builder setTextFontSize(int fontSize) throws RuntimeException {
                mToolbarCustomization.setTextFontSize(fontSize);
                return this;
            }

            /**
             * Build the toolbar customization
             *
             * @return The built toolbar customization
             */
            @NonNull
            public Stripe3ds2ToolbarCustomization build() {
                return new Stripe3ds2ToolbarCustomization(mToolbarCustomization);
            }
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || (obj instanceof Stripe3ds2ToolbarCustomization &&
                    typedEquals((Stripe3ds2ToolbarCustomization) obj));
        }

        private boolean typedEquals(@NonNull Stripe3ds2ToolbarCustomization uiCustomization) {
            return Objects.equals(mToolbarCustomization, uiCustomization.mToolbarCustomization);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mToolbarCustomization);
        }
    }

    /**
     * Customizations for the 3DS2 UI
     */
    public static final class Stripe3ds2UiCustomization {

        @NonNull private final StripeUiCustomization mUiCustomization;

        /**
         * The type of button for which customization can be set
         */
        public enum ButtonType {
            SUBMIT,
            CONTINUE,
            NEXT,
            CANCEL,
            RESEND,
            SELECT
        }

        private Stripe3ds2UiCustomization(@NonNull StripeUiCustomization uiCustomization) {
            mUiCustomization = uiCustomization;
        }

        @NonNull
        public StripeUiCustomization getUiCustomization() {
            return mUiCustomization;
        }

        public static final class Builder implements ObjectBuilder<Stripe3ds2UiCustomization> {
            @NonNull private final StripeUiCustomization mUiCustomization;

            @NonNull
            public static Builder createWithAppTheme(@NonNull Activity activity) {
                return new Builder(activity);
            }

            public Builder() {
                mUiCustomization = new StripeUiCustomization();
            }

            private Builder(@NonNull Activity activity) {
                mUiCustomization = StripeUiCustomization.createWithAppTheme(activity);
            }

            @NonNull
            private UiCustomization.ButtonType getUiButtonType(@NonNull ButtonType buttonType)
                    throws RuntimeException {
                switch (buttonType) {
                    case SUBMIT: {
                        return UiCustomization.ButtonType.SUBMIT;
                    }
                    case CONTINUE: {
                        return UiCustomization.ButtonType.CONTINUE;
                    }
                    case NEXT: {
                        return UiCustomization.ButtonType.NEXT;
                    }
                    case CANCEL: {
                        return UiCustomization.ButtonType.CANCEL;
                    }
                    case RESEND: {
                        return UiCustomization.ButtonType.RESEND;
                    }
                    case SELECT: {
                        return UiCustomization.ButtonType.SELECT;
                    }
                    default: {
                        throw new IllegalArgumentException("Invalid Button Type");
                    }
                }
            }

            /**
             * Set the customization for a particular button
             *
             * @param buttonCustomization The button customization data
             * @param buttonType The type of button to customize
             * @throws RuntimeException If any customization data is invalid
             */
            @NonNull
            public Builder setButtonCustomization(
                    @NonNull Stripe3ds2ButtonCustomization buttonCustomization,
                    @NonNull ButtonType buttonType)
                    throws RuntimeException {
                mUiCustomization.setButtonCustomization(buttonCustomization.mButtonCustomization,
                        getUiButtonType(buttonType));
                return this;
            }

            /**
             * Set the customization data for the 3DS2 toolbar
             *
             * @param toolbarCustomization Toolbar customization data
             * @throws RuntimeException If any customization data is invalid
             */
            @NonNull
            public Builder setToolbarCustomization(
                    @NonNull Stripe3ds2ToolbarCustomization toolbarCustomization)
                    throws RuntimeException {
                mUiCustomization
                        .setToolbarCustomization(toolbarCustomization.mToolbarCustomization);
                return this;
            }

            /**
             * Set the 3DS2 label customization
             *
             * @param labelCustomization Label customization data
             * @throws RuntimeException If any customization data is invalid
             */
            @NonNull
            public Builder setLabelCustomization(
                    @NonNull Stripe3ds2LabelCustomization labelCustomization)
                    throws RuntimeException {
                mUiCustomization.setLabelCustomization(labelCustomization.mLabelCustomization);
                return this;
            }

            /**
             * Set the 3DS2 text box customization
             *
             * @param textBoxCustomization Text box customization data
             * @throws RuntimeException If any customization data is invalid
             */
            @NonNull
            public Builder setTextBoxCustomization(
                    @NonNull Stripe3ds2TextBoxCustomization textBoxCustomization)
                    throws RuntimeException {
                mUiCustomization
                        .setTextBoxCustomization(textBoxCustomization.mTextBoxCustomization);
                return this;
            }

            /**
             * Set the accent color
             *
             * @param hexColor The accent color in the format #RRGGBB or #AARRGGBB
             * @throws RuntimeException If the color cannot be parsed
             */
            @NonNull
            public Builder setAccentColor(@NonNull String hexColor)
                    throws RuntimeException {
                mUiCustomization.setAccentColor(hexColor);
                return this;
            }

            /**
             * Build the UI customization
             *
             * @return the built UI customization
             */
            @NonNull
            public Stripe3ds2UiCustomization build() {
                return new Stripe3ds2UiCustomization(mUiCustomization);
            }
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || (obj instanceof Stripe3ds2UiCustomization &&
                    typedEquals((Stripe3ds2UiCustomization) obj));
        }

        private boolean typedEquals(@NonNull Stripe3ds2UiCustomization uiCustomization) {
            return Objects.equals(mUiCustomization, uiCustomization.mUiCustomization);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mUiCustomization);
        }
    }
}
