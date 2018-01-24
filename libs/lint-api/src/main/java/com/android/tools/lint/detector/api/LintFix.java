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

package com.android.tools.lint.detector.api;

import static com.android.SdkConstants.ANDROID_URI;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nls;

/**
 * A <b>description</b> of a quickfix for a lint warning, which provides
 * structured data for use by the IDE to create an actual fix implementation.
 * For example, a {@linkplain LintFix} can state that it aims to set a given
 * attribute to a given value. When lint is running in the IDE, the quickfix
 * machinery will look at the {@linkplain LintFix} objects and add an actual
 * implementation which sets the attribute.
 * <p>
 * The set of operations is quite limited at the moment; more will be
 * added over time.
 */
public class LintFix {
    @Nls @Nullable public final String displayName;

    protected LintFix(@Nullable String displayName) {
        this.displayName = displayName;
    }

    /** Creates a new Quickfix Builder */
    @NonNull
    public static Builder create() {
        return new Builder();
    }

    /** Return display name */
    @Nls @Nullable
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Convenience wrapper which checks whether the given fix is a map, and
     * if so returns the value stored by its key
     */
    @Nullable
    public static <T> T getData(@Nullable LintFix fix, @NonNull Class<T> key) {
        if (fix instanceof DataMap) {
            return ((DataMap)fix).get(key);
        }

        return null;
    }

    /** Builder for creating various types of fixes */
    public static class Builder {
        @Nls protected String displayName;

        private Builder() {
        }

        /**
         * Sets display name. If not supplied a default will be created based on the
         * type of quickfix.
         *
         * @param displayName the display name
         * @return this
         */
        public Builder name(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * Creates a group of fixes
         */
        @SuppressWarnings("MethodMayBeStatic")
        public GroupBuilder group() {
            return new GroupBuilder(displayName).type(GroupType.ALTERNATIVES);
        }

        /**
         * Creates a composite fix: multiple lint fixes which will all be applied as
         * a single unit.
         * <p>
         * <b>NOTE:</b> Be careful combining multiple fixes that are potentially overlapping,
         * such as replace strings.
         *
         * The test infrastructure may not apply these correctly. This is primarily
         * intended for fixes that are clearly separate, such as setting multiple attributes.
         */
        @SuppressWarnings("MethodMayBeStatic")
        public GroupBuilder composite() {
            return new GroupBuilder(displayName).type(GroupType.COMPOSITE);
        }

        /**
         * Creates a composite fix: multiple lint fixes which will all be applied as
         * a single unit.
         * <p>
         * <b>NOTE:</b> Be careful combining multiple fixes that are potentially overlapping,
         * such as replace strings.
         *
         * The test infrastructure may not apply these correctly. This is primarily
         * intended for fixes that are clearly separate, such as setting multiple attributes.
         */
        @SuppressWarnings("MethodMayBeStatic")
        public LintFix composite(LintFix... fixes) {
            return new GroupBuilder(displayName).type(GroupType.COMPOSITE)
                    .join(fixes).build();
        }

        /**
         * Creates a fix list from a set of lint fixes. The IDE will show all of these
         * as separate options.
         *
         * @param fixes fixes to combine
         * @return a fix representing the list
         */
        @SuppressWarnings("MethodMayBeStatic")
        public LintFix group(LintFix... fixes) {
            return new GroupBuilder(displayName).join(fixes).build();
        }

        /**
         * Replace a string or regular expression
         * @return a string replace builder
         */
        public ReplaceStringBuilder replace() {
            return new ReplaceStringBuilder(displayName);
        }

        /**
         * Set or clear an attribute
         * @return a set attribute builder
         */
        public SetAttributeBuilder set() {
            return new SetAttributeBuilder(displayName);
        }

        /**
         * Clear an attribute
         * @return a set attribute builder
         */
        public SetAttributeBuilder unset() {
            return new SetAttributeBuilder(displayName).value(null);
        }

        /**
         * Sets a specific attribute
         * @return a set attribute builder
         */
        public SetAttributeBuilder set(@Nullable String namespace, @NonNull String attribute,
                @Nullable String value) {
            return new SetAttributeBuilder(displayName).namespace(namespace)
                    .attribute(attribute).value(value);
        }

        /**
         * Sets a specific attribute
         * @return a set attribute builder
         */
        public SetAttributeBuilder unset(@Nullable String namespace, @NonNull String attribute) {
            return new SetAttributeBuilder(displayName).namespace(namespace)
                    .attribute(attribute).value(null);
        }

        /**
         * Provides a map with details for the quickfix implementation
         */
        public FixMapBuilder map() { return new FixMapBuilder(displayName); }

        /**
         * Provides a map with details for the quickfix implementation, pre-initialized
         * with the given objects
         */
        @NonNull
        public FixMapBuilder map(@NonNull Object... args) {
            FixMapBuilder builder = map();

            for (Object arg : args) {
                builder.put(arg);
            }

            return builder;
        }

        /**
         * Passes one or more pieces of data; this will be transferred as a map
         * behind the scenes. This is a convenience wrapper around
         * {@link #map()} and {@link FixMapBuilder#build()}.
         *
         * @return a fix
         */
        @NonNull
        public LintFix data(@NonNull Object... args) {
            return map(args).build();
        }
    }

    /** Builder for constructing a group of fixes */
    public static class GroupBuilder {
        @Nls private String displayName;
        private GroupType type = GroupType.ALTERNATIVES;
        private final List<LintFix> list = Lists.newArrayListWithExpectedSize(4);

        /** Constructed from {@link Builder#set()} */
        private GroupBuilder(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Sets display name. If not supplied a default will be created based on the
         * type of quickfix.
         *
         * @param displayName the display name
         * @return this
         */
        public GroupBuilder name(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /** Adds the given fixes to this group */
        public GroupBuilder join(@NonNull LintFix... fixes) {
            list.addAll(Arrays.asList(fixes));
            return this;
        }

        /** Adds the given fix to this group */
        public GroupBuilder add(@NonNull LintFix fix) {
            list.add(fix);
            return this;
        }

        public GroupBuilder type(@NonNull GroupType type) {
            this.type = type;
            return this;
        }

        /** Construct a {@link LintFix} for this group of fixes */
        @NonNull
        public LintFix build() {
            assert !list.isEmpty();
            return new LintFixGroup(displayName, type, list);
        }
    }

    /** A builder for replacing strings */
    public static class ReplaceStringBuilder {
        @Nls protected String displayName;
        private String newText;
        private String oldText;
        private boolean shortenNames;
        private boolean reformat;
        @Language("RegExp") private String oldPattern;
        private Location range;

        /** Constructed from {@link Builder#replace()} */
        private ReplaceStringBuilder(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Sets display name. If not supplied a default will be created based on the
         * type of quickfix.
         *
         * @param displayName the display name
         * @return this
         */
        public ReplaceStringBuilder name(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /** Replaces the given pattern match (or the first group within it, if any) */
        public ReplaceStringBuilder pattern(@Language("RegExp") String oldPattern) {
            assert this.oldText == null;
            assert this.oldPattern == null;

            if (oldPattern.indexOf('(') == -1) {
                oldPattern = "(" + oldPattern + ")";
            }
            this.oldPattern = oldPattern;
            return this;
        }

        /** Replaces the given literal text */
        public ReplaceStringBuilder text(String oldText) {
            assert this.oldText == null;
            assert this.oldPattern == null;
            this.oldText = oldText;
            return this;
        }

        /**
         * Sets a location range to use for searching for the text or pattern. Useful
         * if you want to make a replacement that is larger than the error range
         * highlighted as the problem range.
         */
        public ReplaceStringBuilder range(@NonNull Location range) {
            this.range = range;
            return this;
        }

        /** Replaces this entire range */
        public ReplaceStringBuilder all() {
            return this;
        }

        /**
         * The text to replace the old text or pattern with.
         * Note that the special syntax \g{n} can be used to
         * reference the n'th group, if and only if this replacement
         * is using {@link #pattern(String)}}.
         */
        public ReplaceStringBuilder with(String newText) {
            assert this.newText == null;
            this.newText = newText;
            return this;
        }

        /** The IDE should simplify fully qualified names in the element after this fix has
         * been run (off by default) */
        public ReplaceStringBuilder shortenNames() {
            this.shortenNames = true;
            return this;
        }

        /** Whether the replaced range should be reformatted */
        public ReplaceStringBuilder reformat(boolean reformat) {
            this.reformat = reformat;
            return this;
        }

        /** Constructs a {@link LintFix} for this string replacement */
        @NonNull
        public LintFix build() {
            return new ReplaceString(displayName, oldText, oldPattern,
                    newText != null ? newText : "", shortenNames,
                    reformat, range);
        }
    }

    public static class SetAttributeBuilder {
        @Nls protected String displayName;
        private String attribute;
        private String namespace;
        private String value = "";
        private int mark = Integer.MIN_VALUE;
        private int dot = Integer.MIN_VALUE;

        /** Constructed from {@link Builder#set()} */
        private SetAttributeBuilder(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Sets display name. If not supplied a default will be created based on the
         * type of quickfix.
         *
         * @param displayName the display name
         * @return this
         */
        public SetAttributeBuilder name(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * Sets the namespace to the Android namespace (shortcut for {@link
         * #namespace(String)} passing in {@link SdkConstants#ANDROID_URI}
         */
        public SetAttributeBuilder android() {
            assert this.namespace == null;
            this.namespace = ANDROID_URI;
            return this;
        }

        /** Sets the namespace to the given namespace */
        public SetAttributeBuilder namespace(@Nullable String namespace) {
            assert this.namespace == null;
            this.namespace = namespace;
            return this;
        }

        /**
         * Sets the value to the given value. Null means delete (though it's
         * more natural to call {@link #remove(String)}
         */
        public SetAttributeBuilder value(@Nullable String value) {
            this.value = value;
            if (value != null && value.isEmpty()) {
                caret(0); // Setting to empty attribute normally means "let the user edit"
            }
            return this;
        }

        /** Sets the attribute name. Should not include the prefix. */
        public SetAttributeBuilder attribute(@NonNull String attribute) {
            assert attribute.indexOf(':') == -1 : attribute;
            assert this.attribute == null;
            this.attribute = attribute;
            return this;
        }

        /** Removes the given attribute */
        public SetAttributeBuilder remove(@NonNull String attribute) {
            assert this.attribute == null;
            assert attribute.indexOf(':') == -1 : attribute;
            this.attribute = attribute;
            this.value = null;
            return this;
        }

        /** Selects the newly inserted value */
        public SetAttributeBuilder selectAll() {
            assert value != null; // must be set first
            this.mark = 0;
            this.dot = value.length();
            return this;
        }

        /**
         * Sets the value to TＯDＯ meant for values that aren't optional.
         * You can also supply a prefix and/or a suffix.
         *
         * @param prefix optional prefix to add before the TＯDＯ marker
         * @param suffix optional suffix to add after the TＯDＯ marker
         * @return a builder for TＯDＯ edits
         */
        public SetAttributeBuilder todo(@Nullable String namespace, @NonNull String attribute,
                @Nullable String prefix, @Nullable String suffix) {
            namespace(namespace);
            attribute(attribute);
            StringBuilder sb = new StringBuilder();
            if (prefix != null) {
                sb.append(prefix);
            }
            int start = sb.length();
            sb.append("TODO");
            int end = sb.length();
            if (suffix != null) {
                sb.append(suffix);
            }
            value(sb.toString());
            select(start, end);
            return this;
        }

        /**
         * Sets the value to TＯDＯ meant for values that aren't optional.
         */
        public SetAttributeBuilder todo(@Nullable String namespace, @NonNull String attribute) {
            return todo(namespace, attribute, null, null);
        }

        /** Selects the value in the offset range (relative to value start */
        public SetAttributeBuilder select(int start, int end) {
            assert value != null; // must be set first
            this.mark = Math.min(start, end);
            this.dot = Math.max(start, end);
            return this;
        }

        /**
         * Moves the caret to the given offset (relative to the position
         * of the value text; can be negative ({@link Integer#MIN_VALUE means not set}
         */
        public SetAttributeBuilder caret(int valueStartDelta) {
            this.mark = this.dot = valueStartDelta;
            return this;
        }

        /**
         * Moves the caret to the beginning of the value after applying the new attribute
         */
        public SetAttributeBuilder caretBegin() {
            return caret(0);
        }


        /**
         * Moves the caret to the end of the value after applying the new attribute
         */
        public SetAttributeBuilder caretEnd() {
            assert value != null; // must be set first
            return caret(value.length());
        }

        /** Constructs a {@link LintFix} for this attribute operation */
        @NonNull
        public LintFix build() {
            return new SetAttribute(displayName, namespace, attribute, value,
                    dot, mark);
        }
    }

    public static class FixMapBuilder {
        @Nls protected final String displayName;

        /** Constructed from {@link Builder#map()} */
        private FixMapBuilder(String displayName) {
            this.displayName = displayName;
        }

        private final Map<Object, Object> map = Maps.newHashMapWithExpectedSize(4);

        /** Puts the given value into the map using its class as the key */
        public <T> FixMapBuilder put(@Nullable T value) {
            if (value == null) {
                return this;
            }
            Class<?> key = value.getClass();
            // Simplify keys such that you don't end up with ArrayList, HashMap etc
            // when you passed in something just typed List, Map, Set, etc
            if (value instanceof List) {
                key = List.class;
            } else if (value instanceof Map) {
                key = Map.class;
            } else if (value instanceof Set) {
                key = Set.class;
            }
            assert !map.containsKey(key);
            map.put(key, value);
            return this;
        }

        /** Puts the given value into the map using the given key */
        public FixMapBuilder put(@NonNull String key, @Nullable Object value) {
            if (value == null) {
                return this;
            }
            assert !map.containsKey(key);
            map.put(key, value);
            return this;
        }

        /** Constructs a {@link LintFix} with this map data */
        @NonNull
        public LintFix build() {
            return new DataMap(displayName, map);
        }
    }

    /**
     * General map storage for quickfix data; clients can look up via map keys or
     * types of values
     * <p>
     * This class/API is <b>only</b> intended for IDE use. Lint checks should be
     * accessing the builder class instead - {@link #create()}.
     */
    public static class DataMap extends LintFix implements Iterable {
        private final Map<Object, Object> map;

        private DataMap(@Nullable String displayName, Map<Object, Object> map) {
            super(displayName);
            this.map = map;
        }

        /** Returns the value for the given class key */
        @Nullable
        public <T> T get(@NonNull Class<T> key) {
            //noinspection unchecked
            T t = (T)map.get(key);
            if (t != null) {
                return t;
            }

            // See if there are other matches for this class.
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                Object k = entry.getKey();
                if (k instanceof Class &&
                        key.isAssignableFrom((Class<?>)k)) {
                    //noinspection unchecked
                    return (T)entry.getValue();
                }
            }

            return null;
        }

        /** Returns the value for the given String key */
        @Nullable
        public Object get(@NonNull String key) {
            //noinspection unchecked
            return map.get(key);
        }

        @NonNull
        @Override
        public Iterator iterator() {
            return map.values().iterator();
        }

        /** Returns the keys */
        public Set<Object> keys() {
            return map.keySet();
        }

        @Override
        public String toString() {
            return map.toString();
        }
    }

    /** Captures the various types of a {@link LintFixGroup} */
    public enum GroupType {
        /**
         * This group represents a single fix where all the fixes should be applied
         * as one
         */
        COMPOSITE,

        /**
         * This group represents separate fix alternatives the user can choose between
         */
        ALTERNATIVES
    }

    /**
     * A list of quickfixes
     * <p>
     * This class/API is <b>only</b> intended for IDE use. Lint checks should be
     * accessing the builder class instead - {@link #create()}.
     */
    public static class LintFixGroup extends LintFix {
        /** A list of fixes */
        @NonNull public final List<LintFix> fixes;

        /** The type of group */
        @NonNull public final GroupType type;

        public LintFixGroup(@Nullable String displayName, @NonNull GroupType type,
                @NonNull List<LintFix> fixes) {
            super(displayName);
            this.type = type;
            this.fixes = fixes;
        }

        @Nls
        @Nullable
        @Override
        public String getDisplayName() {
            // For composites, we can display the name of one of the actions
            if (displayName == null && type == GroupType.COMPOSITE) {
                for (LintFix fix : fixes) {
                    String name = fix.getDisplayName();
                    if (name != null) {
                        return name;
                    }
                }
            }

            return displayName;
        }
    }

    /**
     * Convenience class for the common scenario of suggesting a fix which involves
     * setting an XML attribute.
     * <p>
     * This class/API is <b>only</b> intended for IDE use. Lint checks should be
     * accessing the builder class instead - {@link #create()}.
     */
    public static class SetAttribute extends LintFix {
        /** The namespace */
        @Nullable public final String namespace;

        /** The local attribute name */
        @NonNull public final String attribute;

        /** The value (or null to delete the attribute) */
        @Nullable public final String value;

        /** The caret location to show, OR {@link Integer#MIN_VALUE} if not set.
         * If {@link #mark} is set, the end of the selection too. */
        public final int dot;

        /** The selection anchor, OR {@link Integer#MIN_VALUE} if not set */
        public final int mark;

        /**
         * Set or reset the given attribute
         *
         * @param displayName the displayName
         * @param namespace   optional name space
         * @param attribute   attribute name
         * @param value       value, or null to delete (if already set) or to edit (if already set)
         * @param dot   the caret position
         * @param mark  the selection end point (dot is the other)
         */
        private SetAttribute(
                @Nullable String displayName,
                @Nullable String namespace, @NonNull String attribute,
                @Nullable String value, int dot, int mark) {
            super(displayName);
            this.namespace = namespace;
            this.attribute = attribute;
            this.value = value;
            this.dot = dot;
            this.mark = mark;
        }

        /** Return display name */
        @NonNull
        @Override
        public String getDisplayName() {
            if (displayName != null) {
                return displayName;
            } else if (value != null) {
                if (value.isEmpty() || dot > 0) { // dot > 0: value is partial?
                    return "Set " + attribute;
                } else {
                    return "Set " + attribute + "=\"" + value + "\"";
                }
            } else {
                return "Delete " + attribute;
            }
        }
    }

    /**
     * Convenience class for the common scenario of suggesting a fix which involves
     * replacing a static string or regular expression with a replacement string
     * <p>
     * This class/API is <b>only</b> intended for IDE use. Lint checks should be
     * accessing the builder class instead - {@link #create()}.
     */
    public static class ReplaceString extends LintFix {
        /** The string literal to replace. */
        @Nullable public final String oldString;
        /** The regex to replace. Will always have at least one group, which should
         * be the replacement range. */
        @Nullable public final String oldPattern;
        /** The replacement string. */
        @NonNull public final String replacement;

        /**
         * A location range to use for searching for the text or pattern. Useful
         * if you want to make a replacement that is larger than the error range
         * highlighted as the problem range.
         */
        @Nullable public final Location range;
        /** Whether symbols should be shortened after replacement */
        public final boolean shortenNames;
        /** Whether the modified text range should be reformatted */
        public final boolean reformat;

        /**
         * Replace the given string within the range of the element this warning is marked on
         *
         * @param displayName  the displayName
         * @param oldString    the literal string to replace
         * @param oldPattern   the regular expression to replace (provided as a string such that it
         *                     only needs to be compiled if actually referenced by the IDE. If there
         *                     is a group in the regexp, the substitution will be placed within the
         *                     group. If there is more than one group, it will be placed in
         *                     the group named "target"; if no group is named "target" it will be
         *                     placed in the first group.
         * @param replacement  the replacement literal string
         * @param shortenNames whether to shorten references in the replaced range
         * @param reformat     whether to reformat the replaced range
         * @param range        a range to use for searching for the old text, if different/larger
         *                     than the warning highlight range
         */
        private ReplaceString(
          @NonNull String displayName,
          @Nullable String oldString, @Nullable String oldPattern,
          @NonNull String replacement, boolean shortenNames,
                boolean reformat,
                @Nullable Location range) {
            super(displayName);
            this.oldString = oldString;
            this.oldPattern = oldPattern;
            this.replacement = replacement;
            this.shortenNames = shortenNames;
            this.reformat = reformat;
            this.range = range;
        }

        /** Return display name */
        @NonNull
        @Override
        public String getDisplayName() {
            if (displayName != null) {
                return displayName;
            } else {
                return "Replace with " + replacement;
            }
        }

        /**
         * If this {@linkplain ReplaceString} specified a regular expression in
         * {@link #oldPattern}, and the replacement string {@link #replacement}
         * specifies one or more "back references" (with {@code (?<name>)} with the syntax
         * {@code \k<name>} then this method will substitute in the matching
         * group. Note that "target" is a reserved name, used to identify the range
         * that should be completed.
         */
        public String expandBackReferences(@NonNull Matcher matcher) {
            return expandBackReferences(replacement, matcher);
        }

        /**
         * Given a matched regular expression and a back reference expression,
         * this method produces the expression with back references substituted in.
         */
        public static String expandBackReferences(@NonNull String replacement,
                @NonNull Matcher matcher) {
            if (!replacement.contains("\\k<")) {
                return replacement;
            }

            StringBuilder sb = new StringBuilder();
            int begin = 0;
            while (true) {
                int end = replacement.indexOf("\\k<", begin);
                if (end == -1) {
                    sb.append(replacement.substring(begin));
                    break;
                } else {
                    int next = replacement.indexOf('>', end + 3);
                    if (next != -1 && Character.isDigit(replacement.charAt(end + 3))) {
                        sb.append(replacement.substring(begin, end));
                        String groupString = replacement.substring(end + 3, next);
                        int group = Integer.parseInt(groupString);
                        if (group <= matcher.groupCount()) {
                            sb.append(matcher.group(group));
                        }
                        begin = next + 1;
                    } else {
                        end += 3;
                        sb.append(replacement.substring(begin, end));
                        begin = end;
                    }
                }
            }

            return sb.toString();
        }
    }
}
