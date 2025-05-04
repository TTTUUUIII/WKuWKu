package ink.snowland.wkuwku.view;

import android.content.Context;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import java.util.Objects;

public class AutoMarqueeTextView extends AppCompatTextView {
    private boolean mIsInitialized;
    private boolean mNoWidthLimit;
    private int mOneBlankWidth;
    private int mWidth;

    private CharSequence mText;
    private TextPaint mPaint;

    public AutoMarqueeTextView(@NonNull Context context) {
        this(context, null);
    }

    public AutoMarqueeTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        postInitialize(() -> {
            synchronized (this) {
                notify();
            }
        });
    }

    @Override
    public void setText(@NonNull CharSequence text, BufferType type) {
        super.setText(text, type);
        if (text.toString().isEmpty()) return;
        if (mIsInitialized) {
            super.setText(marqueeText(text), type);
        } else {
            new Thread(() -> {
                synchronized (this) {
                    try {
                        wait(300);
                        post(() -> {
                            super.setText(marqueeText(text), type);
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    @Override
    public boolean isFocused() {
        return true;
    }

    @Override
    public CharSequence getText() {
        return super.getText().toString().trim();
    }

    public void clearText() {
        mText = "";
        super.setText(mText, BufferType.NORMAL);
    }

    public void setNoWidthLimit(boolean noWidthLimit) {
        mNoWidthLimit = noWidthLimit;
        if (Objects.nonNull(mText)) setText(mText);
    }

    private CharSequence marqueeText(@NonNull CharSequence content)
    {
        String text = content.toString();
        if (mNoWidthLimit) {
            final int textWidth = Math.round(mPaint.measureText(text));
            final int blankWidth = mWidth - textWidth;
            if (blankWidth >= 0)
            {
                final StringBuilder newText = new StringBuilder(text);
                for (int i = 0; i < blankWidth / mOneBlankWidth + 1; ++i)
                {
                    newText.append(" ");
                }
                text = newText.toString();
            }
        }
        mText = text;
        return text;
    }

    private void postInitialize(@NonNull Runnable initializedAction) {
        mPaint = getPaint();
        mOneBlankWidth = Math.round(mPaint.measureText(" "));
        setEllipsize(TextUtils.TruncateAt.MARQUEE);
        setMarqueeRepeatLimit(-1);
        setSingleLine(true);
        post(() -> {
            mWidth = getMeasuredWidth();
            mIsInitialized = true;
            initializedAction.run();
        });
    }
}
