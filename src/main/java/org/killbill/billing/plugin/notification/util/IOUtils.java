package org.killbill.billing.plugin.notification.util;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;

import java.io.IOException;
import java.io.InputStream;

public class IOUtils {
    public static String toString(final InputStream stream) throws IOException {
        final InputSupplier<InputStream> inputSupplier = new InputSupplier<InputStream>() {
            @Override
            public InputStream getInput() throws IOException {
                return stream;
            }
        };

        return CharStreams.toString(CharStreams.newReaderSupplier(inputSupplier, Charsets.UTF_8));
    }
}
