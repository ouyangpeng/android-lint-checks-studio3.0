/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.tools.lint.checks.infrastructure;

import com.android.ide.common.res2.ResourceItem;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.Map;

class ResourceRepositories {
    private ResourceRepositories() {}

    static void sortItemLists(ListMultimap<String, ResourceItem> multimap) {
        ListMultimap<String, ResourceItem> sorted = ArrayListMultimap.create();
        Ordering<ResourceItem> ordering = Ordering.natural().onResultOf(ResourceItem::getKey);
        for (Map.Entry<String, Collection<ResourceItem>> entry : multimap.asMap().entrySet()) {
            sorted.putAll(entry.getKey(), ordering.sortedCopy(entry.getValue()));
        }

        multimap.clear();
        multimap.putAll(sorted);
    }
}
