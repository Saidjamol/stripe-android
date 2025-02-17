package com.stripe.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Transformation;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.stripe.android.R;
import com.stripe.android.model.Card;
import com.stripe.android.model.PaymentMethodCreateParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

import static com.stripe.android.model.Card.CardBrand;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_CARD;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_CVC;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_EXPIRY;

/**
 * A card input widget that handles all animation on its own.
 */
public class CardInputWidget extends LinearLayout implements CardWidget {

    static final String LOGGING_TOKEN = "CardInputView";

    private static final String PEEK_TEXT_COMMON = "4242";
    private static final String PEEK_TEXT_DINERS = "88";
    private static final String PEEK_TEXT_AMEX = "34343";

    private static final String CVC_PLACEHOLDER_COMMON = "CVC";
    private static final String CVC_PLACEHOLDER_AMEX = "2345";

    // These intentionally include a space at the end.
    private static final String HIDDEN_TEXT_AMEX = "3434 343434 ";
    private static final String HIDDEN_TEXT_COMMON = "4242 4242 4242 ";

    private static final String FULL_SIZING_CARD_TEXT = "4242 4242 4242 4242";
    private static final String FULL_SIZING_DATE_TEXT = "MM/MM";

    private static final String EXTRA_CARD_VIEWED = "extra_card_viewed";
    private static final String EXTRA_SUPER_STATE = "extra_super_state";

    // This value is used to ensure that onSaveInstanceState is called
    // in the event that the user doesn't give this control an ID.
    private static final @IdRes int DEFAULT_READER_ID = R.id.default_reader_id;

    private static final long ANIMATION_LENGTH = 150L;

    @NonNull private final ImageView mCardIconImageView;
    @NonNull private final FrameLayout mFrameLayout;
    @NonNull private final CardNumberEditText mCardNumberEditText;
    @NonNull private final StripeEditText mCvcNumberEditText;
    @NonNull private final ExpiryDateEditText mExpiryDateEditText;

    @Nullable private CardInputListener mCardInputListener;
    private boolean mCardNumberIsViewed = true;

    @ColorInt private int mTintColorInt;

    private boolean mIsAmEx;
    private boolean mInitFlag;

    private int mTotalLengthInPixels;

    @Nullable private DimensionOverrideSettings mDimensionOverrides;
    @NonNull private final PlacementParameters mPlacementParameters;

    public CardInputWidget(@NonNull Context context) {
        this(context, null);
    }

    public CardInputWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardInputWidget(@NonNull Context context, @Nullable AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(getContext(), R.layout.card_input_widget, this);

        // This ensures that onRestoreInstanceState is called
        // during rotations.
        if (getId() == NO_ID) {
            setId(DEFAULT_READER_ID);
        }

        setOrientation(LinearLayout.HORIZONTAL);
        setMinimumWidth(getResources().getDimensionPixelSize(R.dimen.card_widget_min_width));

        mPlacementParameters = new PlacementParameters();
        mCardIconImageView = findViewById(R.id.iv_card_icon);
        mCardNumberEditText = findViewById(R.id.et_card_number);
        mExpiryDateEditText = findViewById(R.id.et_expiry_date);
        mCvcNumberEditText = findViewById(R.id.et_cvc_number);
        mFrameLayout = findViewById(R.id.frame_container);

        initView(attrs);
    }

    /**
     * Gets a {@link PaymentMethodCreateParams.Card} object from the user input, if all fields are
     * valid. If not, returns {@code null}.
     *
     * @return a valid {@link PaymentMethodCreateParams.Card} object based on user input, or
     * {@code null} if any field is invalid
     */
    @Nullable
    @Override
    public PaymentMethodCreateParams.Card getPaymentMethodCard() {
        final String cardNumber = mCardNumberEditText.getCardNumber();
        final int[] cardDate = mExpiryDateEditText.getValidDateFields();
        final String cvcValue = Objects.requireNonNull(mCvcNumberEditText.getText())
                .toString().trim();

        // CVC/CVV is the only field not validated by the entry control itself, so we check here.
        if (cardNumber == null || cardDate == null || cardDate.length != 2 ||
                !isCvcLengthValid(cvcValue)) {
            return null;
        }

        return new PaymentMethodCreateParams.Card.Builder()
                .setNumber(cardNumber)
                .setCvc(cvcValue)
                .setExpiryMonth(cardDate[0])
                .setExpiryYear(cardDate[1]).build();
    }

    @Nullable
    @Override
    public PaymentMethodCreateParams getPaymentMethodCreateParams() {
        final PaymentMethodCreateParams.Card card = getPaymentMethodCard();
        if (card == null) {
            return null;
        }

        return PaymentMethodCreateParams.create(card);
    }

    /**
     * Gets a {@link Card} object from the user input, if all fields are valid. If not, returns
     * {@code null}.
     *
     * @return a valid {@link Card} object based on user input, or {@code null} if any field is
     * invalid
     */
    @Override
    @Nullable
    public Card getCard() {
        final Card.Builder builder = getCardBuilder();
        return builder != null ? builder.build() : null;
    }

    @Override
    @Nullable
    public Card.Builder getCardBuilder() {
        final String cardNumber = mCardNumberEditText.getCardNumber();
        final int[] cardDate = mExpiryDateEditText.getValidDateFields();
        if (cardNumber == null || cardDate == null || cardDate.length != 2) {
            return null;
        }

        // CVC/CVV is the only field not validated by the entry control itself, so we check here.
        final String cvcValue = Objects.requireNonNull(mCvcNumberEditText.getText())
                .toString().trim();
        if (!isCvcLengthValid(cvcValue)) {
            return null;
        }

        return new Card.Builder(cardNumber, cardDate[0], cardDate[1], cvcValue)
                .loggingTokens(new ArrayList<>(Collections.singletonList(LOGGING_TOKEN)));
    }

    /**
     * Set a {@link CardInputListener} to be notified of card input events.
     *
     * @param listener the listener
     */
    @Override
    public void setCardInputListener(@Nullable CardInputListener listener) {
        mCardInputListener = listener;
    }

    /**
     * Set the card number. Method does not change text field focus.
     *
     * @param cardNumber card number to be set
     */
    @Override
    public void setCardNumber(@Nullable String cardNumber) {
        mCardNumberEditText.setText(cardNumber);
        setCardNumberIsViewed(!mCardNumberEditText.isCardNumberValid());
    }

    @Override
    public void setCardHint(@NonNull String cardHint) {
        mCardNumberEditText.setHint(cardHint);
    }

    /**
     * Set the expiration date. Method invokes completion listener and changes focus
     * to the CVC field if a valid date is entered.
     *
     * Note that while a four-digit and two-digit year will both work, information
     * beyond the tens digit of a year will be truncated. Logic elsewhere in the SDK
     * makes assumptions about what century is implied by various two-digit years, and
     * will override any information provided here.
     *
     * @param month a month of the year, represented as a number between 1 and 12
     * @param year a year number, either in two-digit form or four-digit form
     */
    @Override
    public void setExpiryDate(
            @IntRange(from = 1, to = 12) int month,
            @IntRange(from = 0, to = 9999) int year) {
        mExpiryDateEditText.setText(DateUtils.createDateStringFromIntegerInput(month, year));
    }

    /**
     * Set the CVC value for the card. Note that the maximum length is assumed to
     * be 3, unless the brand of the card has already been set (by setting the card number).
     *
     * @param cvcCode the CVC value to be set
     */
    @Override
    public void setCvcCode(@Nullable String cvcCode) {
        mCvcNumberEditText.setText(cvcCode);
    }

    /**
     * Clear all text fields in the CardInputWidget.
     */
    @Override
    public void clear() {
        if (mCardNumberEditText.hasFocus()
                || mExpiryDateEditText.hasFocus()
                || mCvcNumberEditText.hasFocus()
                || this.hasFocus()) {
            mCardNumberEditText.requestFocus();
        }
        mCvcNumberEditText.setText("");
        mExpiryDateEditText.setText("");
        mCardNumberEditText.setText("");
    }

    /**
     * Enable or disable text fields
     *
     * @param isEnabled boolean indicating whether fields should be enabled
     */
    public void setEnabled(boolean isEnabled) {
        mCardNumberEditText.setEnabled(isEnabled);
        mExpiryDateEditText.setEnabled(isEnabled);
        mCvcNumberEditText.setEnabled(isEnabled);
    }

    /**
     * Expose a text watcher to receive updates when the card number is changed.
     */
    public void setCardNumberTextWatcher(@Nullable TextWatcher cardNumberTextWatcher) {
        mCardNumberEditText.addTextChangedListener(cardNumberTextWatcher);
    }

    /**
     * Expose a text watcher to receive updates when the expiry date is changed.
     */
    public void setExpiryDateTextWatcher(@Nullable TextWatcher expiryDateTextWatcher) {
        mExpiryDateEditText.addTextChangedListener(expiryDateTextWatcher);
    }

    /**
     * Expose a text watcher to receive updates when the cvc number is changed.
     */
    public void setCvcNumberTextWatcher(@Nullable TextWatcher cvcNumberTextWatcher) {
        mCvcNumberEditText.addTextChangedListener(cvcNumberTextWatcher);
    }

    /**
     * Override of {@link View#isEnabled()} that returns {@code true} only
     * if all three sub-controls are enabled.
     *
     * @return {@code true} if the card number field, expiry field, and cvc field are enabled,
     * {@code false} otherwise
     */
    @Override
    public boolean isEnabled() {
        return mCardNumberEditText.isEnabled() &&
                mExpiryDateEditText.isEnabled() &&
                mCvcNumberEditText.isEnabled();
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        if (ev.getAction() != MotionEvent.ACTION_DOWN) {
            return super.onInterceptTouchEvent(ev);
        }

        StripeEditText focusEditText = getFocusRequestOnTouch((int) ev.getX());
        if (focusEditText != null) {
            focusEditText.requestFocus();
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @NonNull
    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_SUPER_STATE, super.onSaveInstanceState());
        bundle.putBoolean(EXTRA_CARD_VIEWED, mCardNumberIsViewed);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundleState = (Bundle) state;
            mCardNumberIsViewed = bundleState.getBoolean(EXTRA_CARD_VIEWED, true);
            updateSpaceSizes(mCardNumberIsViewed);
            mTotalLengthInPixels = getFrameWidth();
            int cardMargin, dateMargin, cvcMargin;
            if (mCardNumberIsViewed) {
                cardMargin = 0;
                dateMargin = mPlacementParameters.cardWidth
                        + mPlacementParameters.cardDateSeparation;
                cvcMargin = mTotalLengthInPixels;
            } else {
                cardMargin = -1 * mPlacementParameters.hiddenCardWidth;
                dateMargin = mPlacementParameters.peekCardWidth
                        + mPlacementParameters.cardDateSeparation;
                cvcMargin = dateMargin
                        + mPlacementParameters.dateWidth
                        + mPlacementParameters.dateCvcSeparation;
            }

            setLayoutValues(mPlacementParameters.cardWidth, cardMargin, mCardNumberEditText);
            setLayoutValues(mPlacementParameters.dateWidth, dateMargin, mExpiryDateEditText);
            setLayoutValues(mPlacementParameters.cvcWidth, cvcMargin, mCvcNumberEditText);

            super.onRestoreInstanceState(bundleState.getParcelable(EXTRA_SUPER_STATE));
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    /**
     * Checks on the horizontal position of a touch event to see if
     * that event needs to be associated with one of the controls even
     * without having actually touched it. This essentially gives a larger
     * touch surface to the controls. We return {@code null} if the user touches
     * actually inside the widget because no interception is necessary - the touch will
     * naturally give focus to that control, and we don't want to interfere with what
     * Android will naturally do in response to that touch.
     *
     * @param touchX distance in pixels from the left side of this control
     * @return a {@link StripeEditText} that needs to request focus, or {@code null}
     * if no such request is necessary.
     */
    @VisibleForTesting
    @Nullable
    StripeEditText getFocusRequestOnTouch(int touchX) {
        int frameStart = mFrameLayout.getLeft();

        if (mCardNumberIsViewed) {
            // Then our view is
            // |CARDVIEW||space||DATEVIEW|

            if (touchX < frameStart + mPlacementParameters.cardWidth) {
                // Then the card edit view will already handle this touch.
                return null;
            } else if (touchX < mPlacementParameters.cardTouchBufferLimit){
                // Then we want to act like this was a touch on the card view
                return mCardNumberEditText;
            } else if (touchX < mPlacementParameters.dateStartPosition) {
                // Then we act like this was a touch on the date editor.
                return mExpiryDateEditText;
            } else {
                // Then the date editor will already handle this touch.
                return null;
            }
        } else {
            // Our view is
            // |PEEK||space||DATE||space||CVC|
            if (touchX < frameStart + mPlacementParameters.peekCardWidth) {
                // This was a touch on the card number editor, so we don't need to handle it.
                return null;
            } else if (touchX < mPlacementParameters.cardTouchBufferLimit) {
                // Then we need to act like the user touched the card editor
                return mCardNumberEditText;
            } else if (touchX < mPlacementParameters.dateStartPosition) {
                // Then we need to act like this was a touch on the date editor
                return mExpiryDateEditText;
            } else if (touchX < mPlacementParameters.dateStartPosition +
                    mPlacementParameters.dateWidth) {
                // Just a regular touch on the date editor.
                return null;
            } else if (touchX < mPlacementParameters.dateRightTouchBufferLimit) {
                // We need to act like this was a touch on the date editor
                return mExpiryDateEditText;
            } else if (touchX < mPlacementParameters.cvcStartPosition) {
                // We need to act like this was a touch on the cvc editor.
                return mCvcNumberEditText;
            } else {
                return null;
            }
        }
    }

    @VisibleForTesting
    void setDimensionOverrideSettings(@Nullable DimensionOverrideSettings dimensonOverrides) {
        mDimensionOverrides = dimensonOverrides;
    }

    @VisibleForTesting
    void setCardNumberIsViewed(boolean cardNumberIsViewed) {
        mCardNumberIsViewed = cardNumberIsViewed;
    }

    @NonNull
    @VisibleForTesting
    PlacementParameters getPlacementParameters() {
        return mPlacementParameters;
    }

    @VisibleForTesting
    void updateSpaceSizes(boolean isCardViewed) {
        int frameWidth = getFrameWidth();
        int frameStart = mFrameLayout.getLeft();
        if (frameWidth == 0) {
            // This is an invalid view state.
            return;
        }

        mPlacementParameters.cardWidth =
                getDesiredWidthInPixels(FULL_SIZING_CARD_TEXT, mCardNumberEditText);

        mPlacementParameters.dateWidth =
                getDesiredWidthInPixels(FULL_SIZING_DATE_TEXT, mExpiryDateEditText);

        @Card.CardBrand String brand = mCardNumberEditText.getCardBrand();
        mPlacementParameters.hiddenCardWidth =
                getDesiredWidthInPixels(getHiddenTextForBrand(brand), mCardNumberEditText);

        mPlacementParameters.cvcWidth =
                getDesiredWidthInPixels(getCvcPlaceHolderForBrand(brand), mCvcNumberEditText);

        mPlacementParameters.peekCardWidth =
                getDesiredWidthInPixels(getPeekCardTextForBrand(brand), mCardNumberEditText);

        if (isCardViewed) {
            mPlacementParameters.cardDateSeparation = frameWidth
                    - mPlacementParameters.cardWidth - mPlacementParameters.dateWidth;
            mPlacementParameters.cardTouchBufferLimit = frameStart
                    + mPlacementParameters.cardWidth + mPlacementParameters.cardDateSeparation / 2;
            mPlacementParameters.dateStartPosition = frameStart
                    + mPlacementParameters.cardWidth + mPlacementParameters.cardDateSeparation;
        } else {
            mPlacementParameters.cardDateSeparation = frameWidth / 2
                    - mPlacementParameters.peekCardWidth
                    - mPlacementParameters.dateWidth / 2;
            mPlacementParameters.dateCvcSeparation = frameWidth
                    - mPlacementParameters.peekCardWidth
                    - mPlacementParameters.cardDateSeparation
                    - mPlacementParameters.dateWidth
                    - mPlacementParameters.cvcWidth;

            mPlacementParameters.cardTouchBufferLimit = frameStart
                    + mPlacementParameters.peekCardWidth
                    + mPlacementParameters.cardDateSeparation / 2;
            mPlacementParameters.dateStartPosition = frameStart
                    + mPlacementParameters.peekCardWidth
                    + mPlacementParameters.cardDateSeparation;
            mPlacementParameters.dateRightTouchBufferLimit =
                    mPlacementParameters.dateStartPosition
                            + mPlacementParameters.dateWidth
                            + mPlacementParameters.dateCvcSeparation / 2;
            mPlacementParameters.cvcStartPosition = mPlacementParameters.dateStartPosition
                    + mPlacementParameters.dateWidth
                    + mPlacementParameters.dateCvcSeparation;
        }
    }

    private boolean isCvcLengthValid(@NonNull String cvcValue) {
        final int cvcLength = cvcValue.length();
        if (mIsAmEx && cvcLength == Card.CVC_LENGTH_AMERICAN_EXPRESS) {
            return true;
        }

        return cvcLength == Card.CVC_LENGTH_COMMON;
    }

    private void setLayoutValues(int width, int margin, @NonNull StripeEditText editText) {
        final FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) editText.getLayoutParams();
        layoutParams.width = width;
        layoutParams.leftMargin = margin;
        editText.setLayoutParams(layoutParams);
    }

    private int getDesiredWidthInPixels(@NonNull String text, @NonNull StripeEditText editText) {
        return mDimensionOverrides == null
                ? (int) Layout.getDesiredWidth(text, editText.getPaint())
                : mDimensionOverrides.getPixelWidth(text, editText);
    }

    private int getFrameWidth() {
        return mDimensionOverrides == null
                ? mFrameLayout.getWidth()
                : mDimensionOverrides.getFrameWidth();
    }

    private void initView(@Nullable AttributeSet attrs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mCardNumberEditText.setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_NUMBER);
            mExpiryDateEditText.setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE);
            mCvcNumberEditText.setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE);
        }

        ViewCompat.setAccessibilityDelegate(mCvcNumberEditText, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(
                    @NonNull View host,
                    @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                final String accLabel = getResources().getString(
                        R.string.acc_label_cvc_node,
                        mCvcNumberEditText.getText()
                );
                info.setText(accLabel);
            }

        });

        mCardNumberIsViewed = true;

        @ColorInt int errorColorInt = mCardNumberEditText.getDefaultErrorColorInt();
        mTintColorInt = mCardNumberEditText.getHintTextColors().getDefaultColor();
        String cardHintText = null;
        if (attrs != null) {
            final TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.CardInputView,
                    0, 0);

            try {
                mTintColorInt =
                        a.getColor(R.styleable.CardInputView_cardTint, mTintColorInt);
                errorColorInt =
                        a.getColor(R.styleable.CardInputView_cardTextErrorColor, errorColorInt);
                cardHintText =
                        a.getString(R.styleable.CardInputView_cardHintText);
            } finally {
                a.recycle();
            }
        }

        if (cardHintText != null) {
            mCardNumberEditText.setHint(cardHintText);
        }
        mCardNumberEditText.setErrorColor(errorColorInt);
        mExpiryDateEditText.setErrorColor(errorColorInt);
        mCvcNumberEditText.setErrorColor(errorColorInt);

        mCardNumberEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    scrollLeft();
                    if (mCardInputListener != null) {
                        mCardInputListener.onFocusChange(FOCUS_CARD);
                    }
                }
            }
        });

        mExpiryDateEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    scrollRight();
                    if (mCardInputListener != null) {
                        mCardInputListener.onFocusChange(FOCUS_EXPIRY);
                    }
                }
            }
        });

        mExpiryDateEditText.setDeleteEmptyListener(
                new BackUpFieldDeleteListener(mCardNumberEditText));

        mCvcNumberEditText.setDeleteEmptyListener(
                new BackUpFieldDeleteListener(mExpiryDateEditText));

        mCvcNumberEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    scrollRight();
                    if (mCardInputListener != null) {
                        mCardInputListener.onFocusChange(FOCUS_CVC);
                    }
                }
                updateIconCvc(
                        mCardNumberEditText.getCardBrand(),
                        hasFocus,
                        Objects.requireNonNull(mCvcNumberEditText.getText()).toString()
                );
            }
        });

        mCvcNumberEditText.setAfterTextChangedListener(
                new StripeEditText.AfterTextChangedListener() {
                    @Override
                    public void onTextChanged(String text) {
                        if (mCardInputListener != null && ViewUtils.isCvcMaximalLength
                                (mCardNumberEditText.getCardBrand(), text)) {
                            mCardInputListener.onCvcComplete();
                        }
                        updateIconCvc(mCardNumberEditText.getCardBrand(),
                                mCvcNumberEditText.hasFocus(),
                                text);
                    }
                }
        );

        mCardNumberEditText.setCardNumberCompleteListener(
                new CardNumberEditText.CardNumberCompleteListener() {
                    @Override
                    public void onCardNumberComplete() {
                        scrollRight();
                        if (mCardInputListener != null) {
                            mCardInputListener.onCardComplete();
                        }
                    }
                });

        mCardNumberEditText.setCardBrandChangeListener(
                new CardNumberEditText.CardBrandChangeListener() {
                    @Override
                    public void onCardBrandChanged(@NonNull @Card.CardBrand String brand) {
                        mIsAmEx = Card.CardBrand.AMERICAN_EXPRESS.equals(brand);
                        updateIcon(brand);
                        updateCvc(brand);
                    }
                });

        mExpiryDateEditText.setExpiryDateEditListener(
                new ExpiryDateEditText.ExpiryDateEditListener() {
                    @Override
                    public void onExpiryDateComplete() {
                        mCvcNumberEditText.requestFocus();
                        if (mCardInputListener != null) {
                            mCardInputListener.onExpirationComplete();
                        }
                    }
                }
        );

        mCardNumberEditText.requestFocus();
    }

    private void scrollLeft() {
        if (mCardNumberIsViewed || !mInitFlag) {
            return;
        }

        final int dateStartPosition =
                mPlacementParameters.peekCardWidth + mPlacementParameters.cardDateSeparation;
        final int cvcStartPosition =
                dateStartPosition
                        + mPlacementParameters.dateWidth + mPlacementParameters.dateCvcSeparation;

        updateSpaceSizes(true);

        final int startPoint = ((FrameLayout.LayoutParams)
                mCardNumberEditText.getLayoutParams()).leftMargin;
        Animation slideCardLeftAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) mCardNumberEditText.getLayoutParams();
                params.leftMargin = (int) (startPoint * (1 - interpolatedTime));
                mCardNumberEditText.setLayoutParams(params);
            }
        };

        final int dateDestination =
                mPlacementParameters.cardWidth + mPlacementParameters.cardDateSeparation;
        Animation slideDateLeftAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                int tempValue =
                        (int) (interpolatedTime * dateDestination
                                + (1 - interpolatedTime) * dateStartPosition);
                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) mExpiryDateEditText.getLayoutParams();
                params.leftMargin = tempValue;
                mExpiryDateEditText.setLayoutParams(params);
            }
        };

        final int cvcDestination = cvcStartPosition + (dateDestination - dateStartPosition);
        Animation slideCvcLeftAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                int tempValue =
                        (int) (interpolatedTime * cvcDestination
                                + (1 - interpolatedTime) * cvcStartPosition);
                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) mCvcNumberEditText.getLayoutParams();
                params.leftMargin = tempValue;
                params.rightMargin = 0;
                params.width = mPlacementParameters.cvcWidth;
                mCvcNumberEditText.setLayoutParams(params);
            }
        };

        slideCardLeftAnimation.setAnimationListener(new AnimationEndListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mCardNumberEditText.requestFocus();
            }
        });

        slideCardLeftAnimation.setDuration(ANIMATION_LENGTH);
        slideDateLeftAnimation.setDuration(ANIMATION_LENGTH);
        slideCvcLeftAnimation.setDuration(ANIMATION_LENGTH);

        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(slideCardLeftAnimation);
        animationSet.addAnimation(slideDateLeftAnimation);
        animationSet.addAnimation(slideCvcLeftAnimation);
        mFrameLayout.startAnimation(animationSet);
        mCardNumberIsViewed = true;
    }

    private void scrollRight() {
        if (!mCardNumberIsViewed || !mInitFlag) {
            return;
        }

        final int dateStartMargin = mPlacementParameters.cardWidth
                + mPlacementParameters.cardDateSeparation;

        updateSpaceSizes(false);

        Animation slideCardRightAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                FrameLayout.LayoutParams cardParams =
                        (FrameLayout.LayoutParams) mCardNumberEditText.getLayoutParams();
                cardParams.leftMargin =
                        (int) (-1 * mPlacementParameters.hiddenCardWidth * interpolatedTime);
                mCardNumberEditText.setLayoutParams(cardParams);
            }
        };

        final int dateDestination =
                mPlacementParameters.peekCardWidth
                        + mPlacementParameters.cardDateSeparation;

        Animation slideDateRightAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                int tempValue =
                        (int) (interpolatedTime * dateDestination
                                + (1 - interpolatedTime) * dateStartMargin);
                FrameLayout.LayoutParams dateParams =
                        (FrameLayout.LayoutParams) mExpiryDateEditText.getLayoutParams();
                dateParams.leftMargin = tempValue;
                mExpiryDateEditText.setLayoutParams(dateParams);
            }
        };

        final int cvcDestination =
                mPlacementParameters.peekCardWidth
                        + mPlacementParameters.cardDateSeparation
                        + mPlacementParameters.dateWidth
                        + mPlacementParameters.dateCvcSeparation;
        final int cvcStartMargin = cvcDestination + (dateStartMargin - dateDestination);

        Animation slideCvcRightAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                int tempValue =
                        (int) (interpolatedTime * cvcDestination
                                + (1 - interpolatedTime) * cvcStartMargin);
                FrameLayout.LayoutParams cardParams =
                        (FrameLayout.LayoutParams) mCvcNumberEditText.getLayoutParams();
                cardParams.leftMargin = tempValue;
                cardParams.rightMargin = 0;
                cardParams.width = mPlacementParameters.cvcWidth;
                mCvcNumberEditText.setLayoutParams(cardParams);
            }
        };

        slideCardRightAnimation.setDuration(ANIMATION_LENGTH);
        slideDateRightAnimation.setDuration(ANIMATION_LENGTH);
        slideCvcRightAnimation.setDuration(ANIMATION_LENGTH);

        slideCardRightAnimation.setAnimationListener(new AnimationEndListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mExpiryDateEditText.requestFocus();
            }
        });

        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(slideCardRightAnimation);
        animationSet.addAnimation(slideDateRightAnimation);
        animationSet.addAnimation(slideCvcRightAnimation);

        mFrameLayout.startAnimation(animationSet);
        mCardNumberIsViewed = false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            applyTint(false);
        }
    }

    /**
     * Determines whether or not the icon should show the card brand instead of the
     * CVC helper icon.
     *
     * @param brand the {@link Card.CardBrand} in question, used for determining max length
     * @param cvcHasFocus {@code true} if the CVC entry field has focus, {@code false} otherwise
     * @param cvcText the current content of {@link #mCvcNumberEditText}
     * @return {@code true} if we should show the brand of the card, or {@code false} if we
     * should show the CVC helper icon instead
     */
    @VisibleForTesting
    static boolean shouldIconShowBrand(
            @NonNull @Card.CardBrand String brand,
            boolean cvcHasFocus,
            @Nullable String cvcText) {
        if (!cvcHasFocus) {
            return true;
        }
        return ViewUtils.isCvcMaximalLength(brand, cvcText);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (!mInitFlag && getWidth() != 0) {
            mInitFlag = true;
            mTotalLengthInPixels = getFrameWidth();

            updateSpaceSizes(mCardNumberIsViewed);

            int cardLeftMargin = mCardNumberIsViewed
                    ? 0 : -1 * mPlacementParameters.hiddenCardWidth;
            setLayoutValues(mPlacementParameters.cardWidth, cardLeftMargin, mCardNumberEditText);

            int dateMargin = mCardNumberIsViewed
                    ? mPlacementParameters.cardWidth + mPlacementParameters.cardDateSeparation
                    : mPlacementParameters.peekCardWidth + mPlacementParameters.cardDateSeparation;
            setLayoutValues(mPlacementParameters.dateWidth, dateMargin, mExpiryDateEditText);

            int cvcMargin = mCardNumberIsViewed
                    ? mTotalLengthInPixels
                    : mPlacementParameters.peekCardWidth
                    + mPlacementParameters.cardDateSeparation
                    + mPlacementParameters.dateWidth
                    + mPlacementParameters.dateCvcSeparation;
            setLayoutValues(mPlacementParameters.cvcWidth, cvcMargin, mCvcNumberEditText);
        }
    }

    @NonNull
    private String getHiddenTextForBrand(@NonNull @CardBrand String brand) {
        if (Card.CardBrand.AMERICAN_EXPRESS.equals(brand)) {
            return HIDDEN_TEXT_AMEX;
        } else {
            return HIDDEN_TEXT_COMMON;
        }
    }

    @NonNull
    private String getCvcPlaceHolderForBrand(@NonNull @Card.CardBrand String brand) {
        if (Card.CardBrand.AMERICAN_EXPRESS.equals(brand)) {
            return CVC_PLACEHOLDER_AMEX;
        } else {
            return CVC_PLACEHOLDER_COMMON;
        }
    }

    @NonNull
    private String getPeekCardTextForBrand(@NonNull @Card.CardBrand String brand) {
        switch (brand) {
            case Card.CardBrand.AMERICAN_EXPRESS: {
                return PEEK_TEXT_AMEX;
            }
            case Card.CardBrand.DINERS_CLUB: {
                return PEEK_TEXT_DINERS;
            }
            default: {
                return PEEK_TEXT_COMMON;
            }
        }
    }

    private void applyTint(boolean isCvc) {
        if (isCvc || Card.CardBrand.UNKNOWN.equals(mCardNumberEditText.getCardBrand())) {
            Drawable icon = mCardIconImageView.getDrawable();
            Drawable compatIcon = DrawableCompat.wrap(icon);
            DrawableCompat.setTint(compatIcon.mutate(), mTintColorInt);
            mCardIconImageView.setImageDrawable(DrawableCompat.unwrap(compatIcon));
        }
    }

    private void updateCvc(@NonNull @Card.CardBrand String brand) {
        if (Card.CardBrand.AMERICAN_EXPRESS.equals(brand)) {
            mCvcNumberEditText.setFilters(
                    new InputFilter[] {
                            new InputFilter.LengthFilter(Card.CVC_LENGTH_AMERICAN_EXPRESS)});
            mCvcNumberEditText.setHint(R.string.cvc_amex_hint);
        } else {
            mCvcNumberEditText.setFilters(
                    new InputFilter[] {new InputFilter.LengthFilter(Card.CVC_LENGTH_COMMON)});
            mCvcNumberEditText.setHint(R.string.cvc_number_hint);
        }
    }

    private void updateIcon(@NonNull @Card.CardBrand String brand) {
        if (Card.CardBrand.UNKNOWN.equals(brand)) {
            Drawable icon  = getResources().getDrawable(R.drawable.ic_unknown);
            mCardIconImageView.setImageDrawable(icon);
            applyTint(false);
        } else {
            mCardIconImageView.setImageResource(Card.getBrandIcon(brand));
        }
    }

    private void updateIconCvc(
            @NonNull @CardBrand String brand,
            boolean hasFocus,
            @Nullable String cvcText) {
        if (shouldIconShowBrand(brand, hasFocus, cvcText)) {
            updateIcon(brand);
        } else {
            updateIconForCvcEntry(Card.CardBrand.AMERICAN_EXPRESS.equals(brand));
        }
    }

    private void updateIconForCvcEntry(boolean isAmEx) {
        if (isAmEx) {
            mCardIconImageView.setImageResource(R.drawable.ic_cvc_amex);
        } else {
            mCardIconImageView.setImageResource(R.drawable.ic_cvc);
        }
        applyTint(true);
    }

    /**
     * Interface useful for testing calculations without generating real views.
     */
    @VisibleForTesting
    interface DimensionOverrideSettings {
        int getPixelWidth(@NonNull String text, @NonNull EditText editText);

        int getFrameWidth();
    }

    /**
     * A data-dump class.
     */
    static class PlacementParameters {
        int cardWidth;
        int hiddenCardWidth;
        int peekCardWidth;
        int cardDateSeparation;
        int dateWidth;
        int dateCvcSeparation;
        int cvcWidth;

        int cardTouchBufferLimit;
        int dateStartPosition;
        int dateRightTouchBufferLimit;
        int cvcStartPosition;

        @NonNull
        @Override
        public String toString() {
            final String touchBufferData = String.format(Locale.ENGLISH,
                    "Touch Buffer Data:\n" +
                            "CardTouchBufferLimit = %d\n" +
                            "DateStartPosition = %d\n" +
                            "DateRightTouchBufferLimit = %d\n" +
                            "CvcStartPosition = %d",
                    cardTouchBufferLimit,
                    dateStartPosition,
                    dateRightTouchBufferLimit,
                    cvcStartPosition);
            final String elementSizeData = String.format(Locale.ENGLISH,
                    "CardWidth = %d\n" +
                            "HiddenCardWidth = %d\n" +
                            "PeekCardWidth = %d\n" +
                            "CardDateSeparation = %d\n" +
                            "DateWidth = %d\n" +
                            "DateCvcSeparation = %d\n" +
                            "CvcWidth = %d\n",
                    cardWidth,
                    hiddenCardWidth,
                    peekCardWidth,
                    cardDateSeparation,
                    dateWidth,
                    dateCvcSeparation,
                    cvcWidth);
            return elementSizeData + touchBufferData;
        }
    }

    /**
     * A convenience class for when we only want to listen for when an animation ends.
     */
    private abstract class AnimationEndListener implements Animation.AnimationListener {
        @Override
        public void onAnimationStart(Animation animation) {
            // Intentional No-op
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            // Intentional No-op
        }
    }

}
