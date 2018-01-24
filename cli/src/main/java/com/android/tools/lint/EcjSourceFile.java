/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.lint;

import static com.android.SdkConstants.UTF_8;

import com.android.annotations.NonNull;
import com.android.utils.CharSequences;
import java.io.File;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;

/**
 * Source file for ECJ. Subclassed to let us hold on to the String contents (ECJ operates
 * on char[]'s exclusively, whereas for PSI we'll need Strings) and serve it back quickly.
 */
public class EcjSourceFile extends CompilationUnit {
    private final File file;

    private EcjSourceFile(@NonNull char[] source, @NonNull String path,
            @NonNull String encoding, @NonNull File file) {
        super(source, path, encoding);
        this.file = file;
    }

    @NonNull
    public String getSource() {
        // Note: Not cached. This method is not expected to be called frequently or really
        // at all from normal lint checks.
        return new String(getContents());
    }

    @NonNull
    public File getFile() {
        return file;
    }


    public static EcjSourceFile create(@NonNull char[] source, @NonNull File file,
            @NonNull String encoding) {
        return new EcjSourceFile(source, file.getPath(), encoding, file);
    }

    public static EcjSourceFile create(@NonNull CharSequence source, @NonNull File file,
            @NonNull String encoding) {
        char[] contents = CharSequences.getCharArray(source);
        return new EcjSourceFile(contents, file.getPath(), encoding, file);
    }

    public static EcjSourceFile create(@NonNull CharSequence source, @NonNull File file) {
        return create(source, file, UTF_8);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EcjSourceFile that = (EcjSourceFile) o;

        return file.equals(that.file);

    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }
}
