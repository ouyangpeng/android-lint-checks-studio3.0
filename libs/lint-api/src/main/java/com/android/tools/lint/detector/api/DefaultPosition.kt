/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.tools.lint.detector.api

import com.google.common.annotations.Beta

/**
 * A simple offset-based position
 *
 * **NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.**
 */
@Beta
class DefaultPosition(
        /** The line number (0-based where the first line is line 0)  */
        override val line: Int,
        /**
         * The column number (where the first character on the line is 0), or -1 if
         * unknown
         */
        override val column: Int,
        /** The character offset  */
        override val offset: Int) : Position()
