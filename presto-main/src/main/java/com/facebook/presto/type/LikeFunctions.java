/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.type;

import com.facebook.presto.metadata.OperatorType;
import com.facebook.presto.operator.scalar.ScalarFunction;
import com.facebook.presto.operator.scalar.ScalarOperator;
import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.type.StandardTypes;
import io.airlift.slice.Slice;
import org.jcodings.specific.UTF8Encoding;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Syntax;

import static com.facebook.presto.spi.StandardErrorCode.INVALID_FUNCTION_ARGUMENT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.joni.constants.MetaChar.INEFFECTIVE_META_CHAR;
import static org.joni.constants.SyntaxProperties.OP_ASTERISK_ZERO_INF;
import static org.joni.constants.SyntaxProperties.OP_DOT_ANYCHAR;
import static org.joni.constants.SyntaxProperties.OP_LINE_ANCHOR;

public final class LikeFunctions
{
    private static final Syntax SYNTAX = new Syntax(
            OP_DOT_ANYCHAR | OP_ASTERISK_ZERO_INF | OP_LINE_ANCHOR,
            0,
            0,
            Option.NONE,
            new Syntax.MetaCharTable(
                    '\\',                           /* esc */
                    INEFFECTIVE_META_CHAR,          /* anychar '.' */
                    INEFFECTIVE_META_CHAR,          /* anytime '*' */
                    INEFFECTIVE_META_CHAR,          /* zero or one time '?' */
                    INEFFECTIVE_META_CHAR,          /* one or more time '+' */
                    INEFFECTIVE_META_CHAR           /* anychar anytime */
            )
    );

    private LikeFunctions() {}

    // TODO: this should not be callable from SQL
    @ScalarFunction(value = "like", hidden = true)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean like(@SqlType(StandardTypes.VARCHAR) Slice value, @SqlType(LikePatternType.NAME) Regex pattern)
    {
        // Joni doesn't handle invalid UTF-8, so replace invalid characters
        byte[] bytes = value.getBytes();
        if (isAscii(bytes)) {
            return regexMatches(pattern, bytes);
        }
        // convert to a String and back to "fix" any broken UTF-8 sequences
        return regexMatches(pattern, value.toStringUtf8().getBytes(UTF_8));
    }

    @ScalarOperator(OperatorType.CAST)
    @SqlType(LikePatternType.NAME)
    public static Regex likePattern(@SqlType(StandardTypes.VARCHAR) Slice pattern)
    {
        return likeToPattern(pattern.toStringUtf8(), '0', false);
    }

    @ScalarFunction
    @SqlType(LikePatternType.NAME)
    public static Regex likePattern(@SqlType(StandardTypes.VARCHAR) Slice pattern, @SqlType(StandardTypes.VARCHAR) Slice escape)
    {
        return likeToPattern(pattern.toStringUtf8(), getEscapeChar(escape), true);
    }

    private static boolean regexMatches(Regex regex, byte[] bytes)
    {
        return regex.matcher(bytes).match(0, bytes.length, Option.NONE) != -1;
    }

    @SuppressWarnings("NestedSwitchStatement")
    private static Regex likeToPattern(String patternString, char escapeChar, boolean shouldEscape)
    {
        StringBuilder regex = new StringBuilder(patternString.length() * 2);

        regex.append('^');
        boolean escaped = false;
        for (char currentChar : patternString.toCharArray()) {
            if (shouldEscape && !escaped && (currentChar == escapeChar)) {
                escaped = true;
            }
            else {
                switch (currentChar) {
                    case '%':
                        regex.append(escaped ? "%" : ".*");
                        escaped = false;
                        break;
                    case '_':
                        regex.append(escaped ? "_" : ".");
                        escaped = false;
                        break;
                    default:
                        // escape special regex characters
                        switch (currentChar) {
                            case '\\':
                            case '^':
                            case '$':
                            case '.':
                            case '*':
                                regex.append('\\');
                        }

                        regex.append(currentChar);
                        escaped = false;
                }
            }
        }
        regex.append('$');

        byte[] bytes = regex.toString().getBytes(UTF_8);
        return new Regex(bytes, 0, bytes.length, Option.MULTILINE, UTF8Encoding.INSTANCE, SYNTAX);
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    private static char getEscapeChar(Slice escape)
    {
        String escapeString = escape.toString(UTF_8);
        if (escapeString.isEmpty()) {
            // escaping disabled
            return (char) -1; // invalid character
        }
        if (escapeString.length() == 1) {
            return escapeString.charAt(0);
        }
        throw new PrestoException(INVALID_FUNCTION_ARGUMENT, "Escape must be empty or a single character");
    }

    private static boolean isAscii(byte[] bytes)
    {
        for (byte b : bytes) {
            if (b < 0) {
                return false;
            }
        }
        return true;
    }
}
