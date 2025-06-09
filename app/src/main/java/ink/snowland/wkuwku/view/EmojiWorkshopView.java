package ink.snowland.wkuwku.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import ink.snowland.wkuwku.util.PoissonDiskSampler;
import ink.snowland.wkuwku.util.SettingsManager;

public class EmojiWorkshopView extends View {
    private static final String EMOJI_WORKSHOP_SOURCE = "app_emoji_workshop_source";
    private static final String EMOJI_WORKSHOP_EMOJI_SIZE = "app_emoji_workshop_emoji_size";
    private static final String DISTANCE_BETWEEN_EMOJIS = "app_distance_between_emojis";
    private static final String DEFAULT_SOURCE = "\uD83D\uDC22☺️⭐️";
    private static Typeface typeface;
    private final Paint mPaint = new Paint();
    private List<PoissonDiskSampler.Point> mPoints;
    private final int mDistanceBetweenEmojis = SettingsManager.getInt(DISTANCE_BETWEEN_EMOJIS, 40);
    private String[] mCharacters;
    public EmojiWorkshopView(Context context) {
        this(context, null);
    }

    public EmojiWorkshopView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (typeface == null) {
            typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/NotoEmoji-VariableFont_wght.ttf");
        }
        mPaint.setTypeface(typeface);
        mPaint.setTextAlign(Paint.Align.CENTER);
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(com.google.android.material.R.attr.backgroundColor, typedValue, true)) {
            mPaint.setColor(typedValue.data);
        }
        mPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
        setSource(SettingsManager.getString(EMOJI_WORKSHOP_SOURCE, DEFAULT_SOURCE), SettingsManager.getInt(EMOJI_WORKSHOP_EMOJI_SIZE, 40), false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mPoints == null) {
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();
            float textWidth = mPaint.measureText("⭐️");
            PoissonDiskSampler sampler = new PoissonDiskSampler(width, height, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mDistanceBetweenEmojis, getResources().getDisplayMetrics()) + textWidth / 2, 10);
            mPoints = sampler.generatePoints();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (mCharacters == null || mCharacters.length == 0) return;
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        for (PoissonDiskSampler.Point point : mPoints) {
            double baseLine = point.y - (fontMetrics.ascent + fontMetrics.descent) / 2;
            canvas.drawText(mCharacters[ThreadLocalRandom.current().nextInt(mCharacters.length)], (float) point.x, (int)baseLine, mPaint);
        }
    }

    private void setSource(@NonNull String s, int textSize) {
        setSource(s, textSize, true);
    }

    private void setSource(@NonNull String s, int textSize, boolean invalidate) {
        int[] points = s.codePoints().toArray();
        Set<String> sources = new ArraySet<>();
        for (int point : points) {
            sources.add(new String(Character.toChars(point)));
        }
        mCharacters = sources.toArray(new String[0]);
        mPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, textSize, getResources().getDisplayMetrics()));
        if (invalidate) {
            invalidate();
        }
    }
}
