package io.agora.scene.widget.toast;

import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.RestrictTo;

import java.lang.reflect.Field;

/**
 * The type Internal hook toast.
 */
@RestrictTo({RestrictTo.Scope.LIBRARY})
public final class InternalHookToast {

    private InternalHookToast() {
    }

    private static Field sFieldTN;
    private static Field sFieldTNHandler;

    static {
        try {
            Class<?> clazz = Toast.class;
            sFieldTN = clazz.getDeclaredField("mTN");
            sFieldTN.setAccessible(true);
            sFieldTNHandler = sFieldTN.getType().getDeclaredField("mHandler");
            sFieldTNHandler.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hook.
     *
     * @param toast the toast
     */
    public static void hook(Toast toast) {
        try {
            Object tn = sFieldTN.get(toast);
            Handler preHandler = (Handler) sFieldTNHandler.get(tn);
            sFieldTNHandler.set(tn, new SafelyHandler(preHandler));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The type Safely handler.
     */
    public static class SafelyHandler extends Handler {

        private final Handler impl;

        /**
         * Instantiates a new Safely handler.
         *
         * @param impl the
         */
        public SafelyHandler(Handler impl) {
            this.impl = impl;
        }

        @Override
        public void dispatchMessage(Message msg) {
            try {
                super.dispatchMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void handleMessage(Message msg) {
            impl.handleMessage(msg);
        }
    }

}
