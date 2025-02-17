package com.stripe.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.stripe.android.CardUtils;
import com.stripe.android.R;
import com.stripe.android.model.Address;
import com.stripe.android.model.Card;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_CARD;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_CVC;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_EXPIRY;
import static com.stripe.android.view.CardInputListener.FocusField.FOCUS_POSTAL;

/**
 * A multiline card input widget using the support design library's {@link TextInputLayout}
 * to match Material Design.
 */
public class CardMultilineWidget extends LinearLayout implements CardWidget {

    static final String CARD_MULTILINE_TOKEN = "CardMultilineView";
    static final long CARD_NUMBER_HINT_DELAY = 120L;
    static final long COMMON_HINT_DELAY = 90L;

    @NonNull private final CardNumberEditText mCardNumberEditText;
    @NonNull private final ExpiryDateEditText mExpiryDateEditText;
    @NonNull private final StripeEditText mCvcEditText;
    @NonNull private final StripeEditText mPostalCodeEditText;
    @NonNull private final TextInputLayout mCardNumberTextInputLayout;
    @NonNull private final TextInputLayout mExpiryTextInputLayout;
    @NonNull private final TextInputLayout mCvcTextInputLayout;
    @NonNull private final TextInputLayout mPostalInputLayout;

    @Nullable private CardInputListener mCardInputListener;

    private boolean mIsEnabled;
    private boolean mShouldShowPostalCode;
    private boolean mHasAdjustedDrawable;
    @Nullable private String mCustomCvcLabel;

    @Card.CardBrand private String mCardBrand;
    @ColorInt private final int mTintColorInt;

    @Nullable private String mCardHintText;

    public CardMultilineWidget(@NonNull Context context) {
        this(context, null);
    }

    public CardMultilineWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardMultilineWidget(@NonNull Context context, @Nullable AttributeSet attrs,
                               int defStyleAttr) {
        this(context, attrs, defStyleAttr, false);
    }

    @VisibleForTesting
    CardMultilineWidget(@NonNull Context context, boolean shouldShowPostalCode) {
        this(context, null, 0, shouldShowPostalCode);
    }

    private CardMultilineWidget(@NonNull Context context, @Nullable AttributeSet attrs,
                                int defStyleAttr, boolean shouldShowPostalCode) {
        super(context, attrs, defStyleAttr);
        mShouldShowPostalCode = shouldShowPostalCode;
        mCardHintText = getResources().getString(R.string.card_number_hint);

        setOrientation(VERTICAL);
        inflate(getContext(), R.layout.card_multiline_widget, this);

        mCardNumberEditText = findViewById(R.id.et_add_source_card_number_ml);
        mExpiryDateEditText = findViewById(R.id.et_add_source_expiry_ml);
        mCvcEditText = findViewById(R.id.et_add_source_cvc_ml);
        mPostalCodeEditText = findViewById(R.id.et_add_source_postal_ml);
        mTintColorInt = mCardNumberEditText.getHintTextColors().getDefaultColor();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mCardNumberEditText.setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_NUMBER);
            mExpiryDateEditText.setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE);
            mCvcEditText.setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE);
            mPostalCodeEditText.setAutofillHints(View.AUTOFILL_HINT_POSTAL_CODE);
        }

        mCardBrand = Card.CardBrand.UNKNOWN;
        // This sets the value of mShouldShowPostalCode
        checkAttributeSet(attrs);

        mCardNumberTextInputLayout = findViewById(R.id.tl_add_source_card_number_ml);
        mExpiryTextInputLayout = findViewById(R.id.tl_add_source_expiry_ml);
        // We dynamically set the hint of the CVC field, so we need to keep a reference.
        mCvcTextInputLayout = findViewById(R.id.tl_add_source_cvc_ml);
        mPostalInputLayout = findViewById(R.id.tl_add_source_postal_ml);

        if (mShouldShowPostalCode) {
            // Set the label/hint to the shorter value if we have three things in a row.
            mExpiryTextInputLayout.setHint(getResources().getString(R.string.expiry_label_short));
        }

        initTextInputLayoutErrorHandlers(
                mCardNumberTextInputLayout,
                mExpiryTextInputLayout,
                mCvcTextInputLayout,
                mPostalInputLayout);

        initErrorMessages();
        initFocusChangeListeners();
        initDeleteEmptyListeners();

        mCardNumberEditText.setCardBrandChangeListener(
                new CardNumberEditText.CardBrandChangeListener() {
                    @Override
                    public void onCardBrandChanged(@NonNull @Card.CardBrand String brand) {
                        updateBrand(brand);
                    }
                });

        mCardNumberEditText.setCardNumberCompleteListener(
                new CardNumberEditText.CardNumberCompleteListener() {
                    @Override
                    public void onCardNumberComplete() {
                        mExpiryDateEditText.requestFocus();
                        if (mCardInputListener != null) {
                            mCardInputListener.onCardComplete();
                        }
                    }
                });

        mExpiryDateEditText.setExpiryDateEditListener(
                new ExpiryDateEditText.ExpiryDateEditListener() {
                    @Override
                    public void onExpiryDateComplete() {
                        mCvcEditText.requestFocus();
                        if (mCardInputListener != null) {
                            mCardInputListener.onExpirationComplete();
                        }
                    }
                });

        mCvcEditText.setAfterTextChangedListener(
                new StripeEditText.AfterTextChangedListener() {
                    @Override
                    public void onTextChanged(String text) {
                        if (ViewUtils.isCvcMaximalLength(mCardBrand, text)) {
                            updateBrand(mCardBrand);
                            if (mShouldShowPostalCode) {
                                mPostalCodeEditText.requestFocus();
                            }
                            if (mCardInputListener != null) {
                                mCardInputListener.onCvcComplete();
                            }
                        } else {
                            flipToCvcIconIfNotFinished();
                        }
                        mCvcEditText.setShouldShowError(false);
                    }
                });

        adjustViewForPostalCodeAttribute();

        mPostalCodeEditText.setAfterTextChangedListener(
                new StripeEditText.AfterTextChangedListener() {
                    @Override
                    public void onTextChanged(String text) {
                        if (isPostalCodeMaximalLength(true, text)
                                && mCardInputListener != null) {
                            mCardInputListener.onPostalCodeComplete();
                        }
                        mPostalCodeEditText.setShouldShowError(false);
                    }
                });

        mCardNumberEditText.updateLengthFilter();
        updateBrand(Card.CardBrand.UNKNOWN);
        setEnabled(true);
    }

    /**
     * Clear all entered data and hide all error messages.
     */
    @Override
    public void clear() {
        mCardNumberEditText.setText("");
        mExpiryDateEditText.setText("");
        mCvcEditText.setText("");
        mPostalCodeEditText.setText("");
        mCardNumberEditText.setShouldShowError(false);
        mExpiryDateEditText.setShouldShowError(false);
        mCvcEditText.setShouldShowError(false);
        mPostalCodeEditText.setShouldShowError(false);
        updateBrand(Card.CardBrand.UNKNOWN);
    }

    /**
     * @param cardInputListener A {@link CardInputListener} to be notified of changes
     *                          to the user's focused field
     */
    @Override
    public void setCardInputListener(@Nullable CardInputListener cardInputListener) {
        mCardInputListener = cardInputListener;
    }

    @Override
    public void setCardHint(@NonNull String cardHint) {
        mCardHintText = cardHint;
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
        if (validateAllFields()) {
            final int[] cardDate = mExpiryDateEditText.getValidDateFields();

            return new PaymentMethodCreateParams.Card.Builder()
                    .setNumber(mCardNumberEditText.getCardNumber())
                    .setCvc(mCvcEditText.getText().toString())
                    .setExpiryMonth(cardDate[0])
                    .setExpiryYear(cardDate[1]).build();
        }

        return null;
    }

    /**
     * @return a valid {@link PaymentMethodCreateParams} object based on user input, or `null` if
     * any field is invalid. The object will include any billing details that the user entered.
     */
    @Nullable
    @Override
    public PaymentMethodCreateParams getPaymentMethodCreateParams() {
        final PaymentMethodCreateParams.Card card = getPaymentMethodCard();
        if (card == null) {
            return null;
        }

        return PaymentMethodCreateParams.create(card, getPaymentMethodBillingDetails());
    }

    /**
     * @return a valid {@link PaymentMethod.BillingDetails} object based on user input, or
     * {@code null} if any field is invalid
     */
    @Nullable
    public PaymentMethod.BillingDetails getPaymentMethodBillingDetails() {
        final PaymentMethod.BillingDetails.Builder builder =
                getPaymentMethodBillingDetailsBuilder();
        return builder != null ? builder.build() : null;
    }

    /**
     * @return a valid {@link PaymentMethod.BillingDetails.Builder} object based on user input, or
     * {@code null} if any field is invalid
     */
    @Nullable
    public PaymentMethod.BillingDetails.Builder getPaymentMethodBillingDetailsBuilder() {
        if (mShouldShowPostalCode && validateAllFields()) {
            return new PaymentMethod.BillingDetails.Builder()
                    .setAddress(new Address.Builder()
                            .setPostalCode(mPostalCodeEditText.getText().toString())
                            .build()
                    );
        } else {
            return null;
        }
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
        final Card.Builder cardBuilder = getCardBuilder();
        return cardBuilder != null ? cardBuilder.build() : null;
    }

    @Override
    @Nullable
    public Card.Builder getCardBuilder() {
        if (!validateAllFields()) {
            return null;
        }

        final String cardNumber = mCardNumberEditText.getCardNumber();
        final int[] cardDate = Objects.requireNonNull(mExpiryDateEditText.getValidDateFields());
        final String cvcValue = mCvcEditText.getText().toString();

        return new Card.Builder(cardNumber, cardDate[0], cardDate[1], cvcValue)
                .addressZip(mShouldShowPostalCode ?
                        mPostalCodeEditText.getText().toString() : null)
                .loggingTokens(new ArrayList<>(Collections.singletonList(CARD_MULTILINE_TOKEN)));
    }

    /**
     * Validates all fields and shows error messages if appropriate.
     *
     * @return {@code true} if all shown fields are valid, {@code false} otherwise
     */
    public boolean validateAllFields() {
        final boolean cardNumberIsValid =
                CardUtils.isValidCardNumber(mCardNumberEditText.getCardNumber());
        final boolean expiryIsValid = mExpiryDateEditText.getValidDateFields() != null &&
                mExpiryDateEditText.isDateValid();
        final boolean cvcIsValid = isCvcLengthValid();
        mCardNumberEditText.setShouldShowError(!cardNumberIsValid);
        mExpiryDateEditText.setShouldShowError(!expiryIsValid);
        mCvcEditText.setShouldShowError(!cvcIsValid);
        final boolean postalCodeIsValidOrGone;
        if (mShouldShowPostalCode) {
            postalCodeIsValidOrGone = isPostalCodeMaximalLength(true,
                    mPostalCodeEditText.getText().toString());
            mPostalCodeEditText.setShouldShowError(!postalCodeIsValidOrGone);
        } else {
            postalCodeIsValidOrGone = true;
        }

        final List<StripeEditText> fields = Arrays.asList(
                mCardNumberEditText, mExpiryDateEditText, mCvcEditText, mPostalCodeEditText
        );
        for (StripeEditText field : fields) {
            if (field.getShouldShowError()) {
                field.requestFocus();
                break;
            }
        }

        return cardNumberIsValid
                && expiryIsValid
                && cvcIsValid
                && postalCodeIsValidOrGone;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            updateBrand(mCardBrand);
        }
    }

    /**
     * Set an optional CVC field label to override defaults, or <code>null</code> to use defaults.
     */
    public void setCvcLabel(@Nullable String cvcLabel) {
        mCustomCvcLabel = cvcLabel;
        updateCvc();
    }

    public void setShouldShowPostalCode(boolean shouldShowPostalCode) {
        mShouldShowPostalCode = shouldShowPostalCode;
        adjustViewForPostalCodeAttribute();
    }

    /**
     * Set the card number. Method does not change text field focus.
     *
     * @param cardNumber card number to be set
     */
    @Override
    public void setCardNumber(@Nullable String cardNumber) {
        mCardNumberEditText.setText(cardNumber);
    }

    @Override
    public void setExpiryDate(@IntRange(from = 1, to = 12) int month,
                              @IntRange(from = 0, to = 9999) int year) {
        mExpiryDateEditText.setText(DateUtils.createDateStringFromIntegerInput(month, year));
    }

    @Override
    public void setCvcCode(@Nullable String cvcCode) {
        mCvcEditText.setText(cvcCode);
    }

    /**
     * Checks whether the current card number is valid
     */
    public boolean validateCardNumber() {
        final boolean cardNumberIsValid =
                CardUtils.isValidCardNumber(mCardNumberEditText.getCardNumber());
        mCardNumberEditText.setShouldShowError(!cardNumberIsValid);
        return cardNumberIsValid;
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
        mCvcEditText.addTextChangedListener(cvcNumberTextWatcher);
    }

    /**
     * Expose a text watcher to receive updates when the cvc number is changed.
     */
    public void setPostalCodeTextWatcher(@Nullable TextWatcher postalCodeTextWatcher) {
        mPostalCodeEditText.addTextChangedListener(postalCodeTextWatcher);
    }

    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mExpiryTextInputLayout.setEnabled(enabled);
        mCardNumberTextInputLayout.setEnabled(enabled);
        mCvcTextInputLayout.setEnabled(enabled);
        mPostalInputLayout.setEnabled(enabled);
        mIsEnabled = enabled;
    }

    void adjustViewForPostalCodeAttribute() {
        // Set the label/hint to the shorter value if we have three things in a row.
        @StringRes final int expiryLabel = mShouldShowPostalCode
                ? R.string.expiry_label_short
                : R.string.acc_label_expiry_date;
        mExpiryTextInputLayout.setHint(getResources().getString(expiryLabel));

        @IdRes final int focusForward = mShouldShowPostalCode
                ? R.id.et_add_source_postal_ml
                : NO_ID;
        mCvcEditText.setNextFocusForwardId(focusForward);
        mCvcEditText.setNextFocusDownId(focusForward);

        final int postalCodeVisibility = mShouldShowPostalCode ? View.VISIBLE : View.GONE;
        mPostalInputLayout.setVisibility(postalCodeVisibility);

        // If the postal code field is not shown, the CVC field is the last one in the form and the
        // action on the keyboard when the CVC field is focused should be "Done". Otherwise, show
        // the "Next" action.
        mCvcEditText.setImeOptions(postalCodeVisibility == View.GONE ?
                EditorInfo.IME_ACTION_DONE : EditorInfo.IME_ACTION_NEXT);

        final int marginPixels = mShouldShowPostalCode
                ? getResources().getDimensionPixelSize(R.dimen.add_card_expiry_middle_margin)
                : 0;
        final LinearLayout.LayoutParams linearParams =
                (LinearLayout.LayoutParams) mCvcTextInputLayout.getLayoutParams();
        linearParams.setMargins(0, 0, marginPixels, 0);
        linearParams.setMarginEnd(marginPixels);

        mCvcTextInputLayout.setLayoutParams(linearParams);
    }

    static boolean isPostalCodeMaximalLength(boolean isZip, @Nullable String text) {
        return isZip && text != null && text.length() == 5;
    }

    private boolean isCvcLengthValid() {
        final int cvcLength = mCvcEditText.getText().toString().trim().length();
        if (TextUtils.equals(Card.CardBrand.AMERICAN_EXPRESS, mCardBrand)
                && cvcLength == Card.CVC_LENGTH_AMERICAN_EXPRESS) {
            return true;
        } else {
            return cvcLength == Card.CVC_LENGTH_COMMON;
        }
    }

    private void checkAttributeSet(@Nullable AttributeSet attrs) {
        if (attrs != null) {
            final TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.CardMultilineWidget,
                    0, 0);

            try {
                mShouldShowPostalCode =
                        a.getBoolean(R.styleable.CardMultilineWidget_shouldShowPostalCode, false);
            } finally {
                a.recycle();
            }
        }
    }

    private void flipToCvcIconIfNotFinished() {
        if (ViewUtils.isCvcMaximalLength(mCardBrand, mCvcEditText.getText().toString())) {
            return;
        }

        @DrawableRes final int resourceId = Card.CardBrand.AMERICAN_EXPRESS.equals(mCardBrand)
                ? R.drawable.ic_cvc_amex
                : R.drawable.ic_cvc;

        updateDrawable(resourceId, true);
    }

    @StringRes
    private int getCvcHelperText() {
        return Card.CardBrand.AMERICAN_EXPRESS.equals(mCardBrand)
                ? R.string.cvc_multiline_helper_amex
                : R.string.cvc_multiline_helper;
    }

    private int getDynamicBufferInPixels() {
        final float pixelsToAdjust = getResources()
                .getDimension(R.dimen.card_icon_multiline_padding_bottom);
        return new BigDecimal(pixelsToAdjust)
                .setScale(0, RoundingMode.HALF_DOWN)
                .intValue();
    }

    private void initDeleteEmptyListeners() {
        mExpiryDateEditText.setDeleteEmptyListener(
                new BackUpFieldDeleteListener(mCardNumberEditText));

        mCvcEditText.setDeleteEmptyListener(
                new BackUpFieldDeleteListener(mExpiryDateEditText));

        // It doesn't matter whether or not the postal code is shown;
        // we can still say where you go when you delete an empty field from it.
        mPostalCodeEditText.setDeleteEmptyListener(
                new BackUpFieldDeleteListener(mCvcEditText));
    }

    private void initErrorMessages() {
        mCardNumberEditText.setErrorMessage(getContext().getString(R.string.invalid_card_number));
        mExpiryDateEditText.setErrorMessage(getContext().getString(R.string.invalid_expiry_year));
        mCvcEditText.setErrorMessage(getContext().getString(R.string.invalid_cvc));
        mPostalCodeEditText.setErrorMessage(getContext().getString(R.string.invalid_zip));
    }

    private void initFocusChangeListeners() {
        mCardNumberEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mCardNumberEditText.setHintDelayed(mCardHintText, CARD_NUMBER_HINT_DELAY);
                    if (mCardInputListener != null) {
                        mCardInputListener.onFocusChange(FOCUS_CARD);
                    }
                } else {
                    mCardNumberEditText.setHint("");
                }
            }
        });

        mExpiryDateEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mExpiryDateEditText.setHintDelayed(
                            R.string.expiry_date_hint, COMMON_HINT_DELAY);
                    if (mCardInputListener != null) {
                        mCardInputListener.onFocusChange(FOCUS_EXPIRY);
                    }
                } else {
                    mExpiryDateEditText.setHint("");
                }
            }
        });

        mCvcEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    flipToCvcIconIfNotFinished();
                    mCvcEditText.setHintDelayed(getCvcHelperText(), COMMON_HINT_DELAY);
                    if (mCardInputListener != null) {
                        mCardInputListener.onFocusChange(FOCUS_CVC);
                    }
                } else {
                    updateBrand(mCardBrand);
                    mCvcEditText.setHint("");
                }
            }
        });

        mPostalCodeEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!mShouldShowPostalCode) {
                    return;
                }
                if (hasFocus) {
                    mPostalCodeEditText.setHintDelayed(R.string.zip_helper, COMMON_HINT_DELAY);
                    if (mCardInputListener != null) {
                        mCardInputListener.onFocusChange(FOCUS_POSTAL);
                    }
                } else {
                    mPostalCodeEditText.setHint("");
                }
            }
        });
    }

    private void initTextInputLayoutErrorHandlers(@NonNull TextInputLayout cardInputLayout,
                                                  @NonNull TextInputLayout expiryInputLayout,
                                                  @NonNull TextInputLayout cvcTextInputLayout,
                                                  @NonNull TextInputLayout postalInputLayout) {
        mCardNumberEditText.setErrorMessageListener(new ErrorListener(cardInputLayout));
        mExpiryDateEditText.setErrorMessageListener(new ErrorListener(expiryInputLayout));
        mCvcEditText.setErrorMessageListener(new ErrorListener(cvcTextInputLayout));
        mPostalCodeEditText.setErrorMessageListener(new ErrorListener(postalInputLayout));
    }

    private void updateBrand(@NonNull @Card.CardBrand String brand) {
        mCardBrand = brand;
        updateCvc();
        updateDrawable(Card.getBrandIcon(brand), Card.CardBrand.UNKNOWN.equals(brand));
    }

    private void updateCvc() {
        final int maxLength = Card.CardBrand.AMERICAN_EXPRESS.equals(mCardBrand) ?
                Card.CVC_LENGTH_AMERICAN_EXPRESS : Card.CVC_LENGTH_COMMON;
        mCvcEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        mCvcTextInputLayout.setHint(generateCvcLabel());
    }

    @NonNull
    private String generateCvcLabel() {
        if (mCustomCvcLabel != null) {
            return mCustomCvcLabel;
        } else {
            return getResources().getString(Card.CardBrand.AMERICAN_EXPRESS.equals(mCardBrand) ?
                    R.string.cvc_amex_hint : R.string.cvc_number_hint);
        }
    }

    private void updateDrawable(@DrawableRes int iconResourceId, boolean needsTint) {
        final Drawable icon = ContextCompat.getDrawable(getContext(), iconResourceId);
        final Drawable[] drawables = mCardNumberEditText.getCompoundDrawablesRelative();
        final Drawable original = drawables[0];
        if (original == null) {
            return;
        }

        final Rect copyBounds = new Rect();
        original.copyBounds(copyBounds);

        final int iconPadding = mCardNumberEditText.getCompoundDrawablePadding();

        if (!mHasAdjustedDrawable) {
            copyBounds.top = copyBounds.top - getDynamicBufferInPixels();
            copyBounds.bottom = copyBounds.bottom - getDynamicBufferInPixels();
            mHasAdjustedDrawable = true;
        }

        icon.setBounds(copyBounds);
        final Drawable compatIcon = DrawableCompat.wrap(icon);
        if (needsTint) {
            DrawableCompat.setTint(compatIcon.mutate(), mTintColorInt);
        }

        mCardNumberEditText.setCompoundDrawablePadding(iconPadding);
        mCardNumberEditText.setCompoundDrawablesRelative(compatIcon, null, null, null);
    }

}
