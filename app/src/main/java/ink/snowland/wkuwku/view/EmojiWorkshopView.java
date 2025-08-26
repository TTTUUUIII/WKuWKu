package ink.snowland.wkuwku.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import ink.snowland.wkuwku.R;
import ink.snowland.wkuwku.util.PoissonDiskSampler;

public class EmojiWorkshopView extends View {
    private static final String DEFAULT_SOURCE = "\uD83D\uDC22☺️⭐️";
    private static Typeface typeface;
    private static final SparseArray<List<Icon>> sIconsCache = new SparseArray<>();
    private final Paint mPaint = new Paint();
    private Options mOptions = new Options(DEFAULT_SOURCE, 40, 40);
    public EmojiWorkshopView(Context context) {
        this(context, null);
    }

    public EmojiWorkshopView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (typeface == null) {
            typeface = ResourcesCompat.getFont(context, R.font.noto_emoji_variable_font_wght);
        }
        mPaint.setTypeface(typeface);
        mPaint.setTextAlign(Paint.Align.CENTER);
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(R.attr.colorOnBackground, typedValue, true)) {
            mPaint.setColor(typedValue.data);
        }
        mPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
    }

    public void setOptions(Options options) {
        mOptions = options;
        mPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, options.fontSize, getResources().getDisplayMetrics()));
        sIconsCache.clear();
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        String[] characters = getCharacters();
        if (characters.length == 0) return;
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        int orientation = getResources().getConfiguration().orientation;
        List<Icon> icons = sIconsCache.get(orientation);
        if (icons == null) {
            float textWidth = mPaint.measureText("⭐️");
            PoissonDiskSampler sampler = new PoissonDiskSampler(getWidth(), getHeight(), TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mOptions.minDistance, getResources().getDisplayMetrics()) + textWidth / 2, 10);
            List<PoissonDiskSampler.Point> points = sampler.generatePoints();
            icons = new ArrayList<>();
            for (PoissonDiskSampler.Point point : points) {
                icons.add(new Icon(point, characters[ThreadLocalRandom.current().nextInt(characters.length)]));
            }
            sIconsCache.put(orientation, icons);
        }
        for (Icon icon : icons) {
            double baseLine = icon.pos.y - (fontMetrics.ascent + fontMetrics.descent) / 2;
            canvas.drawText(icon.cha, (float) icon.pos.x, (int)baseLine, mPaint);
        }
    }

    private String[] getCharacters() {
        int[] points = mOptions.source.codePoints().toArray();
        Set<String> sources = new ArraySet<>();
        for (int point : points) {
            sources.add(new String(Character.toChars(point)));
        }
        return sources.toArray(new String[0]);
    }

    private static class Icon {
        final PoissonDiskSampler.Point pos;
        final String cha;

        public Icon(PoissonDiskSampler.Point pos, String cha) {
            this.pos = pos;
            this.cha = cha;
        }
    }

    public static class Options {
        private String source;
        private int minDistance;
        private int fontSize;

        public Options(String source, int minDistance, int fontSize) {
            this.source = source;
            this.minDistance = minDistance;
            this.fontSize = fontSize;
        }

        public Options() {
        }

        public void setSource(String source) {
            this.source = source;
        }

        public void setMinDistance(int minDistance) {
            this.minDistance = minDistance;
        }

        public void setFontSize(int fontSize) {
            this.fontSize = fontSize;
        }

        public String getSource() {
            return source;
        }

        public int getMinDistance() {
            return minDistance;
        }

        public int getFontSize() {
            return fontSize;
        }
    }
}
